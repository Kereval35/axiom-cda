package net.ihe.gazelle.axiomcda.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourcePaths {
    private static final Set<Path> EXTRACTED_PATHS = new LinkedHashSet<>();
    private static final AtomicBoolean HOOK_REGISTERED = new AtomicBoolean(false);

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
        registerExtractedPath(targetDir);
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
        registerExtractedPath(target);
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    public static void cleanupExtractedResources() {
        synchronized (EXTRACTED_PATHS) {
            for (Path path : EXTRACTED_PATHS) {
                deleteRecursively(path);
            }
            EXTRACTED_PATHS.clear();
        }
    }

    private static void registerExtractedPath(Path path) {
        if (path == null) {
            return;
        }
        synchronized (EXTRACTED_PATHS) {
            EXTRACTED_PATHS.add(path);
        }
        if (HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(ResourcePaths::cleanupExtractedResources));
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
            walk.sorted(Comparator.reverseOrder()).forEach(current -> {
                try {
                    Files.deleteIfExists(current);
                } catch (IOException ignored) {
                    // Best-effort cleanup.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
