/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.storage.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Abstract BinaryManager implementation that provides a few utilities
 *
 * @author Florent Guillaume
 */
public abstract class AbstractBinaryManager implements BinaryManager {

    public static final String DEFAULT_DIGEST = "MD5"; // "SHA-256"

    public static final int DEFAULT_DEPTH = 2;

    protected String repositoryName;

    protected BinaryManagerRootDescriptor descriptor;

    protected BinaryGarbageCollector garbageCollector;

    @Override
    abstract public void initialize(BinaryManagerDescriptor descriptor)
            throws IOException;

    @Override
    abstract public Binary getBinary(InputStream in) throws IOException;

    @Override
    abstract public Binary getBinary(String digest);

    /**
     * Gets existing descriptor or creates a default one.
     */
    protected BinaryManagerRootDescriptor getDescriptor(File configFile)
            throws IOException {
        BinaryManagerRootDescriptor desc;
        if (configFile.exists()) {
            XMap xmap = new XMap();
            xmap.register(BinaryManagerRootDescriptor.class);
            try {
                desc = (BinaryManagerRootDescriptor) xmap.load(new FileInputStream(
                        configFile));
            } catch (Exception e) {
                throw (IOException) new IOException().initCause(e);
            }
        } else {
            desc = new BinaryManagerRootDescriptor();
            // TODO fetch from repo descriptor
            desc.digest = DEFAULT_DIGEST;
            desc.depth = DEFAULT_DEPTH;
            desc.write(configFile); // may throw IOException
        }
        return desc;
    }

    protected BinaryScrambler getBinaryScrambler() {
        return NullBinaryScrambler.INSTANCE;
    }

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB

    protected String storeAndDigest(InputStream in, OutputStream out)
            throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(descriptor.digest);
        } catch (NoSuchAlgorithmException e) {
            throw (IOException) new IOException().initCause(e);
        }

        int size = in.available();
        if (size == 0) {
            size = MAX_BUF_SIZE;
        } else if (size < MIN_BUF_SIZE) {
            size = MIN_BUF_SIZE;
        } else if (size > MAX_BUF_SIZE) {
            size = MAX_BUF_SIZE;
        }
        byte[] buf = new byte[size];

        /*
         * Scramble, copy and digest.
         */
        BinaryScrambler scrambler = getBinaryScrambler();
        int n;
        while ((n = in.read(buf)) != -1) {
            scrambler.scrambleBuffer(buf, 0, n);
            digest.update(buf, 0, n);
            out.write(buf, 0, n);
        }
        out.flush();

        return toHexString(digest.digest());
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    @Override
    public BinaryGarbageCollector getGarbageCollector() {
        return garbageCollector;
    }

    /**
     * A {@link BinaryScrambler} that does nothing.
     */
    public static class NullBinaryScrambler implements BinaryScrambler {
        private static final long serialVersionUID = 1L;

        public static final BinaryScrambler INSTANCE = new NullBinaryScrambler();

        @Override
        public void scrambleBuffer(byte[] buf, int off, int n) {
        }

        @Override
        public void unscrambleBuffer(byte[] buf, int off, int n) {
        }

        @Override
        public Binary getUnscrambledBinary(File file, String digest,
                String repoName) {
            return new Binary(file, digest, repoName);
        }

        @Override
        public void skip(long n) {
        }

        @Override
        public void reset() {
        }
    }

    /**
     * A {@link Binary} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledBinary extends Binary {

        private static final long serialVersionUID = 1L;

        private final File file;

        protected final BinaryScrambler scrambler;

        public ScrambledBinary(File file, String digest, String repoName,
                BinaryScrambler scrambler) {
            super(file, digest, repoName);
            this.file = file;
            this.scrambler = scrambler;
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ScrambledFileInputStream(file, scrambler);
        }

        @Override
        public StreamSource getStreamSource() {
            return new ScrambledStreamSource(file, scrambler);
        }

    }

    /**
     * A {@link FileSource} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledStreamSource extends FileSource {

        protected final BinaryScrambler scrambler;

        public ScrambledStreamSource(File file, BinaryScrambler scrambler) {
            super(file);
            this.scrambler = scrambler;
        }

        @Override
        public File getFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ScrambledFileInputStream(file, scrambler);
        }
    }

    /**
     * A {@link FileInputStream} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledFileInputStream extends InputStream {

        protected final InputStream is;

        protected final BinaryScrambler scrambler;

        protected final byte[] onebyte = new byte[1];

        protected ScrambledFileInputStream(File file, BinaryScrambler scrambler)
                throws IOException {
            is = new FileInputStream(file);
            this.scrambler = scrambler;
            scrambler.reset();
        }

        @Override
        public int read() throws IOException {
            int b = is.read();
            if (b != -1) {
                onebyte[0] = (byte) b;
                scrambler.unscrambleBuffer(onebyte, 0, 1);
                b = onebyte[0];
            }
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = is.read(b, off, len);
            if (n != -1) {
                scrambler.unscrambleBuffer(b, off, n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            n = is.skip(n);
            scrambler.skip(n);
            return n;
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        @Override
        public void close() throws IOException {
            is.close();
        }
    }

}
