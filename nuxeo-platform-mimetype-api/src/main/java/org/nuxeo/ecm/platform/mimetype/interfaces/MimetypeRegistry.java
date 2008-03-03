/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * @author <a href="ja@nuxeo.com">Julien Anguenot</a>
 */
public interface MimetypeRegistry {

    /**
     * Returns the mime type from a given stream.
     *
     * @return String mimetype name.
     * @throws MimetypeNotFoundException if mimetype sniffing failed to identify
     *             a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the
     *             detection to work as expected
     */
    String getMimetypeFromStream(InputStream stream)
            throws MimetypeNotFoundException, MimetypeDetectionException;

    /**
     * Returns the mime type from a given stream.
     *
     * @return String mimetype name.
     * @throws MimetypeNotFoundException if mimetype sniffing failed to identify
     *             a registered mime type
     * @throws MimetypeDetectionException if unexpected problem prevent the
     *             detection to work as expected
     */
    String getMimetypeFromBlob(Blob blob)
            throws MimetypeNotFoundException, MimetypeDetectionException;

    /**
     * Returns the mime type from a given blob or provided default if not
     * possible.
     *
     * @throws MimetypeDetectionException
     */
    String getMimetypeFromBlobWithDefault(Blob blob,
            String defaultMimetype) throws MimetypeDetectionException;

    /**
     * Returns the mime type from a given stream or provided default if not
     * possible.
     *
     * @throws MimetypeDetectionException
     */
    String getMimetypeFromStreamWithDefault(InputStream is,
            String defaultMimetype) throws MimetypeDetectionException;

    /**
     * Returns the mime type given a file.
     *
     * @return string containing the mime type
     * @throws MimetypeNotFoundException if mimetype sniffing failed
     * @throws MimetypeDetectionException if unexpected problem prevent the
     *             detection to work as expected
     */
    String getMimetypeFromFile(File file)
            throws MimetypeNotFoundException, MimetypeDetectionException;

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
     * Finds the mimetype of some content according to its filename and / or
     * binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     *
     *
     */
     String getMimetypeFromFilenameAndBlobWithDefault(String filename,
            Blob blob, String defaultMimetype) throws MimetypeDetectionException;
}
