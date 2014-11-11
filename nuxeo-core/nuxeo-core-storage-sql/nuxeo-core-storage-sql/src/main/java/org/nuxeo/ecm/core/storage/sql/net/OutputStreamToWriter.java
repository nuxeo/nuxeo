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
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * OutputStream that actually writes chars to a writer. Bytes are converted to
 * chars using ISO-8859-1 encoding (direct).
 */
public class OutputStreamToWriter extends OutputStream {

    protected final Writer writer;

    public OutputStreamToWriter(Writer writer) {
        this.writer = writer;
    }

    // debug
    private final StringBuilder sb = new StringBuilder();

    // debug
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    // debug
    private void dump(int ub) {
        if (ub <= 0x20 || ub >= 0x80) {
            sb.append("0x");
            sb.append(HEX_DIGITS[(0xF0 & ub) >> 4]);
            sb.append(HEX_DIGITS[0x0F & ub]);
        } else {
            sb.append((char) ub);
        }
        sb.append(' ');
    }

    // debug
    private void flushDebug() {
        if (sb.length() > 0) {
            System.out.println("> " + sb.toString());
            sb.setLength(0);
        }
    }

    protected char getChar(byte b) {
        int ub = 0xFF & b;
        // dump(ub); // debug
        return (char) ub;
    }

    @Override
    public void write(int b) throws IOException {
        writer.write(getChar((byte) b));
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            writer.write(getChar(bytes[off + i]));
        }
    }

    @Override
    public void flush() throws IOException {
        // flushDebug(); // debug
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        // flushDebug(); // debug
        writer.close();
    }

}
