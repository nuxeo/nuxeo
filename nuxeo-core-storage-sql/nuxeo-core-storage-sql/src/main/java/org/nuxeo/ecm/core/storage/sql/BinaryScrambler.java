/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
    Binary getUnscrambledBinary(File file, String digets);

    /**
     * Skips n bytes during unscrambling.
     */
    void skip(long n);

    /**
     * Reset scrambling from start.
     */
    void reset();

}
