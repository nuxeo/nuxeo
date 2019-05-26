/*
 * (C) Copyright 2009-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsManager;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNode;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNodeImpl;
import org.nuxeo.ecm.webapp.tree.TreeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("adminPublishActions")
@Scope(ScopeType.CONVERSATION)
public class AdministrationPublishActions extends AbstractPublishActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AdministrationPublishActions.class);

    public static final String PUBLICATION_TREE_PLUGIN_NAME = "publication";

    protected transient RootSectionFinder rootFinder;

    protected transient RootSectionsManager rootSectionsManager;

    protected transient TreeManager treeManager;

    protected DocumentTreeNode sectionsTree;

    protected DocumentModelList sectionRoots;

    protected String currentSectionRootId;

    @Create
    public void create() {
        rootSectionsManager = new RootSectionsManager(documentManager);
    }

    @Factory(value = "defaultPublishingRoots", scope = ScopeType.EVENT)
    public DocumentModelList getSectionRoots() {
        return getRootFinder().getDefaultSectionRoots(true, true);
    }

    protected RootSectionFinder getRootFinder() {
        if (rootFinder == null) {
            PublisherService ps = Framework.getService(PublisherService.class);
            rootFinder = ps.getRootSectionFinder(documentManager);
        }
        return rootFinder;
    }

    public String getCurrentSectionRootId() {
        return currentSectionRootId;
    }

    public List<DocumentTreeNode> getCurrentSectionsTree() {
        DocumentModel sectionsRoot = null;

        sectionRoots = getSectionRoots();
        if (currentSectionRootId == null && sectionRoots.size() > 0) {
            currentSectionRootId = sectionRoots.get(0).getId();
        }

        if (currentSectionRootId != null) {
            sectionsRoot = documentManager.getDocument(new IdRef(currentSectionRootId));
        }

        sectionsTree = getDocumentTreeNode(sectionsRoot);

        return Collections.singletonList(sectionsTree);
    }

    public void setCurrentSectionRootId(String currentSectionRootId) {
        this.currentSectionRootId = currentSectionRootId;
    }

    public String getDomainNameFor(final DocumentModel sectionRoot) {
        final List<String> domainName = new ArrayList<>();
        new UnrestrictedSessionRunner(documentManager) {
            @Override
            public void run() {
                DocumentModel parent = session.getParentDocument(sectionRoot.getRef());
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                while (parent != null && !"/".equals(parent.getPathAsString())) {
                    if (schemaManager.hasSuperType(parent.getType(), "Domain")) {
                        domainName.add(parent.getTitle());
                        return;
                    }
                }
            }
        }.runUnrestricted();
        return domainName.isEmpty() ? null : domainName.get(0);
    }

    protected DocumentTreeNode getDocumentTreeNode(DocumentModel documentModel) {
        DocumentTreeNode documentTreeNode = null;
        if (documentModel != null) {
            Filter filter = getTreeManager().getFilter(PUBLICATION_TREE_PLUGIN_NAME);
            Sorter sorter = getTreeManager().getSorter(PUBLICATION_TREE_PLUGIN_NAME);
            documentTreeNode = new DocumentTreeNodeImpl(documentModel, filter, sorter);
        }
        return documentTreeNode;
    }

    protected TreeManager getTreeManager() {
        if (treeManager == null) {
            treeManager = Framework.getService(TreeManager.class);
        }
        return treeManager;
    }

    public boolean canAddSection(DocumentModel section) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return rootSectionsManager.canAddSection(section, currentDocument);
    }

    public String addSection(String sectionId) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        rootSectionsManager.addSection(sectionId, currentDocument);
        getRootFinder().reset();
        return null;
    }

    public DocumentModelList getSelectedSections() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getRootFinder().getSectionRootsForWorkspace(currentDocument, true);
    }

    public String removeSection(String sectionId) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        rootSectionsManager.removeSection(sectionId, currentDocument);
        getRootFinder().reset();
        return null;
    }

    protected static String formatPathFragments(List<String> pathFragments) {
        String fullPath = "";
        for (String aFragment : pathFragments) {
            if (!"".equals(fullPath)) {
                fullPath = ">" + fullPath;
            }
            fullPath = aFragment + fullPath;
        }
        return fullPath;
    }

    @Override
    protected DocumentModel getParentDocument(DocumentModel documentModel) {
        return documentManager.getDocument(documentModel.getParentRef());
    }

}
