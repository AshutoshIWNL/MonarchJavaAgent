package com.asm.mja.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class ByteCodeUtils {
    public static byte[] getClassBytecode(Class<?> clazz) throws IOException {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }

        String classFilePath = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(classFilePath)) {
            if (is == null) {
                throw new IOException("Class file not found: " + classFilePath);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }
}
