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
 *     alexandre
 */
package org.nuxeo.ecm.platform.publishing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeComparator;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author alexandre
 *
 */
public class SelectionModelGetter extends UnrestrictedSessionRunner {
    private final List<DocumentModelTreeNode> treeNodes = new ArrayList<DocumentModelTreeNode>();

    protected final CoreSession coreSession;

    protected final DocumentRef currentDocRef;

    protected final DocumentRef currentParentRef;

    protected final DocumentModelTree sections;

    protected final QueryModel queryModel;

    private SelectDataModel model;

    protected final VersioningManager versioningManager;

    public SelectionModelGetter(CoreSession coreSession, DocumentModel doc,
            Set<String> sectionRootTypes, Set<String> sectionTypes, QueryModel queryModel)
            throws ClientException {
        super(coreSession);
        this.currentDocRef = doc.getRef();
        this.currentParentRef = doc.getParentRef();
        this.coreSession = coreSession;
        this.queryModel = queryModel;
        try {
            this.versioningManager = Framework.getService(VersioningManager.class);
        } catch (Exception e) {
            throw new IllegalStateException("Versioning Manager not deployed",
                    e);
        }
        sections = new DocumentModelTreeImpl();
        DocumentModelList domains = coreSession.getChildren(
                coreSession.getRootDocument().getRef(), "Domain");
        for (DocumentModel domain : domains) {
            for (String sectionRootNameType : sectionRootTypes) {
                DocumentModelList children = coreSession.getChildren(
                        domain.getRef(), sectionRootNameType);
                for (DocumentModel sectionRoot : children) {
                    String sectionRootPath = sectionRoot.getPathAsString();
                    for (String sectionNameType : sectionTypes) {
                        accumulateAvailableSections(sections, sectionRootPath,
                                sectionNameType);
                    }
                }
            }
        }
    }

    /*
     * Use an unrestricted session to find all proxies and their versions.
     */
    @Override
    public void run() throws ClientException {
        /*
         * The DocumentModelTreeImpl datastructure contains section
         * DocumentModels from the base session, but we only access their ref so
         * it's safe. Also, the selected sections are filled with tree nodes
         * from the base session.
         */
        DocumentModelList publishedProxies = session.getProxies(currentDocRef,
                null);
        for (DocumentModel pProxy : publishedProxies) {
            for (DocumentModelTreeNode node : sections) {
                DocumentRef proxyParentRef = pProxy.getParentRef();
                DocumentRef sectionRef = node.getDocument().getRef();
                if (sectionRef.equals(proxyParentRef)) {
                    String versionLabel = versioningManager.getVersionLabel(pProxy);
                    node.setVersion(versionLabel);
                    // when looking at a proxy, don't check itself
                    if (!sectionRef.equals(currentParentRef)) {
                        treeNodes.add(node);
                    }
                    break;
                }
            }
        }
        model = new SelectDataModelImpl("SECTIONS_DOCUMENT_TREE", sections,
                treeNodes);
    }

    @SuppressWarnings("unchecked")
    private void accumulateAvailableSections(DocumentModelTree sections,
            String sectionRootPath, String sectionNameType)
            throws ClientException {

        Object[] params = { sectionRootPath, sectionNameType };

        PagedDocumentsProvider sectionsProvider = null;
        try {
            sectionsProvider = queryModel.getResultsProvider(coreSession, params);
        } catch (QueryException e) {
            throw new ClientException(String.format("Invalid search query. "
                    + "Check the \"%s\" QueryModel configuration",
                    "DOMAIN_SECTIONS"), e);
        }
        sectionsProvider.rewind();
        DocumentModelList mainSections = sectionsProvider.getCurrentPage();

        while (sectionsProvider.isNextPageAvailable()) {
            mainSections.addAll(sectionsProvider.getNextPage());
        }

        int firstLevel = sectionRootPath.split("/").length + 1;

        DocumentModelTreeImpl nodes = new DocumentModelTreeImpl();
        for (DocumentModel currentSection : mainSections) {
            if (coreSession.hasPermission(currentSection.getRef(),
                    SecurityConstants.READ)) {
                int currentLevel = currentSection.getPathAsString().split("/").length;
                nodes.add(currentSection, currentLevel - firstLevel);
            }
        }
        // sort sections using titles
        DocumentModelTreeNodeComparator comp = new DocumentModelTreeNodeComparator(
                nodes.getPathTitles());
        Collections.sort((ArrayList) nodes, comp);

        // populate sections
        for (DocumentModelTreeNode node : nodes) {
            sections.add(node);
        }
    }

    public SelectDataModel getDataModel() {
        return model;
    }

    public List<DocumentModelTreeNode> getTreeNodes() {
        return treeNodes;
    }
}
