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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.CachableBlobHolder;

/**
 * Interface that must be implemented by any contributer Converter class.
 * <p>
 * There is only one instance of each contributed converter class: that means that the implementation must be
 * thread-safe.
 *
 * @author tiry
 */
public interface Converter {

    /**
     * Initializes the Converter.
     * <p>
     * This can be used to retrieve some configuration information from the XMap Descriptor.
     */
    void init(ConverterDescriptor descriptor);

    /**
     * Main method to handle the real Conversion Job.
     * <p>
     * Returned {@link BlobHolder} must implement {@link CachableBlobHolder}, otherwise result won't be cached.
     */
    BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException;

}
