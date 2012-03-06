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

package org.nuxeo.ecm.platform.publisher.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;

/**
 * Helper class to manage:
 * <ul>
 * <li>unrestricted fetch of Sections
 * <li>filtering according to user rights
 * </ul>
 *
 * @author tiry
 */
public class RootSectionsFinder extends UnrestrictedSessionRunner {

    protected static final String SCHEMA_PUBLISHING = "publishing";

    protected static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    protected Set<String> sectionRootTypes;

    protected Set<String> sectionTypes;

    protected CoreSession userSession;

    protected List<String> unrestrictedSectionRootFromWorkspaceConfig;

    protected List<String> unrestrictedDefaultSectionRoot;

    protected DocumentModelList accessibleSectionRoots;

    protected DocumentModel currentDocument;

    private static final Log log = LogFactory.getLog(RootSectionsFinder.class);

    public RootSectionsFinder(CoreSession userSession,
            Set<String> sectionRootTypes, Set<String> sectionTypes) {
        super(userSession);
        this.sectionRootTypes = sectionRootTypes;
        this.userSession = userSession;
        this.sectionTypes = sectionTypes;
    }

    public void reset() {
        this.currentDocument = null;
    }

    public DocumentModelList getAccessibleSectionRoots(DocumentModel currentDoc)
            throws ClientException {
        if ((currentDocument == null)
                || (!currentDocument.getRef().equals(currentDoc.getRef()))) {
            computeUserSectionRoots(currentDoc);
        }
        return accessibleSectionRoots;
    }

    public DocumentModelList getSectionRootsForWorkspace(
            DocumentModel currentDoc, boolean addDefaultSectionRoots)
            throws ClientException {
        if ((currentDocument == null)
                || (!currentDocument.getRef().equals(currentDoc.getRef()))) {
            computeUserSectionRoots(currentDoc);
        }

        if (unrestrictedDefaultSectionRoot.isEmpty() && addDefaultSectionRoots) {
            if (unrestrictedDefaultSectionRoot == null
                    || unrestrictedDefaultSectionRoot.isEmpty()) {
                DocumentModelList defaultSectionRoots = getDefaultSectionRoots(session);
                unrestrictedDefaultSectionRoot = new ArrayList<String>();
                for (DocumentModel root : defaultSectionRoots) {
                    unrestrictedDefaultSectionRoot.add(root.getPathAsString());
                }
            }
        }

        return getFiltredSectionRoots(
                unrestrictedSectionRootFromWorkspaceConfig, true);
    }

    public DocumentModelList getSectionRootsForWorkspace(
            DocumentModel currentDoc) throws ClientException {
        return getSectionRootsForWorkspace(currentDoc, false);
    }

    public DocumentModelList getDefaultSectionRoots(boolean onlyHeads,
            boolean addDefaultSectionRoots) throws ClientException {
        if (unrestrictedDefaultSectionRoot == null) {
            computeUserSectionRoots(null);
        }

        if (unrestrictedDefaultSectionRoot.isEmpty() && addDefaultSectionRoots) {
            if (unrestrictedDefaultSectionRoot == null
                    || unrestrictedDefaultSectionRoot.isEmpty()) {
                DocumentModelList defaultSectionRoots = getDefaultSectionRoots(session);
                unrestrictedDefaultSectionRoot = new ArrayList<String>();
                for (DocumentModel root : defaultSectionRoots) {
                    unrestrictedDefaultSectionRoot.add(root.getPathAsString());
                }
            }
        }

        return getFiltredSectionRoots(unrestrictedDefaultSectionRoot, onlyHeads);
    }

    public DocumentModelList getDefaultSectionRoots(boolean onlyHeads)
            throws ClientException {
        return getDefaultSectionRoots(onlyHeads, false);
    }

    protected void computeUserSectionRoots(DocumentModel currentDoc)
            throws ClientException {
        this.currentDocument = currentDoc;
        this.runUnrestricted();

        if (currentDoc != null) {
            if (!unrestrictedSectionRootFromWorkspaceConfig.isEmpty()) {
                accessibleSectionRoots = getFiltredSectionRoots(
                        unrestrictedSectionRootFromWorkspaceConfig, true);
            } else {
                accessibleSectionRoots = getFiltredSectionRoots(
                        unrestrictedDefaultSectionRoot, true);
            }
        }
    }

