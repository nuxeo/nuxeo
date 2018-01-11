/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JarUtils {

    // Utility class.
    private JarUtils() {
    }

    public static Manifest getManifest(File file) {
        try {
            if (file.isDirectory()) {
                return getDirectoryManifest(file);
            } else {
                return getJarManifest(file);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    public static Manifest getDirectoryManifest(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(file, "META-INF/MANIFEST.MF"));) {
            return new Manifest(fis);
        }
    }

    public static Manifest getJarManifest(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            return jar.getManifest();
        }
    }

    public static Manifest getManifest(URL url) {
        try (JarFile jarFile = new JarFile(new File(url.getFile()))) {
            return jarFile.getManifest();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Zips recursively as a jar the content of {@code source} to the {@code target} zip file.
     *
     * @since 9.3
     */
    public static Path zipDirectory(Path source, Path target, CopyOption... options) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source argument must be a directory to zip");
        }
        // locate file system by using the syntax defined in java.net.JarURLConnection
        URI uri = toJarURI(target);

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (source.equals(dir)) {
                        // don't process root element
                        return FileVisitResult.CONTINUE;
                    }
                    return visitFile(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // retrieve the destination path in zip
                    Path relativePath = source.relativize(file);
                    Path pathInZipFile = zipfs.getPath(relativePath.toString());
                    // copy a file into the zip file
                    Files.copy(file, pathInZipFile, options);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
        return target;
    }

    /**
     * Convert a {@link Path} to an {@link URI} with {@code jar} scheme.
     *
     * @since 9.10
     */
    public static URI toJarURI(Path path) {
        try {
            return new URI("jar", path.toUri().toString(), null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to create Jar URI", e);
        }
    }

}
