package net.ihe.gazelle.axiomcda.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourcePaths {
    private ResourcePaths() {
    }

    public static Path getResourcePath(String resourceName) {
        URL url = ResourcePaths.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }
        try {
            if ("file".equals(url.getProtocol())) {
                return Paths.get(url.toURI());
            }
            if ("jar".equals(url.getProtocol())) {
                return extractFromJar(resourceName, url);
            }
            throw new IllegalArgumentException("Unsupported resource protocol: " + url.getProtocol());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid resource URI for " + resourceName, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to extract resource " + resourceName, e);
        }
    }

    private static Path extractFromJar(String resourceName, URL url) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            String prefix = resourceName.endsWith("/") ? resourceName : resourceName + "/";
            boolean isDirectory = jarFile.stream().anyMatch(entry -> entry.getName().startsWith(prefix));
            if (isDirectory) {
                return extractDirectory(jarFile, prefix);
            }
            return extractFile(jarFile, resourceName);
        }
    }

    private static Path extractDirectory(JarFile jarFile, String prefix) throws IOException {
        Path targetDir = Files.createTempDirectory("axiom-cda-resource-");
        for (JarEntry entry : jarFile.stream().toList()) {
            if (!entry.getName().startsWith(prefix)) {
                continue;
            }
            if (entry.isDirectory()) {
                continue;
            }
            Path target = targetDir.resolve(entry.getName().substring(prefix.length()));
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return targetDir;
    }

    private static Path extractFile(JarFile jarFile, String entryName) throws IOException {
        JarEntry entry = jarFile.getJarEntry(entryName);
        if (entry == null) {
            throw new IllegalArgumentException("Resource not found in jar: " + entryName);
        }
        String suffix = entryName.contains(".") ? entryName.substring(entryName.lastIndexOf('.')) : ".tmp";
        Path target = Files.createTempFile("axiom-cda-resource-", suffix);
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
