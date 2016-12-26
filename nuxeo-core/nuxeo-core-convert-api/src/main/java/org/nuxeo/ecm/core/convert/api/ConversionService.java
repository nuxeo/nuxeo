/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Gets the convertName given a source and destination MimeType.
     */
    String getConverterName(String sourceMimeType, String destinationMimeType);

    /**
     * Gets the available convertNames given a source and destination MimeType.
     */
    List<String> getConverterNames(String sourceMimeType, String destinationMimeType);

    /**
     * Converts a Blob given a converter name.
     */
    BlobHolder convert(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException;

    /**
     * Converts a Blob given a target destination MimeType.
     */
    BlobHolder convertToMimeType(String destinationMimeType, BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException;

    /**
     * Converts a Blob to PDF. If the blob has inner blobs such as images, they will be correctly rendered in the PDF.
     *
     * @since 9.1
     */
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
    String scheduleConversionToMimeType(String destinationMimeType, BlobHolder blobHolder, Map<String, Serializable> parameters);

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
