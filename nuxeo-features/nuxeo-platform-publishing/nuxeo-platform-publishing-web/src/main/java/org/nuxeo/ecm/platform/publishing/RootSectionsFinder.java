package org.nuxeo.ecm.platform.publishing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 *
 * Helper class to manage : - unrestricted fetch of Sections - filtering
 * according to user rights
 *
 * @author tiry
 *
 */
public class RootSectionsFinder extends UnrestrictedSessionRunner {

    protected static final String SCHEMA_PUBLISHING = "publishing";

    protected static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    protected Set<String> sectionRootTypes = null;

    protected Set<String> sectionTypes = null;

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
            DocumentModel currentDoc) throws ClientException {
        if ((currentDocument == null)
                || (!currentDocument.getRef().equals(currentDoc.getRef()))) {
            computeUserSectionRoots(currentDoc);
        }
        return getFiltredSectionRoots(
                unrestrictedSectionRootFromWorkspaceConfig, true);
    }

    public DocumentModelList getDefaultSectionRoots(boolean onlyHeads)
            throws ClientException {
        if (unrestrictedDefaultSectionRoot == null) {
            computeUserSectionRoots(null);
        }
        return getFiltredSectionRoots(unrestrictedDefaultSectionRoot, onlyHeads);
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
                DocumentModelList accessibleSections = userSession
                        .query(buildQuery(rootPath));
                for (DocumentModel section : accessibleSections) {
                    if (onlyHeads
                            && ((filtredDocRef.contains(section.getParentRef())) || (trashedDocRef
                                    .contains(section.getParentRef())))) {
                        trashedDocRef.add(section.getRef());
                    } else {
                        filtredDocRef.add(section.getRef());
                    }
                }
            }
        }
        return userSession.getDocuments(filtredDocRef
                .toArray(new DocumentRef[filtredDocRef.size()]));
    }

    protected String buildQuery(String path) {
        // SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain'
        // and (ecm:primaryType = 'Section' or ecm:primaryType = 'SectionRoot' )
        String query = "SELECT * FROM Document WHERE ecm:path STARTSWITH '"
                + path + "' and (";

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
             * Get the first parent having "publishing" schema. In order to void
             * infinite loop, if the parent is 'Root' type just break
             * (NXP-3359).
             */
            DocumentModel parentDocumentModel = currentDocument;
            while (!parentDocumentModel.hasSchema(SCHEMA_PUBLISHING)) {
                if ("Root".equals(parentDocumentModel.getType())) {
                    break;
                }
                parentDocumentModel = session.getDocument(parentDocumentModel
                        .getParentRef());
            }

            DocumentModelList sectionRootsFromWorkspaceConfig = getSectionRootsFromWorkspaceConfig(
                    parentDocumentModel, session);
            unrestrictedSectionRootFromWorkspaceConfig = new ArrayList<String>();
            for (DocumentModel root : sectionRootsFromWorkspaceConfig) {
                unrestrictedSectionRootFromWorkspaceConfig.add(root
                        .getPathAsString());
            }
        }

        if (unrestrictedDefaultSectionRoot == null
                || unrestrictedDefaultSectionRoot.isEmpty()) {
            DocumentModelList defaultSectionRoots = getDefaultSectionRoots(session);
            unrestrictedDefaultSectionRoot = new ArrayList<String>();
            for (DocumentModel root : defaultSectionRoots) {
                unrestrictedDefaultSectionRoot.add(root.getPathAsString());
            }
        }
    }

    protected DocumentModelList getDefaultSectionRoots(CoreSession session)
            throws ClientException {
        DocumentModelList sectionRoots = new DocumentModelListImpl();
        DocumentModelList domains = session.getChildren(session
                .getRootDocument().getRef(), "Domain");
        for (DocumentModel domain : domains) {
            for (String sectionRootNameType : sectionRootTypes) {
                DocumentModelList children = session.getChildren(domain
                        .getRef(), sectionRootNameType);
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
            String[] sectionIdsArray = (String[]) workspace
                    .getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList != null) {
                for (String currentSectionId : sectionIdsList) {
                    try {
                        DocumentModel sectionToAdd = session
                                .getDocument(new IdRef(currentSectionId));
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
