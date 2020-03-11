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

import java.nio.file.Path;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Stores a file in a nested subdirectory based on the initial characters of the key, in groups of 2. The key is
 * restricted the a safe subset of characters to use as filenames.
 * <p>
 * For instance for a depth of 3 and a key of 1234567890abcd the path will be 12/34/56/1234567890abcd.
 *
 * @since 11.1
 */
public class PathStrategySubDirs extends PathStrategy {

    /** Allowed key pattern, used as file path. */
    protected static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    protected final int depth;

    public PathStrategySubDirs(Path dir, int depth) {
        super(dir);
        this.depth = depth;
    }

    @Override
    public Path getPathForKey(String key) {
        if (!PATTERN.matcher(key).matches()) {
            throw new NuxeoException("Invalid key: " + key);
        }
        if (key.length() < 2 * depth) {
            // store short keys under a special 000 directory
            return dir.resolve("000").resolve(key);
        }
        Path current = dir;
        for (int i = 0; i < depth; i++) {
            current = current.resolve(key.substring(2 * i, 2 * i + 2));
        }
        return current.resolve(key);
    }

}
