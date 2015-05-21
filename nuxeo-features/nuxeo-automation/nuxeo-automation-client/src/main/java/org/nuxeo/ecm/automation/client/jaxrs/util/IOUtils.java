/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

/**
 * File is deleted on JVM exit. You should delete it explicitly earlier if you know it won't be used anymore.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IOUtils {

    private static final int BUFFER_SIZE = 1024 * 64; // 64K

    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K

    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K

    private IOUtils() {
    }

    private static byte[] createBuffer(int preferredSize) {
        if (preferredSize < 1) {
            preferredSize = BUFFER_SIZE;
        }
        if (preferredSize > MAX_BUFFER_SIZE) {
            preferredSize = MAX_BUFFER_SIZE;
        } else if (preferredSize < MIN_BUFFER_SIZE) {
            preferredSize = MIN_BUFFER_SIZE;
        }
        return new byte[preferredSize];
    }

    public static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = createBuffer(in.available());
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static File copyToTempFile(InputStream in) throws IOException {
        File file = File.createTempFile("nxautomation-", ".tmp");
        file.deleteOnExit();
        copyToFile(in, file, true);
        return file;
    }

    public static File copyToTempFile(InputStream in, boolean closeIn)
            throws IOException {
        File file = File.createTempFile("nxautomation-", ".tmp");
        file.deleteOnExit();
        copyToFile(in, file, closeIn);
        return file;
    }

    public static void copyToFile(InputStream in, File file) throws IOException {
        copyToFile(in, file, true);
    }

    public static void copyToFile(InputStream in, File file, boolean closeIn)
            throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
            if (closeIn) {
                in.close();
            }
        }
    }

    public static void writeToFile(String content, File file)
            throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            write(content, out);
        } finally {
            out.close();
        }
    }

    public static void write(String content, OutputStream out)
            throws IOException {
        out.write(content.getBytes());
    }

    public static String read(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, "UTF-8");
        return read(reader);
    }

    public static String read(Reader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            int read;
            char[] buffer = new char[64 * 1024];
            while ((read = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

}
