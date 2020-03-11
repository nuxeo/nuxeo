/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot <ja@nuxeo.com>
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.platform.mimetype.interfaces;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;

/**
 * Allows the management of the mime type entry registry.
 * <p>
 * Flexible registry of mime types.
 */
public interface MimetypeRegistry {

    String DEFAULT_MIMETYPE = "application/octet-stream";

    String PDF_MIMETYPE = "application/pdf";

    String PDF_EXTENSION = ".pdf";

    /**
     * Returns the mime type from a given stream.
     *
     * @return String mime type name.
     * @throws MimetypeNotFoundException if mime type sniffing failed to identify a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromBlob(Blob blob);

    /**
     * Finds the mime type of a Blob content and returns provided default if not possible.
     *
     * @param blob content to be analyzed
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the mime type for the given blob if it exists, otherwise it returns {@code defaultMimetype}
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromBlobWithDefault(Blob blob, String defaultMimetype);

    /**
     * Returns the mime type from a given filename.
     *
     * @param filename the file name for which we are looking for the mime type
     * @return the mime type that matches the {@code filename}
     * @throws MimetypeNotFoundException if mime type sniffing failed to identify a registered mime type
     */
    String getMimetypeFromFilename(String filename);

    /**
     * Returns the mime type from a given file.
     *
     * @return the mime type of the given file
     * @throws MimetypeNotFoundException if mime type sniffing failed to identify a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromFile(File file);

    /**
     * Returns the extension for given mime type.
     *
     * @param mimetypeName the mime type name.
     * @return a list of strings containing the possible extensions.
     */
    List<String> getExtensionsFromMimetypeName(String mimetypeName);

    /**
     * Gets a mime type entry by name.
     *
     * @param name the mime type name
     * @return the mime type entry that matches the mime type name if it exists, {@code null} otherwise
     */
    MimetypeEntry getMimetypeEntryByName(String name);

    /**
     * Gets the {@link MimetypeEntry} for a given mime type.
     *
     * @param mimetype the mime type for which we are looking for the mime type entry
     * @return the mime type entry if it exists, {@link #DEFAULT_MIMETYPE} otherwise
     */
    MimetypeEntry getMimetypeEntryByMimeType(String mimetype);

    /**
     * Finds the mime type of some content according to its filename and / or binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromFilenameAndBlobWithDefault(String filename, Blob blob, String defaultMimetype);

    /**
     * Finds the mime type of some content according to its filename or binary mime type or binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     * @since 8.4
     */
    String getMimetypeFromFilenameWithBlobMimetypeFallback(String filename, Blob blob, String defaultMimetype);

    /**
     * Updates the mime type field of a Blob based on the provided filename with fallback to binary. If the embedded
     * filename is {@code null}, the provided filename is embedded into the blob as well.
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @param filename with extension to analyze
     * @param withBlobMimetypeFallback to consider blob mime type as fallback or not
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     * @since 8.4
     */
    Blob updateMimetype(Blob blob, String filename, Boolean withBlobMimetypeFallback);

    /**
     * Updates the mime type field of a Blob based on the provided filename with fallback to binary sniffing. If the
     * embedded filename is {@code null}, the provided filename is embedded into the blob as well.
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @param filename with extension to analyze
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    Blob updateMimetype(Blob blob, String filename);

    /**
     * Updates the mime type field of a Blob based on the embedded filename with fallback to binary sniffing. This
     * method should not be called if the embedded filename is {@code null} for performance reasons (+ the fact that
     * binary sniffing is no very reliable).
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    Blob updateMimetype(Blob blob);

    /**
     * Returns the mime type from a given extension.
     *
     * @param extension the extension for which we are looking for the mime type
     * @return the mime type for the given extension if it exists
     * @throws MimetypeNotFoundException if mime type sniffing failed to identify a registered mime type
     * @since 7.3
     */
    String getMimetypeFromExtension(String extension);

    /**
     * Retrieves the normalized mime type for the given {@code mimeType}.
     *
     * @param mimeType the mime for which we are looking for the normalized one
     * @return an {@code Optional} with a present value if the normalized mime type can be found, otherwise an
     *         empty {@code Optional}
     * @since 11.1
     */
    Optional<String> getNormalizedMimeType(String mimeType);

    /**
     * Checks if the given {@code mimeType} is a normalized one.
     *
     * @param mimeType the mime type to check
     * @return {@code true} if {@code mimeType} is normalized, {@code false} otherwise
     * @since 11.1
     */
    boolean isMimeTypeNormalized(String mimeType);

}
