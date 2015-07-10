/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Interface for an object that holds a {@link Blob}.
 */
public interface BlobHolder {

    /**
     * Returns the Blob held inside the object.
     */
    Blob getBlob();

    /**
     * Sets a blob in the object.
     * <p>
     * The underlying document must be saved by the caller.
     */
    void setBlob(Blob blob);

    /**
     * Returns a filesystem-like path to represent the held blob.
     */
    String getFilePath();

    /**
     * Returns the held blob modification date.
     */
    Calendar getModificationDate();

    /**
     * Returns a hash for the held blob.
     */
    String getHash();

    /**
     * Returns a list of blobs, if holder implementation supports multiple blobs.
     */
    List<Blob> getBlobs();

    /**
     * Returns a named property.
     */
    Serializable getProperty(String name);

    /**
     * Returns all properties as a Map.
     */
    Map<String, Serializable> getProperties();

}
