/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.File;

/**
 *
 * @author "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 * @see NXP-10351 file cache not serializable
 * @since 5.7
 */
public abstract class BinaryCachingManager extends AbstractBinaryManager {

    public abstract BinaryFileCache fileCache();

    @Override
    public Binary getBinary(String digest) {
        // Check in the cache
        File file = fileCache().getFile(digest);
        if (file == null) {
            return new LazyBinary(digest, fileCache(), repositoryName);
        } else {
            return new Binary(file, digest, repositoryName);
        }
    }

}
