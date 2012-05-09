/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsFinderHelper;
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

    protected transient RootSectionsFinder rootFinder;

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
    public DocumentModelList getSectionRoots() throws ClientException {
        return getRootFinder().getDefaultSectionRoots(true, true);
    }

    protected RootSectionsFinder getRootFinder() {
        if (rootFinder == null) {
            rootFinder = RootSectionsFinderHelper.getRootSectionsFinder(documentManager);
        }
        return rootFinder;
    }

    public String getCurrentSectionRootId() {
        return currentSectionRootId;
    }

    public DocumentTreeNode getCurrentSectionsTree() throws ClientException {
        DocumentModel sectionsRoot = null;

        sectionRoots = getSectionRoots();
        if (currentSectionRootId == null && sectionRoots.size() > 0) {
            currentSectionRootId = sectionRoots.get(0).getId();
        }

        if (currentSectionRootId != null) {
            sectionsRoot = documentManager.getDocument(new IdRef(
                    currentSectionRootId));
        }

        sectionsTree = getDocumentTreeNode(sectionsRoot);

        return sectionsTree;
    }

    public void setCurrentSectionRootId(String currentSectionRootId) {
        this.currentSectionRootId = currentSectionRootId;
    }

    protected DocumentTreeNode getDocumentTreeNode(DocumentModel documentModel) {
        DocumentTreeNode documentTreeNode = null;
        if (documentModel != null) {
            Filter filter = null;
            Sorter sorter = null;
            try {
                filter = getTreeManager().getFilter(
                        PUBLICATION_TREE_PLUGIN_NAME);
                sorter = getTreeManager().getSorter(
                        PUBLICATION_TREE_PLUGIN_NAME);
            } catch (Exception e) {
                log.error(
                        "Could not fetch filter, sorter or node type for tree ",
                        e);
            }

            documentTreeNode = new DocumentTreeNodeImpl(documentModel, filter,
                    null, sorter, null);
        }

        return documentTreeNode;
    }

    protected TreeManager getTreeManager() {
        if (treeManager == null) {
            try {
                treeManager = Framework.getService(TreeManager.class);
            } catch (Exception e) {
                log.error("Could not fetch Tree Manager ", e);
            }
        }

        return treeManager;
    }

    public boolean canAddSection(DocumentModel section) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return rootSectionsManager.canAddSection(section, currentDocument);
    }

    public String addSection(String sectionId) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        rootSectionsManager.addSection(sectionId, currentDocument);
        getRootFinder().reset();
        return null;
    }

    public DocumentModelList getSelectedSections() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getRootFinder().getSectionRootsForWorkspace(currentDocument, true);
    }

    public String removeSection(String sectionId) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        rootSectionsManager.removeSection(sectionId, currentDocument);
        getRootFinder().reset();
        return null;
    }

    public String getFormattedPath(DocumentModel documentModel)
            throws ClientException {
        List<String> pathFragments = new ArrayList<String>();
        getPathFragments(documentModel, pathFragments);
        return formatPathFragments(pathFragments);
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

    protected void getPathFragments(DocumentModel documentModel,
            List<String> pathFragments) throws ClientException {
        String pathElementName = documentModel.getTitle();
        String translatedPathElement = resourcesAccessor.getMessages().get(
                pathElementName);
        pathFragments.add(translatedPathElement);
        if ("Domain".equals(documentModel.getType())) {
            return;
        }

        DocumentModel parentDocument;
        try {
            parentDocument = documentManager.getDocument(documentModel.getParentRef());
        } catch (Exception e) {
            log.error("Error building path", e);
            return;
        }
        getPathFragments(parentDocument, pathFragments);
    }

}
