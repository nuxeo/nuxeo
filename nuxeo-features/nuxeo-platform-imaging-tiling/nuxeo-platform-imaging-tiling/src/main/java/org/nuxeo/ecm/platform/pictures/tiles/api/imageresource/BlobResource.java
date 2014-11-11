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

/**
 * Blob based implementation of the ImageResource Because ImageResource will be
 * cached this Implementation is not optimal (Blob digest is not compulsory and
 * the modification date is not set).
 * <p>
 * This implementation is mainly used for unit testing.
 *
 * @author tiry
 */
public class BlobResource implements ImageResource {

    private static final long serialVersionUID = 1L;

    protected Blob blob;

    protected String hash;

    protected Calendar modified;

    public BlobResource(Blob blob) {
        this.blob = blob;
        if (blob.getDigest() != null) {
            hash = blob.getDigest();
        } else {
            hash = blob.hashCode() + "fakeHash";
        }

        modified = Calendar.getInstance();
    }

    public Blob getBlob() throws ClientException {
        return blob;
    }

    public String getHash() throws ClientException {
        return hash;
    }

    public Calendar getModificationDate() throws ClientException {
        return modified;
    }

}
