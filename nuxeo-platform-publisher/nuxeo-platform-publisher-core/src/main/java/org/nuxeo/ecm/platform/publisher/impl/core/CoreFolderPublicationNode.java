package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.publisher.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Implementation of the {@link PublicationNode} for Simple Core Folders
 * 
 * @author tiry
 * 
 */
public class CoreFolderPublicationNode extends AbstractPublicationNode
        implements PublicationNode {

    private static final long serialVersionUID = 1L;

    protected DocumentModel folder;

    protected CoreFolderPublicationNode parent;

    protected String treeConfigName;

    protected PublishedDocumentFactory factory;

    protected String sid;

    public CoreFolderPublicationNode(DocumentModel doc, PublicationTree tree,
            PublishedDocumentFactory factory) throws ClientException {
        this.folder = doc;
        this.treeConfigName = tree.getConfigName();
        this.factory = factory;
        this.sid = tree.getSessionId();
    }

    public CoreFolderPublicationNode(DocumentModel doc, PublicationTree tree,
            CoreFolderPublicationNode parent, PublishedDocumentFactory factory)
            throws ClientException {
        this.folder = doc;
        this.treeConfigName = tree.getConfigName();
        this.parent = parent;
        this.factory = factory;
        this.sid = tree.getSessionId();
    }

    public CoreFolderPublicationNode(DocumentModel doc, String treeConfigName,
            String sid, CoreFolderPublicationNode parent,
            PublishedDocumentFactory factory) throws ClientException {
        this.folder = doc;
        this.treeConfigName = treeConfigName;
        this.parent = parent;
        this.factory = factory;
        this.sid = sid;
    }

    public CoreFolderPublicationNode(DocumentModel doc, String treeConfigName,
            String sid, PublishedDocumentFactory factory)
            throws ClientException {
        this.folder = doc;
        this.treeConfigName = treeConfigName;
        this.factory = factory;
        this.sid = sid;
    }

    protected CoreSession getCoreSession() {
        return CoreInstance.getInstance().getSession(folder.getSessionId());
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        DocumentModelList children = getCoreSession().getChildren(
                folder.getRef());

        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();

        for (DocumentModel child : children) {
            if (!child.hasFacet("Folderish")) {
                try {
                    childrenDocs.add(factory.wrapDocumentModel(child));
                } catch (Exception e) {
                    // Nothing to do for now
                }
            }
        }
        return childrenDocs;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        DocumentModelList children = getCoreSession().getChildren(
                folder.getRef());

        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();

        for (DocumentModel child : children) {
            if (child.hasFacet("Folderish")) {
                childrenNodes.add(new CoreFolderPublicationNode(child,
                        treeConfigName, sid, this, factory));
            }
        }
        return childrenNodes;
    }

    public String getTitle() throws ClientException {
        return folder.getTitle();
    }

    public String getName() throws ClientException {
        return folder.getName();
    }

    public PublicationNode getParent() {
        if (parent == null) {
            try {
                parent = new CoreFolderPublicationNode(
                        getCoreSession().getDocument(folder.getParentRef()),
                        treeConfigName, sid, factory);
            } catch (Exception e) {
                // XXX
            }
        }
        return parent;
    }

    public String getPath() {
        return folder.getPathAsString();
    }

    public String getTreeConfigName() {
        return treeConfigName;
    }

    public DocumentRef getTargetDocumentRef() {
        return folder.getRef();
    }

    public DocumentModel getTargetDocumentModel() {
        return folder;
    }

    public String getSessionId() {
        return sid;
    }

}
