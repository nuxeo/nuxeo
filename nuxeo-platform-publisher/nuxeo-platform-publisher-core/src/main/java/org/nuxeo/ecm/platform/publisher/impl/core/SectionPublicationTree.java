package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.publisher.api.*;

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

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected static final String DEFAULT_ROOT_PATH = "/default-domain/sections";

    protected DocumentModel treeRoot;

    @Override
    public void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName) throws ClientException {
        super.initTree(sid, coreSession, parameters, factory, configName);
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

        List<DocumentModel> possibleDocsToCheck = new ArrayList<DocumentModel>();

        DocumentModel livedoc = getCoreSession().getDocument(docLoc.getDocRef());

        if (!livedoc.isVersion()) {
            possibleDocsToCheck = getCoreSession().getVersions(
                    docLoc.getDocRef());
        } else {
            possibleDocsToCheck.add(livedoc);
        }

        for (DocumentModel doc : possibleDocsToCheck) {
            DocumentModelList proxies = getCoreSession().getProxies(
                    doc.getRef(), null);
            for (DocumentModel proxy : proxies) {
                if (proxy.getPathAsString().startsWith(
                        treeRoot.getPathAsString())) {
                    publishedDocs.add(factory.wrapDocumentModel(proxy));
                }
            }
        }
        return publishedDocs;
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
        getCoreSession().removeDocument(
                ((SimpleCorePublishedDocument) publishedDocument).getProxy().getRef());
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

}
