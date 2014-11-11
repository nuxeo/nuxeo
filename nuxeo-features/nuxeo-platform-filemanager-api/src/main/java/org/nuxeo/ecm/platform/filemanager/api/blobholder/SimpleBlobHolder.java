/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.filemanager.api.blobholder;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 *
 * {@link BlobHolder} implementation that simply wraps a detached {@link Blob}
 *
 * @author tiry
 *
 */
public class SimpleBlobHolder extends AbstractBlobHolder implements BlobHolder {

    protected Blob blob;
    protected Calendar creationDate;

    public SimpleBlobHolder(Blob blob) {
        this.blob=blob;
        this.creationDate = Calendar.getInstance();
    }

    @Override
    public Blob getBlob() throws ClientException {
        return blob;
    }

    protected String getBasePath() {
        return "";
    }

    @Override
    public Calendar getModificationDate() throws ClientException {
        return creationDate;
    }

}
