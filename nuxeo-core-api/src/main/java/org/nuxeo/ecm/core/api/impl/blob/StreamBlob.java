/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Vector;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class StreamBlob extends AbstractBlob {

    public static byte[] readBytes(InputStream in) throws IOException {
        List<byte[]> v = new Vector<byte[]>();
        byte[] buffer = new byte[BUFFER_SIZE];
        int w = 0;
        try {
            int read = 0;
            int len;
            do {
                w += read;
                len = BUFFER_SIZE - w;
                if (len <= 0) {
                    v.add(buffer);
                    buffer = new byte[BUFFER_SIZE];
                    len = BUFFER_SIZE;
                    w = 0;
                }
            } while ((read = in.read(buffer, w, len)) != -1);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        byte[] ret = new byte[v.size()*BUFFER_SIZE + w];
        for (int i = 0, size = v.size(); i < size; i++) {
            byte[] tmp = v.get(i);
            System.arraycopy(tmp, 0, ret, i*BUFFER_SIZE, BUFFER_SIZE);
        }
        System.arraycopy(buffer, 0, ret, v.size()*BUFFER_SIZE, w);
        return ret;
    }

    public static byte[] readBytes(Reader reader) throws IOException {
        List<char[]> v = new Vector<char[]>();
        char[] buffer = new char[BUFFER_SIZE];
        int w = 0;
        try {
            int read = 0;
            int len;
            do {
                w += read;
                len = BUFFER_SIZE - w;
                if (len <= 0) {
                    v.add(buffer);
                    buffer = new char[BUFFER_SIZE];
                    len = BUFFER_SIZE;
                    w = 0;
                }
            } while ((read = reader.read(buffer, w, len)) != -1);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        byte[] ret = new byte[v.size()*BUFFER_SIZE + w];
        for (int i = 0, size = v.size(); i < size; i++) {
            char[] tmp = v.get(i);
            System.arraycopy(tmp, 0, ret, i*BUFFER_SIZE, BUFFER_SIZE);
        }
        System.arraycopy(buffer, 0, ret, v.size()*BUFFER_SIZE, w);
        return ret;
    }


//    public static byte[] readBytes(InputStream in) throws IOException {
//        byte[] buffer = new byte[BUFFER_SIZE];
//        int read = 0, w = 0, len;
//        try {
//            do {
//                w += read;
//                len = buffer.length - w;
//                if (len <= 0) { // resize buffer
//                    byte[] b = new byte[buffer.length+BUFFER_SIZE];
//                    System.arraycopy(buffer, 0, b, 0, w);
//                    buffer = b;
//                    len = buffer.length - w;
//                }
//            } while((read = in.read(buffer, w, len)) != -1);
//        } finally {
//            if(in != null) {
//                in.close();
//            }
//        }
//        if (buffer.length > w) { // compact buffer
//            byte[] b = new byte[w];
//            System.arraycopy(buffer, 0, b, 0, w);
//            buffer = b;
//        }
//        return buffer;
//    }

//    public static char[] readChars(Reader reader) throws IOException {
//        char[] buffer = new char[BUFFER_SIZE];
//        int read = 0, w = 0, len;
//        try {
//            do {
//                w += read;
//                len = buffer.length - w;
//                if (len <= 0) { // resize buffer
//                    char[] b = new char[buffer.length+BUFFER_SIZE];
//                    System.arraycopy(buffer, 0, b, 0, w);
//                    buffer = b;
//                    len = buffer.length - w;
//                }
//            } while((read = reader.read(buffer, w, len)) != -1);
//        } finally {
//            if(reader != null) {
//                reader.close();
//            }
//        }
//        if (buffer.length > w) { // compact buffer
//            char[] b = new char[w];
//            System.arraycopy(buffer, 0, b, 0, w);
//            buffer = b;
//        }
//        return buffer;
//    }

    public static String readString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(BUFFER_SIZE);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer, 0, BUFFER_SIZE)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

    public byte[] getByteArray() throws IOException {
        InputStream in = getStream();
        if (in == null || in.available() == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return readBytes(in);
    }

    public String getString() throws IOException {
        Reader reader = getReader();
        if (reader == null || reader == EMPTY_READER) {
            return EMPTY_STRING;
        }
        return readString(reader);
    }

    public Reader getReader() throws IOException {
        InputStream in = getStream();
        if (in == null || in.available() == 0) {
            return EMPTY_READER;
        }
        String enc = getEncoding();
        return enc == null ? new InputStreamReader(in)
                : new InputStreamReader(in, enc);
    }

    public long getLength() {
        return -1;
    }

    public ByteArrayBlob asByteArrayBlob() throws IOException {
        return new ByteArrayBlob(getByteArray(), getMimeType(), getEncoding());
    }

    public StringBlob asStringBlob() throws IOException {
        return new StringBlob(getString(), getMimeType(), getEncoding());
    }

}
