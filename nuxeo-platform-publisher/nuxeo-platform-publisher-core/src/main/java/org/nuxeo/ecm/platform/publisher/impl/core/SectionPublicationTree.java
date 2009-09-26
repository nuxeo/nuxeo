package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of a {@link PublicationTree} using the Core Sections.
 *
 * @author tiry
 */
public class SectionPublicationTree extends AbstractBasePublicationTree
        implements PublicationTree {

    private static final long serialVersionUID = 1L;

    protected static final String CAN_ASK_FOR_PUBLISHING = "CanAskForPublishing";

    protected static final String DEFAULT_ROOT_PATH = "/default-domain/sections";

    protected DocumentModel treeRoot;

    @Override
    public void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title) throws ClientException {
        super.initTree(sid, coreSession, parameters, factory, configName, title);
        treeRoot = coreSession.getDocument(new PathRef(rootPath));
        rootNode = new CoreFolderPublicationNode(treeRoot, getConfigName(),
                sid, factory);
    }

    protected CoreSession getCoreSession() {
        return CoreInstance.getInstance().getSession(treeRoot.getSessionId());
    }

    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {
        List<PublishedDocument> publishedDocs = new ArrayList<PublishedDocument>();
        DocumentModelList proxies = getCoreSession().getProxies(
                docLoc.getDocRef(), null);
        for (DocumentModel proxy : proxies) {
            if (proxy.getPathAsString().startsWith(treeRoot.getPathAsString())) {
                publishedDocs.add(factory.wrapDocumentModel(proxy));
            }
        }
        return publishedDocs;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.publish(
                doc, targetNode);
        PublicationRelationHelper.addPublicationRelation(
                publishedDocument.getProxy(), this);
        return publishedDocument;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.publish(
                doc, targetNode, params);
        PublicationRelationHelper.addPublicationRelation(
                publishedDocument.getProxy(), this);
        return publishedDocument;
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {
        List<PublishedDocument> publishedDocs = getPublishedDocumentInNode(targetNode);
        for (PublishedDocument pubDoc : publishedDocs) {
            if (pubDoc.getSourceDocumentRef().equals(doc.getRef())) {
                unpublish(pubDoc);
            }
        }
    }

    public void unpublish(PublishedDocument publishedDocument)
            throws ClientException {
        if (!accept(publishedDocument)) {
            return;
        }
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        PublicationRelationHelper.removePublicationRelation(proxy);
        getCoreSession().removeDocument(proxy.getRef());
    }

    public PublicationNode getNodeByPath(String path) throws ClientException {
        return new CoreFolderPublicationNode(
                coreSession.getDocument(new PathRef(path)), getConfigName(),
                getSessionId(), factory);
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
    public boolean canPublishTo(PublicationNode publicationNode)
            throws ClientException {
        if (publicationNode == null || publicationNode.getParent() == null) {
            // we can't publish in the root node
            return false;
        }
        DocumentRef docRef = new PathRef(publicationNode.getPath());
        return coreSession.hasPermission(docRef, CAN_ASK_FOR_PUBLISHING);
    }

    @Override
    public boolean canUnpublish(PublishedDocument publishedDocument)
            throws ClientException {
        if (!accept(publishedDocument)) {
            return false;
        }
        DocumentRef docRef = new PathRef(publishedDocument.getParentPath());
        return coreSession.hasPermission(docRef, SecurityConstants.WRITE);
    }

    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel)
            throws ClientException {
        return factory.wrapDocumentModel(documentModel);
    }

    @Override
    public boolean isPublicationNode(DocumentModel documentModel)
            throws ClientException {
        return documentModel.getPathAsString().startsWith(rootPath);
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel)
            throws ClientException {
        if (!isPublicationNode(documentModel)) {
            throw new ClientException("Document "
                    + documentModel.getPathAsString()
                    + " is not a valid publication node.");
        }
        return new CoreFolderPublicationNode(documentModel, getConfigName(),
                sid, factory);
    }

    protected boolean accept(PublishedDocument publishedDocument) {
        return publishedDocument instanceof SimpleCorePublishedDocument;
    }

}
