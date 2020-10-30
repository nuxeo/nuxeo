/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.publisher.impl.finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractRootSectionsFinder extends UnrestrictedSessionRunner implements RootSectionFinder {

    public static final String SCHEMA_PUBLISHING = "publishing";

    public static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    protected static Set<String> sectionRootTypes;

    protected static Set<String> sectionTypes;

    protected CoreSession userSession;

    protected List<String> unrestrictedSectionRootFromWorkspaceConfig;

    protected List<String> unrestrictedDefaultSectionRoot;

    protected DocumentModelList accessibleSectionRoots;

    protected DocumentModel currentDocument;

    protected static final Log log = LogFactory.getLog(AbstractRootSectionsFinder.class);

    protected abstract void computeUserSectionRoots(DocumentModel currentDoc);

    protected abstract String buildQuery(String path);

    protected abstract void computeUnrestrictedRoots(CoreSession session);

    public AbstractRootSectionsFinder(CoreSession userSession) {
        super(userSession);
        this.userSession = userSession;
    }

    @Override
    public void reset() {
        this.currentDocument = null;
    }

    @Override
    public DocumentModelList getAccessibleSectionRoots(DocumentModel currentDoc) {
        if ((currentDocument == null) || (!currentDocument.getRef().equals(currentDoc.getRef()))) {
            computeUserSectionRoots(currentDoc);
        }
        return accessibleSectionRoots;
    }

    @Override
    public DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc, boolean addDefaultSectionRoots)
            {
        if ((currentDocument == null) || (!currentDocument.getRef().equals(currentDoc.getRef()))) {
            computeUserSectionRoots(currentDoc);
        }

        if (unrestrictedDefaultSectionRoot.isEmpty() && addDefaultSectionRoots) {
            DocumentModelList defaultSectionRoots = getDefaultSectionRoots(session);
            unrestrictedDefaultSectionRoot = new ArrayList<>();
            for (DocumentModel root : defaultSectionRoots) {
                unrestrictedDefaultSectionRoot.add(root.getPathAsString());
            }
        }

        return getFiltredSectionRoots(unrestrictedSectionRootFromWorkspaceConfig, true);
    }

    @Override
    public DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc) {
        return getSectionRootsForWorkspace(currentDoc, false);
    }

    @Override
    public DocumentModelList getDefaultSectionRoots(boolean onlyHeads, boolean addDefaultSectionRoots)
            {
        if (unrestrictedDefaultSectionRoot == null) {
            computeUserSectionRoots(null);
        }

        if (unrestrictedDefaultSectionRoot.isEmpty() && addDefaultSectionRoots) {
            DocumentModelList defaultSectionRoots = getDefaultSectionRoots(session);
            unrestrictedDefaultSectionRoot = new ArrayList<>();
            for (DocumentModel root : defaultSectionRoots) {
                unrestrictedDefaultSectionRoot.add(root.getPathAsString());
            }
        }

        return getFiltredSectionRoots(unrestrictedDefaultSectionRoot, onlyHeads);
    }

    @Override
    public DocumentModelList getDefaultSectionRoots(boolean onlyHeads) {
        return getDefaultSectionRoots(onlyHeads, false);
    }

    protected DocumentModelList getFiltredSectionRoots(List<String> rootPaths, boolean onlyHeads)
            {
        List<DocumentRef> filtredDocRef = new ArrayList<>();
        List<DocumentRef> trashedDocRef = new ArrayList<>();

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
        FacetFilter facetFilter = new FacetFilter(Arrays.asList(FacetNames.FOLDERISH),
                Arrays.asList(FacetNames.HIDDEN_IN_NAVIGATION));
        Filter trashedFilter = docModel -> !docModel.isTrashed();
        Filter filter = new CompoundFilter(facetFilter, trashedFilter);
        for (DocumentModel doc : docs) {
            if (filter.accept(doc)) {
                filteredDocuments.add(doc);
            }
        }
        return filteredDocuments;
    }

    protected DocumentModelList getDefaultSectionRoots(CoreSession session) {
        // XXX replace by a query !!!
        DocumentModelList sectionRoots = new DocumentModelListImpl();
        DocumentModelList domains = session.getChildren(session.getRootDocument().getRef(), "Domain");
        for (DocumentModel domain : domains) {
            for (String sectionRootNameType : getSectionRootTypes()) {
                DocumentModelList children = session.getChildren(domain.getRef(), sectionRootNameType);
                sectionRoots.addAll(children);
            }
        }
        return sectionRoots;
    }

    protected DocumentModelList getSectionRootsFromWorkspaceConfig(DocumentModel workspace, CoreSession session)
            {

        DocumentModelList selectedSections = new DocumentModelListImpl();

        if (workspace.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) workspace.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList != null) {
                for (String currentSectionId : sectionIdsList) {
                    try {
                        DocumentModel sectionToAdd = session.getDocument(new IdRef(currentSectionId));
                        selectedSections.add(sectionToAdd);
                    } catch (DocumentNotFoundException e) {
                        log.warn("Section with ID=" + currentSectionId + " not found for document with ID="
                                + workspace.getId());
                    }
                }
            }
        }
        return selectedSections;
    }

    @Override
    public void run() {
        computeUnrestrictedRoots(session);
    }

    protected Set<String> getSectionRootTypes() {
        if (sectionRootTypes == null) {
            sectionRootTypes = getTypeNamesForFacet(FacetNames.MASTER_PUBLISH_SPACE);
            if (sectionRootTypes == null) {
                sectionRootTypes = new HashSet<>();
            }
        }
        return sectionRootTypes;
    }

    protected Set<String> getTypeNamesForFacet(String facetName) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> publishRoots = schemaManager.getDocumentTypeNamesForFacet(facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    protected Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet(FacetNames.MASTER_PUBLISH_SPACE);
            if (sectionTypes == null) {
                sectionTypes = new HashSet<>();
            }
        }
        return sectionTypes;
    }

}
