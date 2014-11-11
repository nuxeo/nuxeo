/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.IOException;

/**
 * A simple binary manager that "hides" binaries on the filesystem by scrambling
 * them on write and unscrambling them on read using XOR.
 * <p>
 * The {@link BinaryManagerDescriptor} holds a key that can be used to drive the
 * scrambling/unscrambling.
 * <p>
 * This is to prevent casual reading of the files, but of course the algorithm
 * and key for scrambling are available on the system as well, so this not a
 * secure store.
 */
public class XORBinaryManager extends LocalBinaryManager {

    protected byte[] pattern;

    @Override
    public void initialize(BinaryManagerDescriptor binaryManagerDescriptor)
            throws IOException {
        super.initialize(binaryManagerDescriptor);
        String key = binaryManagerDescriptor.key;
        if (key == null || key.length() == 0) {
            key = "U"; // 0x55
        }
        try {
            pattern = key.getBytes("UTF-8");
        } catch (Exception e) {
            // cannot happen
            pattern = new byte[] { 'U' };
        }
    }

    @Override
    protected BinaryScrambler getBinaryScrambler() {
        return new XORBinaryScrambler(pattern);
    }

    /**
     * A {@link BinaryScrambler} that does an XOR with the given pattern.
     */
    public static class XORBinaryScrambler implements BinaryScrambler {
        private static final long serialVersionUID = 1L;

        protected final byte[] pattern;

        protected long pos;

        public XORBinaryScrambler(byte[] pattern) {
            this.pattern = pattern;
            pos = 0;
        }

        @Override
        public void scrambleBuffer(byte[] buf, int off, int n) {
            for (int i = 0; i < n; i++) {
                buf[off + i] ^= pattern[(int) (pos % pattern.length)];
                pos++;
            }
        }

        @Override
        public void unscrambleBuffer(byte[] buf, int off, int n) {
            // scramble and unscramble are the same for XOR
            scrambleBuffer(buf, off, n);
        }

        @Override
        public Binary getUnscrambledBinary(File file, String digest,
                String repoName) {
            return new ScrambledBinary(file, digest, repoName, this);
        }

        @Override
        public void skip(long n) {
            pos += n;
        }

        @Override
        public void reset() {
            pos = 0;
        }
    }

}
