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
 */
package org.nuxeo.ecm.platform.filemanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

public final class FileManagerUtils {

    // This is an utility class
    private FileManagerUtils() {
    }

    /**
     * Returns the contents of the file in a byte array.
     *
     * @deprecated since 7.2, use {@link IOUtils#toByteArray} instead
     */
    @Deprecated
    public static byte[] getBytesFromFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return IOUtils.toByteArray(in);
        }
    }

    /**
     * Returns the fileName of a file.
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
    // FIXME: doesn't work in some corner cases, for instance a Unix filename
    // with a \, or a DOS file with a /
    public static String fetchFileName(String fullName) {
        // Fetching filename
        // first normalize input, as unicode can be decomposed (macOS behavior on WebDAV)
        String ret = Normalizer.normalize(fullName, Form.NFC);
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
    public static DocumentModel getExistingDocByFileName(CoreSession documentManager, String path, String filename) {
        // We must use the "file:content/name" sub-property which is the only
        // one on which we can rely
        // Note that the "file:content" property is handled in a particular way
        // by NXQL, so we must use "content/name" instead of
        // "file:content/name".
        return getExistingDocByPropertyName(documentManager, path, filename, "content/name");
    }

    /**
     * Looks if an existing Document with the same title exists.
     */
    public static DocumentModel getExistingDocByTitle(CoreSession documentManager, String path, String title) {
        return getExistingDocByPropertyName(documentManager, path, title, "dc:title");
    }

    /**
     * Looks if an existing Document has the same value for a given property.
     */
    public static DocumentModel getExistingDocByPropertyName(CoreSession documentManager, String path, String value,
            String propertyName) {
        value = Normalizer.normalize(value, Normalizer.Form.NFC);
        DocumentModel existing = null;
        String parentId = documentManager.getDocument(new PathRef(path)).getId();
        String query = "SELECT * FROM Document WHERE ecm:parentId = '" + parentId + "' AND " + propertyName + " = '"
                + value.replace("'", "\\\'") + "' AND ecm:isTrashed = 0";
        DocumentModelList docs = documentManager.query(query, 1);
        if (docs.size() > 0) {
            existing = docs.get(0);
        }
        return existing;
    }

}
