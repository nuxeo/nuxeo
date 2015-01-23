/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.Blob;

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
    abstract public void initialize(BinaryManagerDescriptor binaryManagerDescriptor) throws IOException;

    @Override
    abstract public Binary getBinary(InputStream in) throws IOException;

    /*
     * This abstract implementation just opens the stream.
     */
    @Override
    public Binary getBinary(Blob blob) throws IOException {
        try (InputStream stream = blob.getStream()) {
            return getBinary(stream);
        }
    }

    @Override
    abstract public Binary getBinary(String digest);

    /**
     * Gets existing descriptor or creates a default one.
     */
    protected BinaryManagerRootDescriptor getDescriptor(File configFile) throws IOException {
        BinaryManagerRootDescriptor desc;
        if (configFile.exists()) {
            XMap xmap = new XMap();
            xmap.register(BinaryManagerRootDescriptor.class);
            desc = (BinaryManagerRootDescriptor) xmap.load(new FileInputStream(configFile));
        } else {
            desc = new BinaryManagerRootDescriptor();
            // TODO fetch from repo descriptor
            desc.digest = getDigest();
            desc.depth = DEFAULT_DEPTH;
            desc.write(configFile); // may throw IOException
        }
        return desc;
    }

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB

    protected String storeAndDigest(InputStream in, OutputStream out) throws IOException {
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
         * Copy and digest.
         */
        int n;
        while ((n = in.read(buf)) != -1) {
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
     * Gets the message digest to use to hash binaries.
     *
     * @since 6.0
     */
    protected String getDigest() {
        return DEFAULT_DIGEST;
    }

}
