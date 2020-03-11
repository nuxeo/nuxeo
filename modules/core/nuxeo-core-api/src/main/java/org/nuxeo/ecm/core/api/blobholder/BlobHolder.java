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
