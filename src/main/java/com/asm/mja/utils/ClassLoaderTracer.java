package com.asm.mja.utils;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * The ClassLoaderTracer utility provides methods to trace the classloader hierarchy
 * and print information about the loaded class.
 *
 * @author ashut
 * @since 11-04-2024
 */

public class ClassLoaderTracer {

    /**
     * Prints information about the loaded class including its name, classloader hierarchy,
     * and where it was loaded from.
     *
     * @param className         The name of the loaded class.
     * @param loader            The classloader that loaded the class.
     * @param protectionDomain  The protection domain of the class.
     * @return A string containing information about the loaded class.
     */
    public static String printClassInfo(String className, ClassLoader loader, ProtectionDomain protectionDomain) {
        return "\n{\n\tClass: " + className + "\n\t" +
                printClassLoaderHierarchy(loader) + "\n\t" +
                printLoadedFrom(protectionDomain) + "\n}";
    }

    /**
     * Prints the classloader hierarchy of the loaded class.
     *
     * @param classLoader   The classloader of the loaded class.
     * @return A string representing the classloader hierarchy.
     */
    public static String printClassLoaderHierarchy(ClassLoader classLoader) {
        StringBuilder hierarchy = new StringBuilder("ClassLoaderHierarchy: ");
        while (classLoader != null) {
            hierarchy.append(classLoader.getClass().getName()).append(" -> ");
            classLoader = classLoader.getParent();
        }
        hierarchy.append("BootstrapClassLoader");
        return hierarchy.toString();
    }

    /**
     * Prints where the class was loaded from.
     *
     * @param protectionDomain  The protection domain of the class.
     * @return A string indicating where the class was loaded from.
     */
    public static String printLoadedFrom(ProtectionDomain protectionDomain) {
        CodeSource codeSource = protectionDomain.getCodeSource();
        String loadedFrom = "Loaded from: ";
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            loadedFrom += location.getPath();
        } else {
            loadedFrom +=  "Unknown location";
        }
        return loadedFrom;
    }
}
