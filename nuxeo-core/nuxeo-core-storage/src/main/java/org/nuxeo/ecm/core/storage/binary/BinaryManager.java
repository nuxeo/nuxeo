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

package org.nuxeo.ecm.core.storage.binary;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;

/**
 * A binary manager stores binaries according to their digest.
 */
public interface BinaryManager extends Closeable {

    /**
     * Initializer.
     */
    void initialize(BinaryManagerDescriptor descriptor) throws IOException;

    /**
     * Saves the given input stream into a {@link Binary}.
     * <p>
     * Returns a {@link Binary} representing the stream. The {@link Binary} includes a digest that is a sufficient
     * representation to persist it.
     * <p>
     * The stream is closed after use.
     *
     * @param in the input stream
     * @return the corresponding binary
     * @throws IOException
     */
    Binary getBinary(InputStream in) throws IOException;

    /**
     * Saves the given blob into a {@link Binary}.
     * <p>
     * Returns a {@link Binary} representing the stream. The {@link Binary} includes a digest that is a sufficient
     * representation to persist it.
     * <p>
     * If the blob is a temporary {@link FileBlob}, then the temporary file may be reused as the final storage location
     * after being moved.
     *
     * @param blob the blob
     * @return the corresponding binary
     * @throws IOException
     * @since 7.2
     */
    Binary getBinary(Blob blob) throws IOException;

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
     * Returns the Binary Garbage Collector that can be used for this binary manager.
     * <p>
     * Several calls to this method will return the same GC, so that its status can be monitored using
     * {@link BinaryGarbageCollector#isInProgress}.
     *
     * @return the binary GC
     */
    BinaryGarbageCollector getGarbageCollector();

    /**
     * Closes the binary manager and releases all resources and temporary objects held by it.
     */
    @Override
    void close();

}
