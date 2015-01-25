/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.HTML
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Inits the repository for a content diff test case.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class ContentDiffRepositoryInit extends DefaultRepositoryInit {

    public static String getLeftPlainTextDocPath() {
        return "/leftPlainTextDoc";
    }

    public static String getRightPlainTextDocPath() {
        return "/rightPlainTextDoc";
    }

    public static String getLeftHTMLDocPath() {
        return "/leftHTMLDoc";
    }

    public static String getRightHTMLDocPath() {
        return "/rightHTMLDoc";
    }

    public static String getLeftOfficeDocPath() {
        return "/leftOfficeDoc";
    }

    public static String getRightOfficeDocPath() {
        return "/rightOfficeDoc";
    }

    public static String getLeftImageDocPath() {
        return "/leftImageDoc";
    }

    public static String getRightImageDocPath() {
        return "/rightImageDoc";
    }

    @Override
    public void populate(CoreSession session) throws ClientException {

        createFileDoc(session, "leftPlainTextDoc", "Left plain text doc", "left_doc.txt", "text/plain");
        createFileDoc(session, "rightPlainTextDoc", "Right plain text doc", "right_doc.txt", "text/plain");
        createFileDoc(session, "leftHTMLDoc", "Left HTML doc", "left_doc.html", "text/html");
        createFileDoc(session, "rightHTMLDoc", "Right HTML doc", "right_doc.html", "text/html");
        createFileDoc(session, "leftOfficeDoc", "Left Office doc", "left_doc.odt",
                "application/vnd.oasis.opendocument.text");
        createFileDoc(session, "rightOfficeDoc", "Right Office doc", "right_doc.odt",
                "application/vnd.oasis.opendocument.text");
        createFileDoc(session, "leftImageDoc", "Left image doc", "left_doc.png", "image/png");
        createFileDoc(session, "rightImageDoc", "Right image doc", "right_doc.png", "image/png");
    }

    /**
     * Creates a File document given the specified id, title, file path and mime type.
     *
     * @param session the core session
     * @param id the document id
     * @param title the document title
     * @param filePath the document file path
     * @param mimeType the document file mime type
     * @return the document model
     * @throws ClientException if an error occurs while creating the document
     */
    protected DocumentModel createFileDoc(CoreSession session, String id, String title, String filePath, String mimeType)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", id, "File");

        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("file:content", (Serializable) getBlobFromPath(filePath, mimeType));

        return session.createDocument(doc);
    }

    protected Blob getBlobFromPath(String path, String mimeType) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        try {
            return Blobs.createBlob(file, mimeType, null, path);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }
}
