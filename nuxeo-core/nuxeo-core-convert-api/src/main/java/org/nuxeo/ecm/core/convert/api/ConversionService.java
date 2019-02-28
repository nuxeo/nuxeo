/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.convert.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Interface for the Conversion Service.
 *
 * @author tiry
 */
public interface ConversionService {

    /**
     * Returns the converter name for the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Follows the algorithm of {@link #getConverterNames(String, String)}.
     *
     * @see #getConverterNames(String, String)
     * @see #getConverterName(String, String, boolean)
     */
    default String getConverterName(String sourceMimeType, String destinationMimeType) {
        return getConverterName(sourceMimeType, destinationMimeType, true);
    }

    /**
     * Returns the converter name for the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Follows the algorithm of {@link #getConverterNames(String, String, boolean)}.
     *
     * @since 11.1
     * @see #getConverterNames(String, String, boolean)
     */
    String getConverterName(String sourceMimeType, String destinationMimeType, boolean allowWildcard);

    /**
     * Returns the list of converter names handling the given {@code sourceMimeType} and {@code destinationMimeType}.
     *
     * @see #getConverterNames(String, String, boolean)
     */
    default List<String> getConverterNames(String sourceMimeType, String destinationMimeType) {
        return getConverterNames(sourceMimeType, destinationMimeType, true);
    }

    /**
     * Returns the list of converter names handling the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Finds the converter names based on the following algorithm:
     * <ul>
     * <li>Find the converters exactly matching the given {@code sourceMimeType}</li>
     * <li>If no converter found, find the converters matching a wildcard subtype based on the {@code sourceMimeType},
     * such has "image/*"</li>
     * <li>If no converter found and {@code allowWildcard} is {@code true}, find the converters matching a wildcard
     * source mime type "*"</li>
     * <li>Then, filter only the converters matching the given {@code destinationMimeType}</li>
     * </ul>
     *
     * @param allowWildcard {@code true} to allow returning converters with '*' as source mime type.
     * @since 11.1
     */
    List<String> getConverterNames(String sourceMimeType, String destinationMimeType, boolean allowWildcard);

    /**
     * Converts a Blob given a converter name.
     */
    BlobHolder convert(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException;

    /**
     * Converts a Blob given a target destination MimeType.
     */
    BlobHolder convertToMimeType(String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Converts a Blob to PDF. If the blob has inner blobs such as images, they will be correctly rendered in the PDF.
     *
     * @since 9.1
     * @deprecated since 9.2, use {@link #convertToMimeType(String, BlobHolder, Map)} with the PDF mimetype as
     *             destination instead
     */
    @Deprecated
    Blob convertBlobToPDF(Blob blob) throws IOException;

    /**
     * Returns the names of the registered converters.
     */
    List<String> getRegistredConverters();

    /**
     * Checks for converter availability.
     * <p>
     * Result can be:
     * <ul>
     * <li>{@link ConverterNotRegistered} if converter is not registered.
     * <li>Error Message / Installation message if converter dependencies are not available an successful check.
     * </ul>
     */
    ConverterCheckResult isConverterAvailable(String converterName, boolean refresh) throws ConverterNotRegistered;

    /**
     * Checks for converter availability.
     * <p>
     * Result can be:
     * <ul>
     * <li>{@link ConverterNotRegistered} if converter is not registered.
     * <li>Error Message / Installation message if converter dependencies are not available an successful check.
     * </ul>
     * <p>
     * Result can be taken from an internal cache.
     */
    ConverterCheckResult isConverterAvailable(String converterName) throws ConversionException;

    /**
     * Returns true if the converter supports the given {@code sourceMimeType}, false otherwise.
     *
     * @since 5.8
     */
    boolean isSourceMimeTypeSupported(String converterName, String sourceMimeType);

    /**
     * Schedules a conversion given a converter name.
     * <p>
     * Returns a conversion id to be used by {@link #getConversionResult(String, boolean)}.
     *
     * @since 7.4
     */
    String scheduleConversion(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters);

    /**
     * Schedules a conversion given a target mime type.
     * <p>
     * Returns a conversion id to be used by {@link #getConversionResult(String, boolean)}.
     *
     * @since 7.10
     */
    String scheduleConversionToMimeType(String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters);

    /**
     * Returns the status of a scheduled conversion given its {@code id}, or {@code null} if no conversion scheduled.
     *
     * @since 7.4
     */
    ConversionStatus getConversionStatus(String id);

    /**
     * Returns the conversion result for the given {@code id} if any, {@code null} otherwise.
     *
     * @since 7.4
     */
    BlobHolder getConversionResult(String id, boolean cleanTransientStoreEntry);

}
