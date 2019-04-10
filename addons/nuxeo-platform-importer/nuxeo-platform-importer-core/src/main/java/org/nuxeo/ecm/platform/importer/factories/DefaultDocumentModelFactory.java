/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.factories;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 *
 * Default implementation for DocumentModel factory
 * The default empty constructor create Folder for folderish file and
 * File for other. But you can specify them using the other constructor.
 *
 * @author Thierry Delprat
 *
 */
public class DefaultDocumentModelFactory extends AbstractDocumentModelFactory {

    protected String folderishType;

    protected String leafType;

    /**
     * Instantiate a DefaultDocumentModelFactory that creates Folder and File
     */
    public DefaultDocumentModelFactory() {
        this("Folder", "File");
    }

    /**
     * Instantiate a DefaultDocumentModelFactory that creates specified types doc
     * @param folderishType the folderish type
     * @param leafType the other type
     */
    public DefaultDocumentModelFactory(String folderishType, String leafType) {
        this.folderishType = folderishType;
        this.leafType = leafType;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createFolderishNode(org.nuxeo.ecm.core.api.CoreSession,
     * org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    public DocumentModel createFolderishNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception {
        String name = getValidNameFromFileName(node.getName());

        Map<String, Object> options = new HashMap<String, Object>();
        DocumentModel doc = session.createDocumentModel(folderishType, options);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", node.getName());
        doc = session.createDocument(doc);
        return doc;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createLeafNode(org.nuxeo.ecm.core.api.CoreSession,
     * org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    public DocumentModel createLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception {
        return defaultCreateLeafNode(session, parent, node);
    }

    protected DocumentModel defaultCreateLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node)
            throws Exception {

        BlobHolder bh = node.getBlobHolder();

        String mimeType = bh.getBlob().getMimeType();
        if (mimeType == null) {
            mimeType = getMimeType(node.getName());
        }

        String name = getValidNameFromFileName(node.getName());
        String fileName = node.getName();

        Map<String, Object> options = new HashMap<String, Object>();
        DocumentModel doc = session.createDocumentModel(leafType, options);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", node.getName());
        doc.setProperty("file", "filename", fileName);
        doc.setProperty("file", "content", bh.getBlob());

        doc = session.createDocument(doc);
        doc = setDocumentProperties(session, bh.getProperties(), doc);
        return doc;
    }

    /** Modify this to get right mime types depending on the file input */
    protected String getMimeType(String name) {
        // Dummy MimeType detection : plug nuxeo Real MimeType service to
        // have better results

        if (name == null) {
            return "application/octet-stream";
        } else if (name.endsWith(".doc")) {
            return "application/msword";
        } else if (name.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (name.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (name.endsWith(".txt")) {
            return "text/plain";
        } else if (name.endsWith(".html")) {
            return "text/html";
        } else if (name.endsWith(".xml")) {
            return "text/xml";
        } else if (name.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        } else if (name.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

}
