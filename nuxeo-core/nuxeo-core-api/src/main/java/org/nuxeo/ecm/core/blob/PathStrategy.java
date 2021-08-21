/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Decides at what path a given key is stored.
 *
 * @since 11.1
 */
public abstract class PathStrategy {

    private static final Logger log = LogManager.getLogger(PathStrategy.class);

    protected final Path dir;

    public PathStrategy(Path dir) {
        this.dir = dir.normalize();
    }

    // ASCII except / (Unix) and \ (Windows) and : (Windows) and % (escaping)
    protected static final Pattern SAFE = Pattern.compile("[ -~&&[^%/:\\\\]]+");

    protected static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * Converts a key to a safe path.
     * <p>
     * Different keys always map to different safe paths (no collision).
     *
     * @param key the key
     * @return the safe path
     * @since 11.5
     */
    protected String safePath(String key) {
        if (SAFE.matcher(key).matches() && !key.equals(".") && !key.equals("..")) {
            return key;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('%'); // marker for encoded value, in case we have to decode it later
        for (byte b : key.getBytes(UTF_8)) {
            if (b >= ' ' && b <= '~' && b != '%' && b != '/' && b != ':' && b != '\\') {
                // ASCII except % / : \
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(HEX[(0xF0 & b) >>> 4]);
                sb.append(HEX[0x0F & b]);
            }
        }
        return sb.toString();
    }

    /**
     * Inverse of {@link #safePath}.
     *
     * @param path the safe path
     * @return the key, or {@code null} if the safe path is invalid
     * @since 2021.8
     */
    protected String safePathInverse(String path) {
        if (!path.startsWith("%")) {
            return path;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        char[] chars = path.toCharArray();
        for (int i = 1; i < chars.length; i++) {
            char c = chars[i];
            if (c == '%') {
                if (i + 3 > chars.length) {
                    return null;
                }
                try {
                    c = (char) Integer.parseInt(path.substring(i + 1, i + 3), 16);
                    i += 2;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            out.write(c);
        }
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a temporary file in a location suitable for efficient move to the final path for any key.
     *
     * @return the temporary file
     */
    public Path createTempFile() {
        try {
            return Files.createTempFile(dir, "bin_", ".tmp");
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Checks if the given file is a temporary file
     *
     * @param path the file
     * @return {@code true} if the file is a temporary file
     * @since 11.5
     */
    public boolean isTempFile(Path path) {
        String filename = path.getFileName().toString();
        return filename.startsWith("bin_") && filename.endsWith(".tmp");
    }

    /**
     * Gets the storage path for a given key.
     *
     * @param key the key
     * @return the file for this key
     */
    public abstract Path getPathForKey(String key);

    /**
     * Gets the key for a given storage path.
     *
     * @param path the path
     * @return the key
     */
    public String getKeyForPath(String path) {
        path = path.substring(path.lastIndexOf("/") + 1);
        return safePathInverse(path);
    }

    /**
     * Does an atomic move from source to dest.
     *
     * @param source the source
     * @param dest the destination
     */
    public static void atomicMove(Path source, Path dest) throws IOException {
        try {
            Files.move(source, dest, ATOMIC_MOVE, REPLACE_EXISTING);
            // move also copied the last-modified date; needed for GC
        } catch (AtomicMoveNotSupportedException amnse) {
            // shouldn't happen, given our usual choices of tmp and storage locations
            // do a copy through a tmp file on the same filesystem then atomic rename
            log.debug("Unoptimized atomic move from " + source + " to " + dest);
            Path tmp = Files.createTempFile(dest.getParent(), "bin_", ".tmp");
            try {
                Files.copy(source, tmp, REPLACE_EXISTING);
                Files.move(tmp, dest, ATOMIC_MOVE);
                Files.delete(source);
            } finally {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
        }
    }

    /**
     * Does an atomic copy from source to dest.
     *
     * @param source the source
     * @param dest the destination
     * @since 11.5
     */
    public static void atomicCopy(Path source, Path dest) throws IOException {
        Path tmp = Files.createTempFile(dest.getParent(), "bin_", ".tmp");
        try {
            Files.copy(source, tmp, REPLACE_EXISTING);
            Files.move(tmp, dest, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                log.error(e, e);
            }
        }
    }
}
