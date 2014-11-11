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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An iterator over the entries in a ZIP file.
 * <p>
 * The iterator support filtering using {@link ZipEntryFilter}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ZipIterator implements Iterator<ZipEntry> {

    private final ZipInputStream zin;
    private final ZipEntryFilter filter;
    private ZipEntry zentry;

    public ZipIterator(ZipInputStream zin, ZipEntryFilter filter) throws IOException {
        this.zin = zin;
        this.filter = filter;
        zentry = zin.getNextEntry();
    }

    public ZipIterator(URL url) throws IOException {
        this(url.openStream(), null);
    }

    public ZipIterator(URL url, ZipEntryFilter filter) throws IOException {
        this(url.openStream(), filter);
    }

    public ZipIterator(File file) throws IOException {
        this(new FileInputStream(file), null);
    }

    public ZipIterator(File file, ZipEntryFilter filter) throws IOException {
        this(new FileInputStream(file), filter);
    }

    public ZipIterator(InputStream in) throws IOException {
        this(new ZipInputStream(in), null);
    }

    public ZipIterator(InputStream in, ZipEntryFilter filter) throws IOException {
        this(new ZipInputStream(in), filter);
    }


    public ZipEntry getNextEntry() throws IOException {
        ZipEntry ze = zin.getNextEntry();
        if (ze == null) {
            return null;
        }
        if (filter != null) {
            while (!filter.accept(ze.getName())) {
                ze = zin.getNextEntry();
                if (ze == null) {
                    return null;
                }
            }
        }
        return ze;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove is not supported by this iterator");
    }

    public boolean hasNext() {
        return zentry != null;
    }

    public ZipEntry next() {
        ZipEntry oldEntry = zentry;
        try {
            zentry = getNextEntry();
        } catch (IOException e) {
            e.printStackTrace();
            zentry = null;
        }
        return oldEntry;
    }

    public InputStream getInputStream() {
        return zin;
    }

    public void close() {
        if (zin != null) {
            try {
                zin.close();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }

}
