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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;

public final class FileManagerUtils {

    // This is an utility class
    private FileManagerUtils() {
    }

    /**
     * Returns the contents of the file in a byte array.
     *
     * @param file
     * @return the byte array
     * @throws IOException
     */
    public static byte[] getBytesFromFile(File file) throws IOException {

        FileInputStream is = new FileInputStream(file);

        long length = file.length();
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];
        try {
            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read InputStream");
            }
        } finally {
            // Close the input stream and return bytes
            is.close();
        }
        return bytes;
    }

    /**
     * Returns the fileName of a file.
     *
     * @param file
     * @return the FileName String
     */
    public static String fetchFileName(File file) throws MalformedURLException {
        // Fetching filename
        URL pathUrl = file.toURI().toURL();
        String[] pathArray = pathUrl.getFile().split("/");
        return pathArray[pathArray.length - 1];
    }

    /**
     * Returns the fileName of an uploaded file.
     *
     * @param fullName the full name that we need to parse
     * @return the FileName String
     */
    // FIXME: badly named method
    // FIXME: doesn't work in some corner cases, for instance a Unix filename with a \, or a DOS file with a /
    public static String fetchFileName(String fullName) {
        // Fetching filename
        String ret = fullName;
        int lastWinSeparator = fullName.lastIndexOf('\\');
        int lastUnixSeparator = fullName.lastIndexOf('/');
        int lastSeparator = Math.max(lastWinSeparator, lastUnixSeparator);
        if (lastSeparator != -1) {
            ret = fullName.substring(lastSeparator + 1, fullName.length());
        }
        return ret;
    }

    /**
     * Returns the title.
     *
     * @param filename the file name
     * @return the title
     */
    public static String fetchTitle(String filename) {
        String title = filename.trim();
        if (title.length() == 0) {
            title = IdUtils.generateStringId();
        }
        return title;
    }

    /**
     * Looks if an existing Document with the same filename exists.
     */
    public static DocumentModel getExistingDocByFileName(
            CoreSession documentManager, String path, String filename)
            throws ClientException {
        DocumentModel existing = null;
        DocumentRef pathRef = new PathRef(path);
        DocumentModelList docList = documentManager.getChildren(pathRef);
        for (DocumentModel doc : docList) {
            String currentLifeCycleState = doc.getCurrentLifeCycleState();
            // CB: NXP-2483 - This check should be done only for the documents which aren't deleted
            if (!LifeCycleConstants.DELETED_STATE.equals(currentLifeCycleState)) {
                String existFileName = (String) doc.getProperty("file", "filename");
                if (existFileName != null && existFileName.equals(filename)) {
                    existing = doc;
                    break;
                }
            }
        }
        return existing;
    }

    /**
     * Looks if an existing Document with the same title exists.
     */
    public static DocumentModel getExistingDocByTitle(
            CoreSession documentManager, String path, String title)
            throws ClientException {
        DocumentModel existing = null;
        DocumentRef pathRef = new PathRef(path);
        DocumentModelList docList = documentManager.getChildren(pathRef);
        for (DocumentModel doc : docList) {
            String currentLifeCycleState = doc.getCurrentLifeCycleState();
            // CB: NXP-2483 - This check should be done only for the documents which aren't deleted
            if (!LifeCycleConstants.DELETED_STATE.equals(currentLifeCycleState)) {
                String existTitle = (String) doc.getProperty("dublincore", "title");
                if (existTitle != null && existTitle.equals(title)) {
                    existing = doc;
                    break;
                }
            }
        }
        return existing;
    }

}
