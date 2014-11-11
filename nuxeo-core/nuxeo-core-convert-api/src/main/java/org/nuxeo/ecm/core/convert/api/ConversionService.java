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
package org.nuxeo.ecm.core.convert.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Interface for the Conversion Service
 *
 * @author tiry
 */
public interface ConversionService {

    /**
     * Gets the convertName given a source and destination MimeType
     *
     * @param sourceMimeType
     * @param destinationMimeType
     * @return
     */
    String getConverterName(String sourceMimeType, String destinationMimeType);

    /**
     * Do a Blob conversion given a converter name.
     *
     * @param converterName
     * @param blobHolder
     * @param parameters
     * @return
     * @throws ConversionException
     */
    BlobHolder convert(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Do a Blob conversion given a target destination MimeType.
     *
     * @param destinationMimeType
     * @param blobHolder
     * @param parameters
     * @return
     * @throws ConversionException
     */
    BlobHolder convertToMimeType(String destinationMimeType,
            BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException;

    /**
     * Returns the names of the registered converters.
     *
     * @return
     */
    List<String> getRegistredConverters();

    /**
     * Checks for converter availability.
     * <p>
     * Result can be:
     * <ul>
     * <li>{@link ConverterNotRegistered} if converter is not registered.
     * <li>Error Message / Installation message if converter dependencies are
     * not available an successful check.
     * </ul>
     *
     * @param converterName
     * @param refresh
     * @return
     */
    ConverterCheckResult isConverterAvailable(String converterName,
            boolean refresh) throws ConversionException;

    /**
     * Checks for converter availability.
     * <p>
     * Result can be:
     * <ul>
     * <li>{@link ConverterNotRegistered} if converter is not registered.
     * <li>Error Message / Installation message if converter dependencies are
     * not available an successful check.
     * </ul>
     * <p>
     * Result can be taken from an internal cache.
     *
     * @param converterName
     * @return
     */
    ConverterCheckResult isConverterAvailable(String converterName)
            throws ConversionException;

}
