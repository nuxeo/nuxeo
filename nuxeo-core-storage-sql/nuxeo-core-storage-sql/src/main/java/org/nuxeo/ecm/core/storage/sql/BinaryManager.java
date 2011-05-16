/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * <p>
     * The stream is closed after use.
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

    /**
     * Returns the Binary Garbage Collector that can be used for this binary
     * manager.
     * <p>
     * Several calls to this method will return the same GC, so that its status
     * can be monitored using {@link BinaryGarbageCollector#isInProgress}.
     *
     * @return the binary GC
     */
    BinaryGarbageCollector getGarbageCollector();

}
