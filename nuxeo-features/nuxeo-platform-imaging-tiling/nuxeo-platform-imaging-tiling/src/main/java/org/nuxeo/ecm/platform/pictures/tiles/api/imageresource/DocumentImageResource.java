/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.imageresource;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * DocumentModel-based implementation of ImageResource.
 * Supports clean digest and modification date to have a clean invalidation system.
 *
 * @author tiry
 */
public class DocumentImageResource implements ImageResource {

    private static final long serialVersionUID = 1L;

    protected Blob blob;
    protected String hash;
    protected Calendar modified;

    protected DocumentModel doc;
    protected String xPath;
    protected String fileName;

    public DocumentImageResource(DocumentModel doc, String xPath) {
        this.doc = doc;
        this.xPath = xPath;
    }

    protected String getEscapedxPath(String xPath) {
        String clean = xPath.replace(":", "_");
        clean = clean.replace("/", "_");
        clean = clean.replace("[", "");
        clean = clean.replace("]", "");
        return clean;
    }

    protected void compute() throws PropertyException, ClientException {

        blob = (Blob) doc.getProperty(xPath).getValue();
        modified = (Calendar) doc.getProperty("dublincore", "modified");

        hash = blob.getDigest();
        if (hash == null) {
            hash = doc.getRepositoryName() + "_" + doc.getId() + "_"
                    + getEscapedxPath(xPath);
            if (modified != null) {
                hash = hash + "_" + modified.getTimeInMillis();
            }
        }
    }

    public Blob getBlob() throws ClientException {
        if (blob == null) {
            compute();
        }
        if (fileName!=null) {
            blob.setFilename(fileName);
        }
        return blob;
    }

    public String getHash() throws ClientException {
        if (hash == null) {
            compute();
        }
        return hash;
    }

    public Calendar getModificationDate() throws ClientException {
        if (modified == null) {
            compute();
        }
        return modified;
    }

    public void setFileName(String name) {
        this.fileName=name;
    }

}
