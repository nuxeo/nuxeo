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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractBlob implements Blob {

    public static final String EMPTY_STRING = "";
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final InputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
    public static final Reader EMPTY_READER = new StringReader(EMPTY_STRING);

    protected static final int BUFFER_SIZE = 4096*16;
    //protected static int BUFFER_SIZE = 16;


    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void copy(Reader in, Writer out) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void transferTo(Writer writer) throws IOException {
        Reader reader = getReader();
        if (reader != null && reader != EMPTY_READER) {
            try {
                copy(reader, writer);
            } finally {
                reader.close();
            }
        }
    }

    @Override
    public void transferTo(OutputStream out) throws IOException {
        InputStream in = getStream();
        if (in != null && in != EMPTY_INPUT_STREAM) {
            try {
                copy(in, out);
            } finally {
                in.close();
            }
        }
    }

    @Override
    public void transferTo(File file) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            transferTo(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

}
