/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.adapters;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;

public abstract class BaseNuxeoArtifactDocAdapter extends BaseNuxeoArtifact {

    protected static final Log log = LogFactory.getLog(BaseNuxeoArtifactDocAdapter.class);

    protected final DocumentModel doc;

    protected static final ThreadLocal<CoreSession> localCoreSession = new ThreadLocal<>();

    public static void setLocalCoreSession(CoreSession session) {
        localCoreSession.set(session);
    }

    public static void releaseLocalCoreSession() {
        localCoreSession.remove();
    }

    protected BaseNuxeoArtifactDocAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    protected static String computeDocumentName(String name) {
        return IdUtils.generateId(name, "-", true, 500);
    }

    protected static String getRootPath(CoreSession session, String basePath, String suffix) {
        PathRef rootRef = new PathRef(basePath);
        if (session.exists(rootRef)) {
            Path path = new Path(basePath).append(suffix);
            rootRef = new PathRef(path.toString());
            if (session.exists(rootRef)) {
                return path.toString();
            } else {
                DocumentModel root = session.createDocumentModel("Folder");
                root.setPathInfo(basePath, suffix);
                root = session.createDocument(root);
                return root.getPathAsString();
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return doc.getId().hashCode();
    }

    public DocumentModel getDoc() {
        return doc;
    }

    protected CoreSession getCoreSession() {
        CoreSession session = null;
        if (doc != null) {
            session = doc.getCoreSession();
        }
        if (session == null) {
            session = localCoreSession.get();
        }
        return session;
    }

    protected <T> T getParentNuxeoArtifact(Class<T> artifactClass) {
        List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
        for (DocumentModel parent : parents) {
            T result = parent.getAdapter(artifactClass);
            if (result != null) {
                return result;
            }
        }
        log.error("Parent artifact not found ");
        return null;
    }

    protected String safeGet(String xPath) {
        return safeGet(String.class, xPath, null);
    }

    protected String safeGet(String xPath, String defaultValue) {
        return safeGet(String.class, xPath, defaultValue);
    }

    @SuppressWarnings("unchecked")
    protected <T> T safeGet(Class<T> typ, String xPath, Object defaultValue) {
        try {
            T value = (T) doc.getPropertyValue(xPath);
            return value;
        } catch (PropertyException e) {
            log.error("Error while getting property " + xPath, e);
            if (defaultValue == null) {
                return null;
            }
            return (T) defaultValue;
        }
    }

    @Override
    public String getHierarchyPath() {
        List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
        Collections.reverse(parents);

        String path = "";
        for (DocumentModel doc : parents) {
            if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
                break;
            }
            if (doc.getType().equals(DistributionSnapshot.CONTAINER_TYPE_NAME)) {
                // skip containers
                continue;
            }
            NuxeoArtifact item = doc.getAdapter(NuxeoArtifact.class);

            path = "/" + item.getId() + path;
        }
        return path;
    }

}
