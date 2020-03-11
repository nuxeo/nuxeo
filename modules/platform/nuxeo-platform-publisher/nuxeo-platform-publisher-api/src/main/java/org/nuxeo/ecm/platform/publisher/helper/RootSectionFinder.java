/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