    protected DocumentModelList getFiltredSectionRoots(List<String> rootPaths,
            boolean onlyHeads) throws ClientException {
        List<DocumentRef> filtredDocRef = new ArrayList<DocumentRef>();
        List<DocumentRef> trashedDocRef = new ArrayList<DocumentRef>();

        for (String rootPath : rootPaths) {
            DocumentRef rootRef = new PathRef(rootPath);
            if (userSession.hasPermission(rootRef, SecurityConstants.READ)) {
                filtredDocRef.add(rootRef);
            } else {
                DocumentModelList accessibleSections = userSession.query(buildQuery(rootPath));
                for (DocumentModel section : accessibleSections) {
                    if (onlyHeads
                            && ((filtredDocRef.contains(section.getParentRef())) || (trashedDocRef.contains(section.getParentRef())))) {
                        trashedDocRef.add(section.getRef());
                    } else {
                        filtredDocRef.add(section.getRef());
                    }
                }
            }
        }
        DocumentModelList documents = userSession.getDocuments(filtredDocRef.toArray(new DocumentRef[filtredDocRef.size()]));
        return filterDocuments(documents);
    }

    protected DocumentModelList filterDocuments(DocumentModelList docs) {
        DocumentModelList filteredDocuments = new DocumentModelListImpl();
        FacetFilter facetFilter = new FacetFilter(
                Arrays.asList(FacetNames.FOLDERISH),
                Arrays.asList(FacetNames.HIDDEN_IN_NAVIGATION));
        LifeCycleFilter lfFilter = new LifeCycleFilter(
                LifeCycleConstants.DELETED_STATE, false);
        Filter filter = new CompoundFilter(facetFilter, lfFilter);
        for (DocumentModel doc : docs) {
            if (filter.accept(doc)) {
                filteredDocuments.add(doc);
            }
        }
        return filteredDocuments;
    }

    protected String buildQuery(String path) {
        // SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain'
        // and (ecm:primaryType = 'Section' or ecm:primaryType = 'SectionRoot'
        // )
        String pathForQuery = path.replaceAll("'", "\\\\'");
        String query = "SELECT * FROM Document WHERE ecm:path STARTSWITH '"
                + NXQLQueryBuilder.prepareStringLiteral(pathForQuery, true, true) + "' and (";

        int i = 0;
        for (String type : sectionTypes) {
            query = query + " ecm:primaryType = '" + type + "'";
            i++;
            if (i < sectionTypes.size()) {
                query = query + " or ";
            } else {
                query = query + " )";
            }
        }
        query = query + " order by ecm:path ";
        return query;
    }

    protected void computeUnrestrictedRoots(CoreSession session)
            throws ClientException {

        if (currentDocument != null) {
            /*
             * Get the first parent having "publishing" schema. In order to
             * void infinite loop, if the parent is 'Root' type just break
             * (NXP-3359).
             */
            DocumentModel parentDocumentModel = currentDocument;
            while (!parentDocumentModel.hasSchema(SCHEMA_PUBLISHING)) {
                if ("Root".equals(parentDocumentModel.getType())) {
                    break;
                }
                parentDocumentModel = session.getDocument(parentDocumentModel.getParentRef());
            }

            DocumentModelList sectionRootsFromWorkspaceConfig = getSectionRootsFromWorkspaceConfig(
                    parentDocumentModel, session);
            unrestrictedSectionRootFromWorkspaceConfig = new ArrayList<String>();
            for (DocumentModel root : sectionRootsFromWorkspaceConfig) {
                unrestrictedSectionRootFromWorkspaceConfig.add(root.getPathAsString());
            }
        }

        if (unrestrictedDefaultSectionRoot == null) {
            unrestrictedDefaultSectionRoot = Collections.emptyList();
        }
    }

    protected DocumentModelList getDefaultSectionRoots(CoreSession session)
            throws ClientException {
        DocumentModelList sectionRoots = new DocumentModelListImpl();
        DocumentModelList domains = session.getChildren(
                session.getRootDocument().getRef(), "Domain");
        for (DocumentModel domain : domains) {
            for (String sectionRootNameType : sectionRootTypes) {
                DocumentModelList children = session.getChildren(
                        domain.getRef(), sectionRootNameType);
                sectionRoots.addAll(children);
            }
        }
        return sectionRoots;
    }

    private DocumentModelList getSectionRootsFromWorkspaceConfig(
            DocumentModel workspace, CoreSession session)
            throws ClientException {

        DocumentModelList selectedSections = new DocumentModelListImpl();

        if (workspace.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) workspace.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList != null) {
                for (String currentSectionId : sectionIdsList) {
                    try {
                        DocumentModel sectionToAdd = session.getDocument(new IdRef(
                                currentSectionId));
                        selectedSections.add(sectionToAdd);
                    } catch (ClientException e) {
                        log.warn("Section with ID=" + currentSectionId
                                + " not found for document with ID="
                                + workspace.getId());
                    }
                }
            }
        }
        return selectedSections;
    }

    @Override
    public void run() throws ClientException {
        computeUnrestrictedRoots(session);
    }

}
