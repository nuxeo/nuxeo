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
package org.nuxeo.ecm.platform.wi.backend;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

public abstract class AbstractVirtualBackend extends AbstractCoreBackend {

    private static final Log log = LogFactory.getLog(AbstractVirtualBackend.class);

    protected Map<String, Backend> backendMap;

    protected LinkedList<String> orderedBackendNames;

    protected String rootUrl;

    private String backendDisplayName;

    protected AbstractVirtualBackend(String name, String rootUrl) {
        this(name, rootUrl, null);
    }

    protected AbstractVirtualBackend(String name, String rootUrl,
            CoreSession session) {
        super(session);
        this.backendDisplayName = name;
        this.rootUrl = new Path(rootUrl).append(this.backendDisplayName).toString();
    }

    @Override
    public String getRootPath() {
        return "";
    }

    @Override
    public String getRootUrl() {
        return rootUrl;
    }

    @Override
    public final boolean isVirtual() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public String getBackendDisplayName() {
        return backendDisplayName;
    }

    @Override
    public LinkedList<String> getVirtualFolderNames() throws ClientException {
        initIfNeed();
        if (orderedBackendNames == null) {
            return new LinkedList<String>();
        }
        return orderedBackendNames;
    }

    protected void registerBackend(Backend backend) {
        if (backendMap == null) {
            backendMap = new ConcurrentHashMap<String, Backend>();
        }
        if (orderedBackendNames == null) {
            orderedBackendNames = new LinkedList<String>();
        }
        backendMap.put(backend.getBackendDisplayName(), backend);
        orderedBackendNames.add(backend.getBackendDisplayName());
    }

    @Override
    public Backend getBackend(String uri) {
        Path path = new Path(uri);
        if (path.segmentCount() == 0) {
            return this;
        } else {
            String key = path.segment(0);
            try {
                initIfNeed();
            } catch (ClientException e) {
                log.error("Error during backend initialization", e);
                return null;
            }
            if (backendMap == null) {
                return null;
            }
            Backend backend = backendMap.get(key);
            if (backend == null) {
                return null;
            }
            String location = path.removeFirstSegments(1).toString();
            return backend.getBackend(location);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (backendMap != null) {
            for (Backend backend : backendMap.values()) {
                backend.destroy();
            }
        }
    }

    protected abstract boolean initIfNeed() throws ClientException;

    @Override
    public boolean isLocked(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUnlock(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lock(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unlock(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCheckoutUser(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel resolveLocation(String location)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path parseLocation(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeItem(String location) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeItem(DocumentRef ref) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameItem(DocumentModel source, String destinationName)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel moveItem(DocumentModel source, PathRef targetParentRef)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel copyItem(DocumentModel source, PathRef targetParentRef)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFolder(String parentPath, String name)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFile(String parentPath, String name, Blob content)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFile(String parentPath, String name)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DocumentModel> getChildren(DocumentRef ref)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRename(String source, String destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDisplayName(DocumentModel doc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(DocumentRef docRef, String permission)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel saveDocument(DocumentModel doc) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel moveItem(DocumentModel source,
            DocumentRef targetParentRef, String name) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVirtualPath(String path) throws ClientException {
        initIfNeed();
        for (String backendName : orderedBackendNames) {
            Backend backend = backendMap.get(backendName);
            String url = backend.getVirtualPath(path);
            if (StringUtils.isNotEmpty(url)) {
                return url;
            }
        }
        return null;
    }
}
