/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.runtime.api.Framework;

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

    public static String getLeftJSONDocPath() {
        return "/leftJSONDoc";
    }

    public static String getRightJSONDocPath() {
        return "/rightJSONDoc";
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
    public void populate(CoreSession session) {

        createFileDoc(session, "leftPlainTextDoc", "Left plain text doc", "left_doc.txt", "text/plain");
        createFileDoc(session, "rightPlainTextDoc", "Right plain text doc", "right_doc.txt", "text/plain");
        createFileDoc(session, "leftHTMLDoc", "Left HTML doc", "left_doc.html", "text/html");
        createFileDoc(session, "rightHTMLDoc", "Right HTML doc", "right_doc.html", "text/html");
        createFileDoc(session, "leftJSONDoc", "Left JSON doc", "left_doc.json", "application/json");
        createFileDoc(session, "rightJSONDoc", "Right JSON doc", "right_doc.json", "application/json");
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
     */
    protected DocumentModel createFileDoc(CoreSession session, String id, String title, String filePath, String mimeType)
            {

        DocumentModel doc = session.createDocumentModel("/", id, "File");

        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("file:content", (Serializable) getBlobFromPath(filePath, mimeType));

        return session.createDocument(doc);
    }

    protected Blob getBlobFromPath(String path, String mimeType) {
        try (InputStream in = Framework.getResourceLoader().getResourceAsStream(path)) {
            assertNotNull(path, in);
            Blob blob = Blobs.createBlob(in, mimeType);
            blob.setFilename(path);
            return blob;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }
}
