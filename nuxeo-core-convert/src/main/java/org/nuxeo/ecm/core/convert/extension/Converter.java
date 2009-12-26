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
package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.CachableBlobHolder;

/**
 * Interface that must be implemented by any contributer Converter class.
 * <p>
 * There is only one instance of each contributed converter class: that means
 * that the implementation must be thread-safe.
 *
 * @author tiry
 */
public interface Converter {

    /**
     * Initializes the Converter.
     * <p>
     * This can be used to retrieve some configuration information from the XMap
     * Descriptor.
     */
    void init(ConverterDescriptor descriptor);

    /**
     * Main method to handle the real Conversion Job.
     * <p>
     * Returned {@link BlobHolder} must implement {@link CachableBlobHolder},
     * otherwise result won't be cached.
     */
    BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException;

}
