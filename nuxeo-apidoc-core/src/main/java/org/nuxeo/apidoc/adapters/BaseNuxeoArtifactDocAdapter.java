/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.apidoc.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public abstract class BaseNuxeoArtifactDocAdapter extends BaseNuxeoArtifact {

    protected static final Log log = LogFactory.getLog(BaseNuxeoArtifactDocAdapter.class);

    protected final DocumentModel doc;

    protected BaseNuxeoArtifactDocAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    protected static String computeDocumentName(String name) {
        return IdUtils.generateId(name, "-", true, 500);
    }

    protected static String getRootPath(CoreSession session, String basePath,
            String suffix) throws ClientException {
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
        if (doc == null) {
            return null;
        }
        return doc.getCoreSession();
    }

    protected <T> T getParentNuxeoArtifact(Class<T> artifactClass) {
        try {
            List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
            for (DocumentModel parent : parents) {
                T result = parent.getAdapter(artifactClass);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error while getting Parent artifact", e);
            return null;
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
        } catch (Exception e) {
            log.error("Error while getting property " + xPath, e);
            if (defaultValue == null) {
                return null;
            }
            return (T) defaultValue;
        }
    }

    public String getHierarchyPath() {

        String path = "";
        try {

            List<DocumentModel> parents = getCoreSession().getParentDocuments(doc.getRef());
            Collections.reverse(parents);

            for (DocumentModel doc : parents) {
                if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
                    break;
                }
                NuxeoArtifact item = doc.getAdapter(NuxeoArtifact.class);

                path = "/" + item.getId() + path;
            }
            return path;
        }
        catch (Exception e) {
            log.error("Error while computing Hierarchy path", e);
            return null;
        }

    }

}
