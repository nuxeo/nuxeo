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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

public abstract class AbstractVirtualBackend extends AbstractCoreBackend implements VirtualBackend {

    private static final Log log = LogFactory.getLog(AbstractVirtualBackend.class);

    protected Map<String, Backend> backendMap;

    protected LinkedList<String> orderedBackendNames;

    protected String rootUrl;

    private String backendDisplayName;

    private RealBackendFactory realBackendFactory;

    protected AbstractVirtualBackend(String name, String rootUrl, CoreSession session,
            RealBackendFactory realBackendFactory) {
        super(session);
        this.backendDisplayName = name;
        this.rootUrl = new Path(rootUrl).append(this.backendDisplayName).toString();
        this.realBackendFactory = realBackendFactory;
    }

    protected void registerSimpleBackends(List<DocumentModel> docs) {
        List<String> paths = new ArrayList<>();
        for (DocumentModel doc : docs) {
            paths.add(doc.getPathAsString());
        }

        List<String> heads = new ArrayList<>();
        for (int idx = 0; idx < paths.size(); idx++) {
            String path = paths.get(idx);
            if (isHead(path, paths, idx)) {
                heads.add(path);
            }
        }

        for (String head : heads) {
            String headName = new Path(head).lastSegment();
            String name = headName;
            int idx = 1;
            while (backendMap.containsKey(name)) {
                name = headName + "-" + idx;
                idx = idx + 1;
            }

            Backend backend = realBackendFactory.createBackend(name, head,
                    new Path(this.rootUrl).append(name).toString(), getSession());

            registerBackend(backend);
        }
    }

    private boolean isHead(String path, List<String> paths, int idx) {
        int level = new Path(path).segmentCount();

        for (int i = idx; i >= 0; i--) {
            String other = paths.get(i);
            if (path.contains(other)) {
                if (new Path(other).segmentCount() == level - 1) {
                    return false;
                }
            }
        }
        return true;
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
    public LinkedList<String> getVirtualFolderNames() {
        initIfNeed();
        if (orderedBackendNames == null) {
            return new LinkedList<>();
        }
        return orderedBackendNames;
    }

    protected void registerBackend(Backend backend) {
        if (backendMap == null) {
            backendMap = new ConcurrentHashMap<>();
        }
        if (orderedBackendNames == null) {
            orderedBackendNames = new LinkedList<>();
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
            initIfNeed();
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

    protected void initIfNeed() {
        if (backendMap == null || orderedBackendNames == null) {
            backendMap = new HashMap<>();
            orderedBackendNames = new LinkedList<>();
            init();
        }
    }

    protected abstract void init();

    @Override
    public boolean isLocked(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUnlock(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lock(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unlock(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCheckoutUser(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel resolveLocation(String location) {
        throw new UnsupportedOperationException(location);
    }

    @Override
    public Path parseLocation(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeItem(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeItem(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameItem(DocumentModel source, String destinationName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel moveItem(DocumentModel source, PathRef targetParentRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel copyItem(DocumentModel source, PathRef targetParentRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFolder(String parentPath, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFile(String parentPath, String name, Blob content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createFile(String parentPath, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DocumentModel> getChildren(DocumentRef ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRename(String source, String destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String location) {
        throw new UnsupportedOperationException(location);
    }

    @Override
    public String getDisplayName(DocumentModel doc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(DocumentRef docRef, String permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel getDocument(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel updateDocument(DocumentModel doc, String name, Blob content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel moveItem(DocumentModel source, DocumentRef targetParentRef, String name)
            {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVirtualPath(String path) {
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

    @Override
    public LinkedList<String> getOrderedBackendNames() {
        initIfNeed();
        return orderedBackendNames;
    }

}
