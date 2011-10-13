/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

public class BlobHolderDummyImpl extends AbstractBlobHolder {

    @Override
    protected String getBasePath() {
        return "Test";
    }

    @Override
    public Blob getBlob() throws ClientException {
        return new StringBlob("Test");
    }

    @Override
    public Calendar getModificationDate() throws ClientException {
        return null;
    }

    @Override
    public Serializable getProperty(String name) {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

}
