// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncslidepresenter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;

import org.joda.time.Duration;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import io.v.android.apps.syncslides.db.VCurrentSlide;
import io.v.android.apps.syncslides.db.VSlide;
import io.v.android.apps.syncslides.discovery.ParticipantClient;
import io.v.android.apps.syncslides.discovery.ParticipantClientFactory;
import io.v.android.apps.syncslides.discovery.Presentation;
import io.v.impl.google.naming.NamingUtil;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.V;
import io.v.v23.VIterable;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Syncgroup;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * The entry point for syncslidepresenter. To run:
 *
 * <pre>
 *     cd $JIRI_ROOT/release/java
 *     ./gradlew :projects:syncslidepresenter:installDist
 *     ./projects/syncslidepresenter/build/install/syncslidepresenter/bin/syncslidepresenter
 * </pre>
 *
 * <p>For reasons not yet fully understood, applications running both Swing and
 * Vanadium freeze randomly (at least on Mac OS X). We work around this by
 * running two JVMs. The main JVM will create a sub-JVM in a separate thread,
 * and will then proceed to join a syncgroup and look for slide data. Once a new
 * slide is detected, the bytes representing the slide will be sent to the
 * sub-JVM via RMI.
 *
 * <p>The sub-JVM simply listens for slide bytes and then, using {@link
 * ImageIO#read}, decodes those bytes and displays the resulting image in a
 * JFrame.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final String SYNCBASE_APP = "syncslides";
    private static final String SYNCBASE_DB = "syncslides";
    private static final String PRESENTATIONS_TABLE = "Presentations";
    private static final String DECKS_TABLE = "Decks";
    private static final int MAX_PORT_PICKER_ATTEMPTS = 100;
    private final Table presentations;
    private final Table decks;
    private final ImageViewer viewer;

    private Database db;

    private VContext context;

    public static void main(String[] args)
            throws SyncbaseServer.StartException, VException, IOException, NotBoundException,
            AlreadyBoundException {
        Options options = new Options();
        JCommander commander = new JCommander(options);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            logger.warning("Could not parse parameters: " + e.getMessage());
            commander.usage();
            return;
        }

        if (options.help) {
            commander.usage();
            return;
        }

        ImageViewer viewer = null;
        if (options.swing) {
            // We're just going to be running Swing. Do that and then stop.
            startSwingServer();
            return;
        }

        // Otherwise, we're a Vanadium client. Connect to the swing server and then
        // set up vanadium.
        viewer = startAndConnectToSwingServer(options.swingServerTimeoutSeconds);

        VContext baseContext = V.init();

        AccessList acl = new AccessList(ImmutableList.of(new BlessingPattern("...")),
                ImmutableList.<String>of());
        Permissions permissions = new Permissions(ImmutableMap.of("1", acl));
        String name = NamingUtil.join(options.mountPrefix, UUID.randomUUID().toString());
        logger.info("Mounting new syncbase server at " + name);
        VContext mountContext = SyncbaseServer.withNewServer(baseContext,
                new SyncbaseServer.Params().withPermissions(permissions).withName(name)
                        .withStorageRootDir(options.storageRootDir));
        final Server server = V.getServer(mountContext);
        if (server.getStatus().getEndpoints().length > 0) {
            logger.info("Mounted syncbase server at the following endpoints: ");
            for (Endpoint e : server.getStatus().getEndpoints()) {
                logger.info("\t" + e);
            }
            logger.info("End of endpoint list");

            SyncbaseService service
                    = Syncbase.newService("/" + server.getStatus().getEndpoints()[0]);
            SyncbaseApp app = service.getApp(SYNCBASE_APP);
            if (!app.exists(baseContext)) {
                app.create(baseContext, permissions);
            }
            Database db = app.getNoSqlDatabase(SYNCBASE_DB, null);
            if (!db.exists(baseContext)) {
                db.create(baseContext, permissions);
            }
            Table decks = db.getTable(DECKS_TABLE);
            if (!decks.exists(baseContext)) {
                decks.create(baseContext, permissions);
            }
            Table presentations = db.getTable(PRESENTATIONS_TABLE);
            if (!presentations.exists(baseContext)) {
                presentations.create(baseContext, permissions);
            }

            Main m = new Main(baseContext, viewer, db, decks, presentations);

            Presentation presentation = new Discovery(
                    baseContext, options.mountPrefix,
                    options.deckPrefix, options.maxMtScanCount).getPresentation();
            logger.info("Using presentation: " + presentation);
            m.joinPresentation(presentation, options.joinTimeoutSeconds, options.slideRowFormat);
        }
    }

    private static void startSwingServer() throws IOException, AlreadyBoundException {
        logger.info("inside startSwingServer");
        // Make command-Q do the same as closing the main frame (i.e. exit).
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        System.setProperty("java.rmi.server.hostname", "localhost");

        JFrame frame = new JFrame();
        enableOSXFullscreen(frame);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        JPanel panel = new JPanel(new GridBagLayout());
        ScaleToFitJPanel presentationPanel = new ScaleToFitJPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(presentationPanel, constraints);
        frame.getContentPane().add(panel);
        frame.pack();

        int port = pickUnusedPort();
        LocateRegistry.createRegistry(port);
        String name = "//localhost:" + port + "/" + UUID.randomUUID();
        logger.info("swing server binding to " + name);
        Naming.bind(name, new RemoteImageViewer(presentationPanel));

        // The parent JVM will expect to read this value.
        System.out.println(name);
    }

    public Main(VContext context, ImageViewer viewer, Database db, Table decks,
                Table presentations) throws VException {
        this.context = context;
        this.db = db;
        this.presentations = presentations;
        this.decks = decks;
        this.viewer = viewer;
    }

    public void joinPresentation(final Presentation presentation,
                                 int joinTimeoutSeconds,
                                 String slideRowFormat) throws VException {
        Syncgroup syncgroup = db.getSyncgroup(presentation.getSyncgroupName());
        syncgroup.join(context.withTimeout(Duration.standardSeconds(joinTimeoutSeconds)),
                new SyncgroupMemberInfo((byte) 1));
        for (String member : syncgroup.getMembers(context).keySet()) {
            logger.info("Member: " + member);
        }

        for (KeyValue keyValue : presentations.scan(context, RowRange.prefix(""))) {
            System.out.println("Presentation: " + keyValue);
        }
        BatchDatabase batch = db.beginBatch(context, null);
        ResumeMarker marker = batch.getResumeMarker(context);
        String rowKey = Joiner.on("/").join(presentation.getDeckId(), presentation
                .getPresentationId(), "CurrentSlide");
        logger.info("going to watch row key " + rowKey);
        VIterable<WatchChange> changes = db.watch(context, presentations.name(), rowKey, marker);

        for (WatchChange change : changes) {
            logger.info("Change detected in " + change.getRowName());
            logger.info("Type: " + change.getChangeType());
            try {
                VCurrentSlide currentSlide = (VCurrentSlide) VomUtil.decode(change.getVomValue(),
                        VCurrentSlide.class);
                logger.info("Current slide: " + currentSlide);
                // Read the corresponding slide.
                String row = String.format(slideRowFormat, presentation.getDeckId(),
                        currentSlide.getNum());
                VSlide slide = (VSlide) decks.getRow(row).get(context, VSlide.class);
                viewer.setImage(slide.getThumbnail());
            } catch (IOException | VException e) {
                logger.log(Level.WARNING, "exception encountered while handling change event", e);
            }
        }

        if (changes.error() != null) {
            logger.log(Level.WARNING, "Premature end of slide changes: " + changes.error());
        }
    }

    private static void enableOSXFullscreen(Window window) {
        Preconditions.checkNotNull(window);
        try {
            // This class may not be present on the system (e.g. if we're not on MacOSX),
            // use reflection so that we can make this an optional dependency.
            Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
            Class params[] = new Class[]{Window.class, Boolean.TYPE};

            @SuppressWarnings({"unchecked", "rawtypes"})
            Method method = util.getMethod("setWindowCanFullScreen", params);
            method.invoke(util, window, true);
        } catch (ClassNotFoundException e) {
            // Probably not on Mac OS X
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't enable fullscreen on Mac OS X", e);
        }
    }

    public static class Options {
        @Parameter(names = {"-s", "--storageRootDir"},
                description = "the root directory to use for local storage")
        private String storageRootDir = Joiner.on(File.separator).join(
                System.getProperty("java.io.tmpdir"), "syncslidepresenter-storage");

        @Parameter(names = {"-d", "--deckPrefix"},
                description = "mounttable prefix for live presentations.")
        private String deckPrefix = "happyDeck";

        @Parameter(names = {"--swing"})
        private boolean swing = false;

        @Parameter(names = {"-m", "--mountPrefix"}, description = "the base path in the namespace"
                + " where the syncbase service will be mounted")
        private String mountPrefix = "/192.168.86.254:8101";

        @Parameter(names = {"--joinTimeout"},
                description = "the number of seconds to wait to join the presentation")
        private int joinTimeoutSeconds = 10;

        @Parameter(names = {"--maxMtScanCount"},
                description = "max number of times to scan MT looking for presentations.")
        private int maxMtScanCount = 10;

        @Parameter(names = {"-f", "--slideRowFormat"},
                description = "a pattern specifying where slide rows are found")
        private String slideRowFormat = "%s/slides/%04d";

        @Parameter(names = {"-h", "--help"}, description = "display this help message", help = true)
        private boolean help = false;

        @Parameter(names = {"--swingServerTimeoutSeconds"},
                description = "the amount of time to wait for the swing server to start")
        private int swingServerTimeoutSeconds = 10;
    }

    private static class ScaleToFitJPanel extends JPanel implements ImageViewer {
        private Image image;

        public ScaleToFitJPanel() {
            super();
            setPreferredSize(new Dimension(250, 250));
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                int width;
                int height;
                double containerRatio = 1.0d * getWidth() / getHeight();
                double imageRatio = 1.0d * image.getWidth(null) / image.getHeight(null);

                if (containerRatio < imageRatio) {
                    width = getWidth();
                    height = (int) (getWidth() / imageRatio);
                } else {
                    width = (int) (getHeight() * imageRatio);
                    height = getHeight();
                }

                // Center the image in the container.
                int x = (int) (((double) getWidth() / 2) - ((double) width / 2));
                int y = (int) (((double) getHeight() / 2) - ((double) height / 2));

                g.drawImage(image, x, y, width, height, this);
            }
        }

        @Override
        public void setImage(byte[] imageData) throws RemoteException {
            try {
                this.image = ImageIO.read(new ByteArrayInputStream(imageData));
                setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
                repaint();
            } catch (IOException e) {
                throw new RemoteException("Could not set image", e);
            }
        }
    }

    private interface ImageViewer extends Remote {
        void setImage(byte[] imageData) throws RemoteException;
    }

    private static class RemoteImageViewer extends UnicastRemoteObject implements ImageViewer {
        private final ImageViewer viewer;

        protected RemoteImageViewer(ImageViewer viewer) throws RemoteException {
            super();

            this.viewer = viewer;
        }

        @Override
        public void setImage(byte[] imageData) throws RemoteException {
            viewer.setImage(imageData);
        }
    }

    private static ImageViewer startAndConnectToSwingServer(int timeoutSeconds) throws IOException,
            NotBoundException {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new IllegalStateException("System property java.home is not set");
        }
        File javaBin = new File(javaHome, Joiner.on(File.separator).join("bin", "java"));
        if (!javaBin.exists() || !javaBin.canExecute()) {
            throw new IllegalStateException("Java binary " + javaBin
                    + " does not exist or is not executable");
        }
        ProcessBuilder builder = new ProcessBuilder(javaBin.getAbsolutePath(),
                "-classpath", System.getProperty("java.class.path"),
                Main.class.getCanonicalName(), "--swing");
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        logger.info("starting " + builder.command());
        // The first line of output should be the address to which we should connect.
        final Process process = builder.start();
        logger.info("started " + process);

        // Get a timeout timer going.
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(new Runnable() {
            @Override
            public void run() {
                process.destroyForcibly();
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String name = reader.readLine();
        // If the timeout didn't fire, we don't want it killing our process now!
        service.shutdownNow();
        if (name != null) {
            logger.info("vanadium listener sending slide images to " + name);
            ImageViewer viewer = (ImageViewer) Naming.lookup(name);
            // Hack: when the swing viewer exits, terminate our main app.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            process.waitFor();
                            break;
                        } catch (InterruptedException e) {
                            // Ignored.
                        }
                    } while (true);
                    System.exit(0);
                }
            }, "SwingServerTerminationWatcher").start();
            return viewer;
        } else {
            throw new IOException("could not read slide name");
        }
    }

    private static int pickUnusedPort() throws IOException {
        for (int i = 0; i < MAX_PORT_PICKER_ATTEMPTS; i++) {
            ServerSocket socket = new ServerSocket();
            try {
                socket.bind(new InetSocketAddress(0));
                int port = ((InetSocketAddress) socket.getLocalSocketAddress()).getPort();
                return port;
            } catch (IOException e) {
                // Couldn't bind, try again.
            } finally {
                try {
                    socket.close();
                } catch (IOException closeException) {
                    logger.log(Level.WARNING, "Exception caught during close", closeException);
                }
            }
        }

        throw new IOException("Couldn't locate unused port");
    }

    private static class Discovery {
        public static final Duration MT_TIMEOUT =
                Duration.standardSeconds(10);

        private final VContext context;
        private final String mtName;
        private final String deckPrefix;
        private final int maxMtScanCount;

        public Discovery(VContext context, String mtName, String deckPrefix, int maxMtScanCount) {
            this.context = context;
            this.mtName = mtName;
            this.deckPrefix = deckPrefix;
            this.maxMtScanCount = maxMtScanCount;
        }

        private Set<String> scan(String pattern) throws VException {
            logger.info("Scanning MT " + mtName + " with pattern \"" + pattern + "\"");
            Namespace ns = V.getNamespace(context);
            ns.setRoots(ImmutableList.of(mtName));
            VContext ctx = context.withTimeout(MT_TIMEOUT);
            Set<String> result = new HashSet<>();
            for (int i = 0; i < maxMtScanCount; i++) {
                if (i > 0) {
                    // Wait a little before trying again.
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                }
                for (GlobReply reply : V.getNamespace(ctx).glob(ctx, pattern)) {
                    if (reply instanceof GlobReply.Entry) {
                        MountEntry entry = ((GlobReply.Entry) reply).getElem();
                        result.add(entry.getName());
                        for (MountedServer server : entry.getServers()) {
                            logger.info("    endPoint: \"" + server.getServer() + "\"");
                        }
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
            throw new IllegalStateException(
                    "Unable to find service matching " + pattern +
                            " after " + maxMtScanCount + " attempts.");
        }

        private Presentation findPreso(String serviceName) throws VException {
            V.getNamespace(context).flushCacheEntry(context, serviceName);
            ParticipantClient client =
                    ParticipantClientFactory.getParticipantClient(serviceName);
            return client.get(context.withTimeout(
                    Duration.standardSeconds(5)));
        }

        public Presentation getPresentation() throws VException {
            Set<String> services = scan(deckPrefix + "/*");
            // Just grab the first one.
            return findPreso(services.iterator().next());
        }
    }
}
