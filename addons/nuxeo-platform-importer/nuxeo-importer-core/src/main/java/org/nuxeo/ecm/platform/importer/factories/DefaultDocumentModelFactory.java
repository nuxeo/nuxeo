/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere (Nuxeo)
 */
package org.nuxeo.ecm.platform.importer.factories;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Default implementation for DocumentModel factory The default empty constructor create Folder for folderish file and
 * File for other. But you can specify them using the other constructor. Also, if you are using .properties files to
 * setup metada, you can use the ecm:primaryType xpath to specify the type of document to create. This will override the
 * default ones, and works for files and folders. If no .properties file is provided of it the current node has a
 * .properties file but no ecm:primaryType, the default types are created. This works for leafType but also for
 * folderish type.
 *
 * @author Thierry Delprat
 * @author Daniel Tellez
 * @author Thibaud Arguillere
 */
public class DefaultDocumentModelFactory extends AbstractDocumentModelFactory {

    public static final String DOCTYPE_KEY_NAME = "ecm:primaryType";

    public static final String FACETS_KEY_NAME = "ecm:mixinTypes";

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
     *
     * @param folderishType the folderish type
     * @param leafType the other type
     */
    public DefaultDocumentModelFactory(String folderishType, String leafType) {
        this.folderishType = folderishType;
        this.leafType = leafType;
    }

    /*
     * (non-Javadoc)
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createFolderishNode(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    @Override
    public DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        String name = getValidNameFromFileName(node.getName());

        BlobHolder bh = node.getBlobHolder();
        String folderishTypeToUse = getDocTypeToUse(bh);
        if (folderishTypeToUse == null) {
            folderishTypeToUse = folderishType;
        }
        List<String> facets = getFacetsToUse(bh);

        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, folderishTypeToUse);
        for (String facet : facets) {
            doc.addFacet(facet);
        }
        doc.setProperty("dublincore", "title", node.getName());
        doc = session.createDocument(doc);
        if (bh != null) {
            doc = setDocumentProperties(session, bh.getProperties(), doc);
        }

        return doc;
    }

    /*
     * (non-Javadoc)
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createLeafNode(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    @Override
    public DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException {
        return defaultCreateLeafNode(session, parent, node);
    }

    protected DocumentModel defaultCreateLeafNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        Blob blob = null;
        Map<String, Serializable> props = null;
        String leafTypeToUse = leafType;
        BlobHolder bh = node.getBlobHolder();
        if (bh != null) {
            blob = bh.getBlob();
            props = bh.getProperties();
            String bhType = getDocTypeToUse(bh);
            if (bhType != null) {
                leafTypeToUse = bhType;
            }
        }
        String fileName = node.getName();
        String name = getValidNameFromFileName(fileName);
        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, leafTypeToUse);
        for (String facet : getFacetsToUse(bh)) {
            doc.addFacet(facet);
        }
        doc.setProperty("dublincore", "title", node.getName());
        if (blob != null && blob.getLength() > 0) {
            blob.setFilename(fileName);
            doc.setProperty("file", "content", blob);
        }
        doc = session.createDocument(doc);
        if (props != null) {
            doc = setDocumentProperties(session, props, doc);
        }
        return doc;
    }

    /*
     * Return null if DOCTYPE_KEY_NAME is not in the properties or has been set to nothing.
     */
    protected String getDocTypeToUse(BlobHolder inBH) {
        String type = null;

        if (inBH != null) {
            Map<String, Serializable> props = inBH.getProperties();
            if (props != null) {
                type = (String) props.get(DOCTYPE_KEY_NAME);
                if (type != null && type.isEmpty()) {
                    type = null;
                }
            }
        }

        return type;
    }

    protected List<String> getFacetsToUse(BlobHolder inBH) {
        if (inBH != null) {
            Map<String, Serializable> props = inBH.getProperties();
            if (props != null) {
                Serializable ob = props.get(FACETS_KEY_NAME);
                if (ob instanceof String) {
                    String facet = (String) ob;
                    if (StringUtils.isNotBlank(facet)) {
                        return Collections.singletonList(facet);
                    }
                } else if (ob != null) {
                    return (List<String>) ob;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Modify this to get right mime types depending on the file input
     *
     * @deprecated since 10.1 seems unused
     */
    @Deprecated
    protected String getMimeType(String name) {
        // Dummy MimeType detection : plug nuxeo Real MimeType service to
        // have better results

        if (name == null) {
            return "application/octet-stream";
            /* OpenOffice.org 2.x document types */
        } else if (name.endsWith(".odp")) {
            return "application/vnd.oasis.opendocument.presentation";
        } else if (name.endsWith(".otp")) {
            return "application/vnd.oasis.opendocument.presentation-template";
        } else if (name.endsWith(".otg")) {
            return "application/vnd.oasis.opendocument.graphics-template";
        } else if (name.endsWith(".odg")) {
            return "application/vnd.oasis.opendocument.graphics";
        } else if (name.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        } else if (name.endsWith(".ott")) {
            return "application/vnd.oasis.opendocument.text-template";
        } else if (name.endsWith(".ods")) {
            return "application/vnd.oasis.opendocument.spreadsheet";
        } else if (name.endsWith(".ots")) {
            return "application/vnd.oasis.opendocument.spreadsheet-template";
            /* Microsoft Office document */
        } else if (name.endsWith(".doc")) {
            return "application/msword";
        } else if (name.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (name.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
            /* Ms Office 2007 */
        } else if (name.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (name.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.template";
            /* Other */
        } else if (name.endsWith(".tar")) {
            return "application/x-gtar";
        } else if (name.endsWith(".gz")) {
            return "application/x-gtar";
        } else if (name.endsWith(".csv")) {
            return "text/csv";
        } else if (name.endsWith(".pdf")) {
            return "application/pdf";
        } else if (name.endsWith(".txt")) {
            return "text/plain";
        } else if (name.endsWith(".html")) {
            return "text/html";
        } else if (name.endsWith(".xml")) {
            return "text/xml";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".jpg")) {
            return "image/jpg";
        } else if (name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    public void setFolderishType(String folderishType) {
        this.folderishType = folderishType;
    }

    public void setLeafType(String leafType) {
        this.leafType = leafType;
    }

}
