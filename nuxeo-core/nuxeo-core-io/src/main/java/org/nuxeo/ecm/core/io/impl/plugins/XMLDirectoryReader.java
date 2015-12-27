/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     btatar
 *
 * $Id: XMLDirectoryReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.FileTreeIterator;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * @author bs@nuxeo.com
 */
public class XMLDirectoryReader extends AbstractDocumentReader {

    private File source;

    private FileTreeIterator iterator;

    public XMLDirectoryReader(String sourcePath) {
        this(new File(sourcePath));
    }

    public XMLDirectoryReader(File source) {
        this.source = source;
        iterator = new FileTreeIterator(source);
        iterator.setFilter(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
    }

    public Object getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    @Override
    public void close() {
        source = null;
        iterator = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (iterator.hasNext()) {
            File dir = iterator.next();
            if (dir == null) {
                return null;
            }
            // read document files
            ExportedDocument xdoc = new ExportedDocumentImpl();
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName();
                    if (ExportConstants.DOCUMENT_FILE.equals(name)) {
                        Document doc = loadXML(file);
                        xdoc.setDocument(doc);
                        /*
                         * NXP-1688 Rux: the path was somehow left over when migrated from core 1.3.4 to 1.4.0. Pull
                         * back.
                         */
                        xdoc.setPath(computeRelativePath(dir));
                    } else if (name.endsWith(".xml")) {
                        xdoc.putDocument(FileUtils.getFileNameNoExt(file.getName()), loadXML(file));
                    } else { // presume a blob
                        xdoc.putBlob(file.getName(), Blobs.createBlob(file));
                    }
                }
            }
            return xdoc;
        }
        return null;
    }

    /*
     * NXP-1688 Rux: the path was somehow left over when migrated from core 1.3.4 to 1.4.0. Pull back.
     */
    private Path computeRelativePath(File file) {
        /* NXP-2507 Rux: preserve directory structure with slashes instead OS name separator */
        String subPathS = file.getAbsolutePath().substring(source.getAbsolutePath().length());
        subPathS = subPathS.replace(File.separatorChar, '/');
        return new Path(subPathS);
    }

    private static Document loadXML(File file) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            return new SAXReader().read(in);
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read file document " + file + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
