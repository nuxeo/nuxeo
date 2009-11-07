/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.SectionPublicationTree;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.ExtendedDocumentLocation;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublishedDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link PublicationTree} implementation that supports having plain Documents
 * directly inside the sections. This is typically used for Remote
 * {@link PublicationTree} implementation that will store proxies for locally
 * published documents and complete {@link DocumentModel} for remote published
 * Documents.
 *
 * @author tiry
 */
public class CoreTreeWithExternalDocs extends SectionPublicationTree implements
        PublicationTree {

    private static final long serialVersionUID = 1L;

    @Override
    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {

        List<PublishedDocument> publishedDocs = new ArrayList<PublishedDocument>();

        if (docLoc instanceof ExtendedDocumentLocation) {
            // remote proxy management
            ExtendedDocumentLocation xDocLoc = (ExtendedDocumentLocation) docLoc;

            String xsrv = xDocLoc.getOriginalServer();
            // String source = xDocLoc.getServerName() + "@" + srv + ":" +
            // xDocLoc.getDocRef().toString();
            String source = xDocLoc.toString();
            List<DocumentModel> foundDocs = findDocumentsCommingFromExternalRef(
                    treeRoot, source);

            for (DocumentModel doc : foundDocs) {
                // publishedDocs.add(new
                // ExternalCorePublishedDocument(doc));
                publishedDocs.add(factory.wrapDocumentModel(doc));
            }
        } else {
            // std proxy management
            if (getCoreSession().getRepositoryName().equals(
                    docLoc.getServerName())) {
                // same repo publishing
                publishedDocs.addAll(super.getExistingPublishedDocument(docLoc));
            }
        }
        return publishedDocs;
    }

    protected List<DocumentModel> findDocumentsCommingFromExternalRef(
            DocumentModel root, String extRef) throws ClientException {
        // XXX dummy impl : use Relations or Search to avoid this !!!!

        List<DocumentModel> docs = new ArrayList<DocumentModel>();

        for (DocumentModel child : getCoreSession().getChildren(root.getRef())) {
            if (child.isFolder()) {
                docs.addAll(findDocumentsCommingFromExternalRef(child, extRef));
            } else {
                if (extRef.equals(child.getProperty("dublincore", "source"))) {
                    docs.add(child);
                }
            }
        }
        return docs;

    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        ExternalCorePublishedDocument publishedDocument = (ExternalCorePublishedDocument) factory.publishDocument(
                doc, targetNode);
        return publishedDocument;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        ExternalCorePublishedDocument publishedDocument = (ExternalCorePublishedDocument) factory.publishDocument(
                doc, targetNode, params);
        return publishedDocument;
    }

    @Override
    public void unpublish(PublishedDocument publishedDocument)
            throws ClientException {
        if (publishedDocument instanceof BasicPublishedDocument
                || publishedDocument instanceof ExternalCorePublishedDocument) {
            String source = publishedDocument.getSourceRepositoryName() + "@"
                    + publishedDocument.getSourceServer() + ":"
                    + publishedDocument.getSourceDocumentRef();
            List<DocumentModel> foundDocs = findDocumentsCommingFromExternalRef(
                    treeRoot, source);
            for (DocumentModel doc : foundDocs) {
                if (doc.getPathAsString().equals(publishedDocument.getPath())) {
                    getCoreSession().removeDocument(doc.getRef());
                    getCoreSession().save();
                    break;
                }
            }
        } else {
            super.unpublish(publishedDocument);
        }
    }

    @Override
    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {
        PublicationNode realPublciationNode = getNodeByPath(targetNode.getPath());
        List<PublishedDocument> publishedDocuments = getPublishedDocumentInNode(realPublciationNode);
        String source = (String) doc.getProperty("dublincore", "source");
        for (PublishedDocument publishedDocument : publishedDocuments) {
            String publishedDocumentSource = publishedDocument.getSourceRepositoryName()
                    + "@"
                    + publishedDocument.getSourceServer()
                    + ":"
                    + publishedDocument.getSourceDocumentRef();
            if (source.equals(publishedDocumentSource)) {
                getCoreSession().removeDocument(
                        new PathRef(publishedDocument.getPath()));
                break;
            }
        }
    }

    @Override
    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) throws ClientException {
        return new ExternalCorePublishedDocument(documentModel);
    }

}
