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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Interface to implement when defining a way to get a {@link Blob} that is not stored at the usual place handled by the
 * repository.
 * <p>
 * This is done storing a string instead of a blob, using a prefix that makes it possible to find the adapter in charge
 * of retrieving the file. It makes it also possible to use the same adapter implementation for different
 * configurations, in conjunction with properties.
 * <p>
 * The string will look something like "fs:file/foo.odt", "fs" being the prefix, and "file/foo.odt" being the local
 * name, with all the needed information to retrieve the actual file for this adapter.
 *
 * @author Anahide Tchertchian
 */
public interface ExternalBlobAdapter extends Serializable {

    String PREFIX_SEPARATOR = ":";

    /**
     * Returns the prefix to use as marker for this adapter
     */
    String getPrefix();

    /**
     * Sets the prefix to use as marker for this adapter
     */
    void setPrefix(String prefix);

    /**
     * Return specific properties for this adapter. Can be used for whatever useful property.
     */
    Map<String, String> getProperties();

    /**
     * Return property with gievn name.
     * <p>
     * Returns null if no property value is found.
     */
    String getProperty(String name);

    /**
     * Set specific properties for this adapter. Can be used for whatever useful property.
     */
    void setProperties(Map<String, String> properties);

    /**
     * Retrieves the blob for given uri.
     *
     * @param uri the uri describing what adapter handles the file and the needed info to retrieve it.
     * @return the resolved blob.
     * @throws PropertyException if the blob cannot be retrieved (if adapter cannot retrieve it or if file is not found
     *             for instance)
     */
    Blob getBlob(String uri) throws PropertyException, IOException;

}
