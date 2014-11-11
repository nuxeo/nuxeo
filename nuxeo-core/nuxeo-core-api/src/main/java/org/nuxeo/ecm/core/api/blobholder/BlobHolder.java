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
package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for an object that holds a {@link Blob}.
 *
 * @author tiry
 */
public interface BlobHolder {

    /**
     * Returns the Blob held inside the object.
     */
    Blob getBlob() throws ClientException;

    /**
     * Returns a filesystem-like path to represent the held blob.
     */
    String getFilePath() throws ClientException;

    /**
     * Returns the held blob modification date.
     */
    Calendar getModificationDate() throws ClientException;

    /**
     * Returns a hash for the held blob.
     */
    String getHash() throws ClientException;

    /**
     * Returns a list of blobs, if holder implementation supports multiple
     * blobs.
     */
    List<Blob> getBlobs() throws ClientException;

    /**
     * Returns a named property.
     */
    Serializable getProperty(String name) throws ClientException;

    /**
     * Returns all properties as a Map.
     */
    Map<String, Serializable> getProperties();

}
