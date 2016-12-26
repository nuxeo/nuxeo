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
 *     Julien Anguenot <ja@nuxeo.com>
 *     Estelle Giuly <egiuly@nuxeo.com>
 *
 * $Id: MimetypeRegistry.java 20731 2007-06-18 15:13:32Z ogrisel $
 */
package org.nuxeo.ecm.platform.mimetype.interfaces;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;

/**
 * MimetypeEntry registry.
 * <p>
 * Flexible registry of mimetypes.
 */
public interface MimetypeRegistry {

    String DEFAULT_MIMETYPE = "application/octet-stream";

    String PDF_MIMETYPE = "application/pdf";

    String PDF_EXTENSION = ".pdf";

    /**
     * Returns the mime type from a given stream.
     *
     * @return String mimetype name.
     * @throws MimetypeNotFoundException if mimetype sniffing failed to identify a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromStream(InputStream stream) throws MimetypeNotFoundException, MimetypeDetectionException;

    /**
     * Returns the mime type from a given stream.
     *
     * @return String mimetype name.
     * @throws MimetypeNotFoundException if mimetype sniffing failed to identify a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromBlob(Blob blob) throws MimetypeNotFoundException, MimetypeDetectionException;

    /**
     * Finds the mimetype of a Blob content and returns provided default if not possible.
     *
     * @param blob content to be analyzed
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     */
    String getMimetypeFromBlobWithDefault(Blob blob, String defaultMimetype) throws MimetypeDetectionException;

    /**
     * Returns the mime type from a given stream or provided default if not possible.
     *
     * @throws MimetypeDetectionException
     */
    String getMimetypeFromStreamWithDefault(InputStream is, String defaultMimetype) throws MimetypeDetectionException;

    /**
     * Returns the mime type from a given filename.
     */
    String getMimetypeFromFilename(String filename);

    /**
     * Returns the mime type given a file.
     *
     * @return string containing the mime type
     * @throws MimetypeNotFoundException if mimetype sniffing failed
     * @throws MimetypeDetectionException if unexpected problem prevent the detection to work as expected
     */
    String getMimetypeFromFile(File file) throws MimetypeNotFoundException, MimetypeDetectionException;

    /**
     * Returns the extension for given mimetype.
     *
     * @param mimetypeName the mimetype name.
     * @return a list of strings containing the possible extensions.
     */
    List<String> getExtensionsFromMimetypeName(String mimetypeName);

    /**
     * Gets a mimetype entry by name.
     *
     * @param name the mimetype name.
     * @return mimetype instance
     */
    MimetypeEntry getMimetypeEntryByName(String name);

    /**
     * Gets a mimetype entry given the normalized mimetype.
     *
     * @param mimetype the normalized mimetype
     * @return mimetype instance
     */
    MimetypeEntry getMimetypeEntryByMimeType(String mimetype);

    /**
     * Finds the mimetype of some content according to its filename and / or binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     */
    String getMimetypeFromFilenameAndBlobWithDefault(String filename, Blob blob, String defaultMimetype)
            throws MimetypeDetectionException;

    /**
     * Finds the mimetype of some content according to its filename or binary mime type or binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     * @since 8.4
     */
    String getMimetypeFromFilenameWithBlobMimetypeFallback(String filename, Blob blob, String defaultMimetype)
            throws MimetypeDetectionException;

    /**
     * Update the mimetype field of a Blob based on the provided filename with fallback to binary. If the embedded
     * filename is null, the provided filename is embedded into the blob as well.
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @param filename with extension to analyze
     * @param withBlobMimetypeFallback to consider blob mimetype as fallback or not
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException
     * @since 8.4
     */
    Blob updateMimetype(Blob blob, String filename, Boolean withBlobMimetypeFallback) throws MimetypeDetectionException;

    /**
     * Update the mimetype field of a Blob based on the provided filename with fallback to binary sniffing. If the
     * embedded filename is null, the provided filename is embedded into the blob as well.
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @param filename with extension to analyze
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException
     */
    Blob updateMimetype(Blob blob, String filename) throws MimetypeDetectionException;

    /**
     * Update the mimetype field of a Blob based on the embedded filename with fallback to binary sniffing. This method
     * should not be called if the embedded filename is null for performance reasons (+ the fact that binary sniffing is
     * no very reliable).
     *
     * @param blob content to be analyzed if filename is ambiguous
     * @return updated blob (persisted if necessary)
     * @throws MimetypeDetectionException
     */
    Blob updateMimetype(Blob blob) throws MimetypeDetectionException;

    /**
     * Returns the mime type from a given extension.
     *
     * @since 7.3
     */
    String getMimetypeFromExtension(String extension) throws MimetypeNotFoundException;

}
