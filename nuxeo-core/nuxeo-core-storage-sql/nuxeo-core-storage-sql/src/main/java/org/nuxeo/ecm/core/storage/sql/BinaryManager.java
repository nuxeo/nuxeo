/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.io.InputStream;

/**
 * A binary manager stores binaries according to their digest.
 */
public interface BinaryManager {

    /**
     * Initializer.
     */
    void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException;

    /**
     * Saves the given input stream into a {@link Binary}.
     * <p>
     * Returns a {@link Binary} representing the stream. The {@link Binary}
     * includes a digest that is a sufficient representation to persist it.
     *
     * @param in the input stream
     * @return the corresponding binary
     * @throws IOException
     */
    Binary getBinary(InputStream in) throws IOException;

    /**
     * Returns a {@link Binary} corresponding to the given digest.
     * <p>
     * A {@code null} is returned if the digest could not be found.
     *
     * @param digest the digest, or {@code null}
     * @return the corresponding binary
     */

    Binary getBinary(String digest);

}
