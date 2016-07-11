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

package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple implementation of a {@link PublicationTree} using the Core Sections.
 *
 * @author tiry
 */
public class SectionPublicationTree extends AbstractBasePublicationTree implements PublicationTree {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SectionPublicationTree.class);

    public static final String CAN_ASK_FOR_PUBLISHING = "CanAskForPublishing";

    protected static final String DEFAULT_ROOT_PATH = "/default-domain/sections";

    protected DocumentModel treeRoot;

    protected String sessionId;

    @Override
    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName, String title) {
        super.initTree(sid, coreSession, parameters, factory, configName, title);

        DocumentRef ref = new PathRef(rootPath);
        boolean exists = coreSession.exists(ref);
        if (!exists) {
            log.debug("Root section " + rootPath + " doesn't exist. Check " + "publicationTreeConfig with name "
                    + configName);
        }
        if (exists && coreSession.hasPermission(ref, SecurityConstants.READ)) {
            treeRoot = coreSession.getDocument(new PathRef(rootPath));
            rootNode = new CoreFolderPublicationNode(treeRoot, getConfigName(), sid, factory);
        } else {
            rootNode = new VirtualCoreFolderPublicationNode(coreSession.getSessionId(), rootPath, getConfigName(), sid,
                    factory);
            sessionId = coreSession.getSessionId();
        }
    }

    protected CoreSession getCoreSession() {
        String coreSessionId = treeRoot == null ? sessionId : treeRoot.getSessionId();
        return Framework.getService(CoreSessionService.class).getCoreSession(coreSessionId);
    }

    public List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc) {
        List<PublishedDocument> publishedDocs = new ArrayList<PublishedDocument>();
        DocumentModelList proxies = getCoreSession().getProxies(docLoc.getDocRef(), null);
        for (DocumentModel proxy : proxies) {
            if (proxy.getPathAsString().startsWith(rootPath)) {
                publishedDocs.add(factory.wrapDocumentModel(proxy));
            }
        }
        return publishedDocs;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.publish(doc, targetNode);
        PublicationRelationHelper.addPublicationRelation(publishedDocument.getProxy(), this);
        return publishedDocument;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.publish(doc, targetNode,
                params);
        PublicationRelationHelper.addPublicationRelation(publishedDocument.getProxy(), this);
        return publishedDocument;
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        List<PublishedDocument> publishedDocs = getPublishedDocumentInNode(targetNode);
        for (PublishedDocument pubDoc : publishedDocs) {
            if (pubDoc.getSourceDocumentRef().equals(doc.getRef())) {
                unpublish(pubDoc);
            }
        }
    }

    public void unpublish(PublishedDocument publishedDocument) {
        if (!accept(publishedDocument)) {
            return;
        }
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        PublicationRelationHelper.removePublicationRelation(proxy);
        getCoreSession().removeDocument(proxy.getRef());
        getCoreSession().save();
    }

    public PublicationNode getNodeByPath(String path) {
        DocumentRef docRef = new PathRef(path);
        if (coreSession.hasPermission(docRef, SecurityConstants.READ)) {
            return new CoreFolderPublicationNode(coreSession.getDocument(new PathRef(path)), getConfigName(),
                    getSessionId(), factory);
        } else {
            return new VirtualCoreFolderPublicationNode(coreSession.getSessionId(), path, getConfigName(), sid, factory);
        }

    }

    public void release() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String getDefaultRootPath() {
        return DEFAULT_ROOT_PATH;
    }

    @Override
    protected PublishedDocumentFactory getDefaultFactory() {
        return new CoreProxyFactory();
    }

    @Override
    public boolean canPublishTo(PublicationNode publicationNode) {
        if (publicationNode == null || publicationNode.getParent() == null) {
            // we can't publish in the root node
            return false;
        }
        DocumentRef docRef = new PathRef(publicationNode.getPath());
        return coreSession.hasPermission(docRef, CAN_ASK_FOR_PUBLISHING);
    }

    @Override
    public boolean canUnpublish(PublishedDocument publishedDocument) {
        if (!accept(publishedDocument)) {
            return false;
        }
        DocumentRef docRef = new PathRef(publishedDocument.getParentPath());
        return coreSession.hasPermission(docRef, SecurityConstants.WRITE);
    }

    @Override
    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) {
        return factory.wrapDocumentModel(documentModel);
    }

    @Override
    public boolean isPublicationNode(DocumentModel documentModel) {
        return documentModel.getPathAsString().startsWith(rootPath);
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel) {
        if (!isPublicationNode(documentModel)) {
            throw new NuxeoException("Document " + documentModel.getPathAsString()
                    + " is not a valid publication node.");
        }
        return new CoreFolderPublicationNode(documentModel, getConfigName(), sid, factory);
    }

    @Override
    protected boolean accept(PublishedDocument publishedDocument) {
        return publishedDocument instanceof SimpleCorePublishedDocument;
    }

}
