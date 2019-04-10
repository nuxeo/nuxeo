/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

import java.util.LinkedList;
import java.util.List;

public interface Backend {

    /** @deprecated since 10.10, unused */
    @Deprecated
    String getRootPath();

    /** @deprecated since 10.10, unused */
    @Deprecated
    String getRootUrl();

    CoreSession getSession();

    CoreSession getSession(boolean synchronize);

    String getBackendDisplayName();

    void saveChanges();

    boolean isLocked(DocumentRef ref);

    boolean canUnlock(DocumentRef ref);

    String lock(DocumentRef ref);

    boolean unlock(DocumentRef ref);

    String getCheckoutUser(DocumentRef ref);

    Path parseLocation(String location);

    DocumentModel resolveLocation(String location);

    void removeItem(String location);

    void removeItem(DocumentRef ref);

    /** @deprecated since 10.10, unused, use {@link #moveItem} instead */
    @Deprecated
    void renameItem(DocumentModel source, String destinationName);

    /** @deprecated since 10.10, unused */
    @Deprecated
    DocumentModel moveItem(DocumentModel source, PathRef targetParentRef);

    DocumentModel moveItem(DocumentModel source, DocumentRef targetParentRef, String name);

    DocumentModel updateDocument(DocumentModel doc, String name, Blob content);

    DocumentModel copyItem(DocumentModel source, PathRef targetParentRef);

    DocumentModel createFolder(String parentPath, String name);

    DocumentModel createFile(String parentPath, String name, Blob content);

    /** @deprecated since 10.10, unused */
    @Deprecated
    DocumentModel createFile(String parentPath, String name);

    List<DocumentModel> getChildren(DocumentRef ref);

    /** @deprecated since 10.10, unused */
    @Deprecated
    boolean isRename(String source, String destination);

    boolean exists(String location);

    boolean hasPermission(DocumentRef docRef, String permission);

    String getDisplayName(DocumentModel doc);

    LinkedList<String> getVirtualFolderNames();

    Backend getBackend(String path);

    boolean isVirtual();

    /** @deprecated since 10.10, unused */
    @Deprecated
    boolean isRoot();

    String getVirtualPath(String path);

    DocumentModel getDocument(String location);

}
