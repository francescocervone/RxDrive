package com.francescocervone.rxdrive;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class IOUtils {
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, n);
        }
    }
}
