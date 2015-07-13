package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface RootSectionFinder {

    void reset();

    /**
     * Returns the head (root) sections that are bound to a given Workspace.
     * <p>
     * If no specific binding is defined at the workspace level, an empty list is returned.
     *
     * @param currentDoc the target Workspace
     * @param addDefaultSectionRoots flag to indicate is default roots should be added
     * @return
     */
    DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc, boolean addDefaultSectionRoots);

    /**
     * Returns the head (root) sections that are bound to a given Workspace.
     * <p>
     * If no specific binding is defined at the workspace level, an empty list is returned.
     *
     * @param currentDoc the target Workspace
     * @return
     */
    DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc);

    DocumentModelList getAccessibleSectionRoots(DocumentModel currentDoc);

    DocumentModelList getDefaultSectionRoots(boolean onlyHeads, boolean addDefaultSectionRoots);

    DocumentModelList getDefaultSectionRoots(boolean onlyHeads);

}
