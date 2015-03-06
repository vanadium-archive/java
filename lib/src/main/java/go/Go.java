package go;

// Go is an entry point for libraries compiled in Go.  The caller should invoke:
// <code>
//     Go.init();
// </code>
// When the function returns, it is safe to start calling Go code.
public final class Go {
    private static boolean running = false;

    private static native void run();
    private static native void waitForRun();

    /**
     * Initializes the Go runtime.  This method must be called from the main thread.
     */
    public static void init() {
        if (running) {
            return;
        }
        running = true;

        new Thread("GoMain") {
            @Override
            public void run() {
                Go.run();
            }
        }.start();

        Go.waitForRun();
    }
}
