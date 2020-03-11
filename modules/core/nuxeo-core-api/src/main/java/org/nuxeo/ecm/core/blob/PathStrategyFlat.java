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

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Stores a file in a directory based on its key.
 *
 * @since 11.1
 */
public class PathStrategyFlat extends PathStrategy {

    public PathStrategyFlat(Path dir) {
        super(dir);
    }

    @Override
    public Path getPathForKey(String key) {
        Path path = dir.resolve(key);
        if (!path.normalize().getParent().equals(dir)) {
            throw new NuxeoException("Invalid key: " + key);
        }
        return path;
    }

}
