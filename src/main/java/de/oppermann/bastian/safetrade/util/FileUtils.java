package de.oppermann.bastian.safetrade.util;

import java.io.*;

/**
 * This class contains some useful methods e.g. for copying files.
 */
public class FileUtils {

    private FileUtils() { /* nope */ }

    /**
     * Copies an {@link InputStream} to a {@link File}.
     *
     * @param from The InputStream to copy from.
     * @param to   The File to copy to.
     * @throws IOException If something went wrong.
     */
    public static void copy(InputStream from, File to) throws IOException {
        InputStream inputStream = from;
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(to);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } finally { // close the steams when finished
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
