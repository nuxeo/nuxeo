/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.ecm.core.blob;

import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Extends the default {@link PathStrategy#safePath(String)} method to ensure resulting path and file name is not too
 * long.
 *
 * @since 2021.14
 */
public class PathStrategyShortened extends PathStrategy {

    public PathStrategyShortened(Path dir) {
        super(dir);
    }

    @Override
    protected String safePath(String key) {
        // sha512 digest is limited to 128 hexa characters
        return DigestUtils.sha512Hex(key);
    }

    @Override
    protected String safePathInverse(String path) {
        // This strategy cannot be used when we need to retrieve a key from a path (i.e garbage collecting)
        throw new UnsupportedOperationException(
                "The org.nuxeo.ecm.core.blob.PathStrategyShorten should not be used with a blob store supporting garbage collecting.");
    }

    @Override
    public Path getPathForKey(String key) {
        key = safePath(key);
        return dir.resolve(key);
    }

}
