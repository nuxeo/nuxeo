package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface RootSectionFinder {

    void reset();

    /**
     * Returns the head (root) sections that are bound to a given Workspace.
     * <p>
     * If no specific binding is defined at the workspace level, an empty list
     * is returned.
     * 
     * @param currentDoc the target Workspace
     * @param addDefaultSectionRoots flag to indicate is default roots should be
     *            added
     * @return
     * @throws ClientException
     */
    DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc,
            boolean addDefaultSectionRoots) throws ClientException;

    /**
     * Returns the head (root) sections that are bound to a given Workspace.
     * <p>
     * If no specific binding is defined at the workspace level, an empty list
     * is returned.
     * 
     * @param currentDoc the target Workspace
     * 
     * @return
     * @throws ClientException
     */
    DocumentModelList getSectionRootsForWorkspace(DocumentModel currentDoc)
            throws ClientException;

    DocumentModelList getAccessibleSectionRoots(DocumentModel currentDoc)
            throws ClientException;

    DocumentModelList getDefaultSectionRoots(boolean onlyHeads,
            boolean addDefaultSectionRoots) throws ClientException;

    DocumentModelList getDefaultSectionRoots(boolean onlyHeads)
            throws ClientException;

}