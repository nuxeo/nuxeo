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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.tree.DefaultDocumentTreeSorter;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

/**
 * Implementation of the {@link PublicationNode} for Simple Core Folders.
 *
 * @author tiry
 */
public class CoreFolderPublicationNode extends AbstractPublicationNode {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_SORT_PROP_NAME = "dc:title";

    protected DocumentModel folder;

    protected PublicationNode parent;

    protected List<PublicationNode> childrenNodes;

    protected PublishedDocumentFactory factory;

    public CoreFolderPublicationNode(DocumentModel doc, PublicationTree tree, PublicationNode parent,
            PublishedDocumentFactory factory) {
        super(tree);
        this.folder = doc;
        this.parent = parent;
        this.factory = factory;
    }

    public CoreFolderPublicationNode(DocumentModel doc, PublicationTree tree, PublishedDocumentFactory factory) {
        super(tree);
        this.folder = doc;
        this.factory = factory;
    }

    protected CoreSession getCoreSession() {
        return folder.getCoreSession();
    }

    protected String buildChildrenWhereClause(boolean queryDocuments) {
        String clause = String.format("ecm:parentId = '%s' AND ecm:isTrashed = 0", folder.getId());
        if (queryDocuments) {
            clause += String.format(" AND ecm:mixinType NOT IN ('%s', '%s')", FacetNames.PUBLISH_SPACE,
                    FacetNames.HIDDEN_IN_NAVIGATION);
        } else {
            clause += String.format("AND ecm:mixinType IN ('%s') AND ecm:mixinType NOT IN ('%s')",
                    FacetNames.PUBLISH_SPACE, FacetNames.HIDDEN_IN_NAVIGATION);
        }
        return clause;
    }

    @Override
    public List<PublishedDocument> getChildrenDocuments() {
        DocumentModelList children = getSortedChildren(true);
        List<PublishedDocument> childrenDocs = new ArrayList<>();
        for (DocumentModel child : children) {
            childrenDocs.add(factory.wrapDocumentModel(child));
        }
        return childrenDocs;
    }

    @Override
    public List<PublicationNode> getChildrenNodes() {
        if (childrenNodes == null) {
            DocumentModelList children = getSortedChildren(false);
            childrenNodes = new ArrayList<>();
            for (DocumentModel child : children) {
                childrenNodes.add(new CoreFolderPublicationNode(child, tree, this, factory));
            }
        }
        return childrenNodes;
    }

    protected DocumentModelList getOrderedChildren() {
        return getCoreSession().getChildren(folder.getRef(), null, null, computeGetChildrenFilter(), null);
    }

    protected Filter computeGetChildrenFilter() {
        FacetFilter facetFilter = new FacetFilter(Arrays.asList(FacetNames.FOLDERISH),
                Arrays.asList(FacetNames.HIDDEN_IN_NAVIGATION));
        Filter trashedFilter = docModel -> !docModel.isTrashed();
        return new CompoundFilter(facetFilter, trashedFilter);
    }

    protected DocumentModelList getSortedChildren(boolean queryDocuments) {
        String whereClause = buildChildrenWhereClause(queryDocuments);
        DocumentModelList children = getCoreSession().query("SELECT * FROM Document WHERE " + whereClause);
        if (!folder.hasFacet(FacetNames.ORDERABLE)) {
            DefaultDocumentTreeSorter sorter = new DefaultDocumentTreeSorter();
            sorter.setSortPropertyPath(DEFAULT_SORT_PROP_NAME);
            Collections.sort(children, sorter);
        }
        return children;
    }

    @Override
    public String getTitle() {
        return folder.getTitle();
    }

    @Override
    public String getName() {
        return folder.getName();
    }

    @Override
    public PublicationNode getParent() {
        if (parent == null) {
            DocumentRef docRef = folder.getParentRef();
            if (getCoreSession().hasPermission(docRef, SecurityConstants.READ)) {
                parent = new CoreFolderPublicationNode(getCoreSession().getDocument(folder.getParentRef()), tree,
                        factory);
            } else {
                parent = new VirtualCoreFolderPublicationNode(getCoreSession(), docRef.toString(), tree,
                        factory);
            }
        }
        return parent;
    }

    @Override
    public String getPath() {
        return folder.getPathAsString();
    }

    public DocumentRef getTargetDocumentRef() {
        return folder.getRef();
    }

    public DocumentModel getTargetDocumentModel() {
        return folder;
    }

}
