package io.veyron.veyron.veyron.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * TestBufferedPipedInputStream ensures that the BufferedPipedInputStream works
 * properly.
 */
public class TestBufferedPipedInputStream extends TestCase {
    public void testBufferedPipedInputStream() throws IOException {
        final BufferedPipedInputStream is = new BufferedPipedInputStream();
        final OutputStream os = is.getOutputStream();

        // Test small write and read.
        byte[] basic = new byte[] {
                1, 2
        };
        os.write(basic);
        byte[] buf = new byte[2];
        is.read(buf);
        assertTrue(Arrays.equals(basic, buf));

        // Test large write and read.
        byte[] large = new byte[10000];
        large[7000] = 8;
        os.write(large);
        buf = new byte[10000];
        is.read(buf);
        assertTrue(Arrays.equals(large, buf));

        // Test read before fully written.
        // TODO(bprosnitz) Write a better test without sleep!
        byte[] data = new byte[] {
                1, 2, 3, 4, 5
        };
        os.write(data[0]);
        os.write(data[1]);
        os.write(data[2]);
        final byte[] threadBuf = new byte[5];
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    is.read(threadBuf);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Sleep interrupted");
                }
            }
        });

        readThread.start();
        os.write(data[3]);
        os.write(data[4]);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            is.close();
            throw new RuntimeException("Sleep interrupted");
        }
        assertTrue(Arrays.equals(data, threadBuf));

        // Test EOF.
        os.close();
        assertEquals(-1, is.read());

        is.close();
    }
}
