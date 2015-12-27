/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.synchonize.server;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractSynchonizablePublicationTree implements ServerSynchronizablePublicationTree {

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

    public List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc) {
        // TODO Auto-generated method stub
        return null;
    }

    public PublicationNode getNodeByPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node) {
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
     * public List<PublicationNode> getTree() { // TODO Auto-generated method stub return null; }
     */

    public String getTreeType() {
        // TODO Auto-generated method stub
        return null;
    }

    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName) {
        // TODO Auto-generated method stub

    }

    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        // TODO Auto-generated method stub
        return null;
    }

    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        // TODO Auto-generated method stub
        return null;
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        // TODO Auto-generated method stub

    }

    public List<PublishedDocument> getChildrenDocuments() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PublicationNode> getChildrenNodes() {
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
