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

import java.io.File;

/**
 * A scrambler/unscrambler of binaries.
 *
 * @author Florent Guillaume
 */
public interface BinaryScrambler {

    /**
     * Scramble a buffer at the given offset for n bytes.
     */
    void scrambleBuffer(byte[] buf, int off, int n);

    /**
     * Unscramble a buffer at the given offset for n bytes.
     */
    void unscrambleBuffer(byte[] buf, int off, int n);

    /**
     * Gets an unscrambled {@link Binary} for the given file.
     */
    Binary getUnscrambledBinary(File file, String digets, String repoName);

    /**
     * Skips n bytes during unscrambling.
     */
    void skip(long n);

    /**
     * Reset scrambling from start.
     */
    void reset();

}
