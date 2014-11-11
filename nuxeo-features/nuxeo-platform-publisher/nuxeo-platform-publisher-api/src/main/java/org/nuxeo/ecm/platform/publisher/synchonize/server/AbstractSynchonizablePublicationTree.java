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

package org.nuxeo.ecm.platform.publisher.synchonize.server;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractSynchonizablePublicationTree implements
        ServerSynchronizablePublicationTree {

    public String exportPublishedDocumentByPath(String path) {
        // TODO Auto-generated method stub
        // PublicationNode node = this.getNodeByPath(path);

        return null;
    }

    public List<PublicationNode> listModifiedNodes(long timeDelta) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublishedDocument> listModifiedPublishedDocuments(long timeDelta) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getConfigName() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public PublicationNode getNodeByPath(String path) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublishedDocument> getPublishedDocumentInNode(
            PublicationNode node) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * public List<PublicationNode> getTree() throws ClientException { // TODO
     * Auto-generated method stub return null; }
     */

    public String getTreeType() {
        // TODO Auto-generated method stub
        return null;
    }

    public void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName) throws ClientException {
        // TODO Auto-generated method stub

    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {
        // TODO Auto-generated method stub

    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNodeType() {
        // TODO Auto-generated method stub
        return null;
    }

    public PublicationNode getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTreeName() {
        // TODO Auto-generated method stub
        return null;
    }

}
