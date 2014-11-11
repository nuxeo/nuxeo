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

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.tree.DefaultDocumentTreeSorter;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link PublicationNode} for Simple Core Folders.
 *
 * @author tiry
 */
public class CoreFolderPublicationNode extends AbstractPublicationNode
        implements PublicationNode {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreFolderPublicationNode.class);

    private static final String DEFAULT_SORT_PROP_NAME = "dc:title";

    protected DocumentModel folder;

    protected PublicationNode parent;

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
            PublicationNode parent, PublishedDocumentFactory factory)
            throws ClientException {
        this.folder = doc;
        this.treeConfigName = tree.getConfigName();
        this.parent = parent;
        this.factory = factory;
        this.sid = tree.getSessionId();
    }

    public CoreFolderPublicationNode(DocumentModel doc, String treeConfigName,
            String sid, PublicationNode parent, PublishedDocumentFactory factory)
            throws ClientException {
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
        DefaultDocumentTreeSorter sorter = new DefaultDocumentTreeSorter();
        sorter.setSortPropertyPath(DEFAULT_SORT_PROP_NAME);
        FacetFilter facetFilter = new FacetFilter(null, Arrays.asList(
                FacetNames.FOLDERISH, FacetNames.HIDDEN_IN_NAVIGATION));
        LifeCycleFilter lfFilter = new LifeCycleFilter(
                LifeCycleConstants.DELETED_STATE, false);
        DocumentModelList children = getCoreSession().getChildren(
                folder.getRef(), null, null,
                new CompoundFilter(facetFilter, lfFilter), sorter);

        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();
        for (DocumentModel child : children) {
            try {
                childrenDocs.add(factory.wrapDocumentModel(child));
            } catch (Exception e) {
                // Nothing to do for now
                log.error(e);
            }
        }
        return childrenDocs;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        DefaultDocumentTreeSorter sorter = new DefaultDocumentTreeSorter();
        sorter.setSortPropertyPath(DEFAULT_SORT_PROP_NAME);
        FacetFilter facetFilter = new FacetFilter(
                Arrays.asList(FacetNames.FOLDERISH),
                Arrays.asList(FacetNames.HIDDEN_IN_NAVIGATION));
        LifeCycleFilter lfFilter = new LifeCycleFilter(
                LifeCycleConstants.DELETED_STATE, false);
        DocumentModelList children = getCoreSession().getChildren(
                folder.getRef(), null, null,
                new CompoundFilter(facetFilter, lfFilter), sorter);

        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();

        for (DocumentModel child : children) {
            childrenNodes.add(new CoreFolderPublicationNode(child,
                    treeConfigName, sid, this, factory));
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
            DocumentRef docRef = folder.getParentRef();
            try {
                if (getCoreSession().hasPermission(docRef,
                        SecurityConstants.READ)) {
                    parent = new CoreFolderPublicationNode(
                            getCoreSession().getDocument(folder.getParentRef()),
                            treeConfigName, sid, factory);
                } else {
                    parent = new VirtualCoreFolderPublicationNode(
                            getCoreSession().getSessionId(), docRef.toString(),
                            treeConfigName, sid, factory);
                }
            } catch (Exception e) {
                log.error("Error while retrieving parent: ", e);
            }
        }
        return parent;
    }

    public String getPath() {
        return folder.getPathAsString();
    }

    @Override
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
