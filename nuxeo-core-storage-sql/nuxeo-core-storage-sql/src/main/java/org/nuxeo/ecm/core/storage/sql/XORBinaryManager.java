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
import java.io.IOException;

/**
 * A simple binary manager that "hides" binaries on the filesystem by scrambling
 * them on write and unscrambling them on read using XOR.
 * <p>
 * The {@link RepositoryDescriptor} holds a key that can be used to drive the
 * scrambling/unscrambling.
 * <p>
 * This is to prevent casual reading of the files, but of course the algorithm
 * and key for scrambling are available on the system as well, so this not a
 * secure store.
 */
public class XORBinaryManager extends DefaultBinaryManager {

    protected byte[] pattern;

    @Override
    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        super.initialize(repositoryDescriptor);
        String key = repositoryDescriptor.binaryManagerKey;
        if (key == null || key.length() == 0) {
            key = "U"; // 0x55
        }
        byte[] pattern;
        try {
            pattern = key.getBytes("UTF-8");
        } catch (Exception e) {
            // cannot happen
            pattern = new byte[] { 'U' };
        }
        this.pattern = pattern;
    }

    @Override
    protected BinaryScrambler getBinaryScrambler() {
        return new XORBinaryScrambler(pattern);
    }

    /**
     * A {@link BinaryScrambler} that does an XOR with the given pattern.
     */
    public static class XORBinaryScrambler implements BinaryScrambler {

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
        public Binary getUnscrambledBinary(File file, String digest, String repoName) {
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
