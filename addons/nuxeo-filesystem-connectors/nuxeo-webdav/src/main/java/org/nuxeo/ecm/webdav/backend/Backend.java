/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    String getRootPath();

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

    void renameItem(DocumentModel source, String destinationName);

    DocumentModel moveItem(DocumentModel source, PathRef targetParentRef);

    DocumentModel moveItem(DocumentModel source, DocumentRef targetParentRef, String name);

    DocumentModel updateDocument(DocumentModel doc, String name, Blob content);

    DocumentModel copyItem(DocumentModel source, PathRef targetParentRef);

    DocumentModel createFolder(String parentPath, String name);

    DocumentModel createFile(String parentPath, String name, Blob content);

    DocumentModel createFile(String parentPath, String name);

    List<DocumentModel> getChildren(DocumentRef ref);

    boolean isRename(String source, String destination);

    boolean exists(String location);

    boolean hasPermission(DocumentRef docRef, String permission);

    String getDisplayName(DocumentModel doc);

    LinkedList<String> getVirtualFolderNames();

    Backend getBackend(String path);

    boolean isVirtual();

    boolean isRoot();

    String getVirtualPath(String path);

    DocumentModel getDocument(String location);

}
