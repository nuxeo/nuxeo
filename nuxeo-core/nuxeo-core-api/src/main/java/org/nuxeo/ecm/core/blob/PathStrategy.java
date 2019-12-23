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

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Decides at what path a given key is stored.
 *
 * @since 11.1
 */
public abstract class PathStrategy {

    private static final Log log = LogFactory.getLog(PathStrategy.class);

    protected final Path dir;

    public PathStrategy(Path dir) {
        this.dir = dir.normalize();
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
     * Gets the storage path for a given key.
     *
     * @param key the key
     * @return the file for this key
     */
    public abstract Path getPathForKey(String key);

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

}
