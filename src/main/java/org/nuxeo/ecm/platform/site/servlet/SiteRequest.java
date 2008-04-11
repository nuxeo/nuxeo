/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.site.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;

/**
 * Request wrapper for SiteObjects publishing
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */

public class SiteRequest extends HttpServletRequestWrapper {

    public static final String MODE_KEY = "render_mode";

    public static final String ENGINE_KEY = "render_engine";

    public static final String VIEW_MODE = "VIEW";

    public static final String EDIT_MODE = "EDIT";

    public static final String CREATE_MODE = "CREATE";

    public static final String UNRESOLVED_SUBPATH = "unresolvedSubPath";

    protected String mode = VIEW_MODE;

    protected String engineName = "default";

    protected boolean cancelRenderingFlag = false;

    private SiteAwareObject currentSiteObject;

    protected List<SiteAwareObject> traversedObjects = new ArrayList<SiteAwareObject>();

    protected List<DocumentModel> nonTraversedDocs = new ArrayList<DocumentModel>();

    protected List<DocumentModel> docsToTraverse;

    protected CoreSession documentManager;


    public boolean hasUnresolvedSubPath() {
        List<String> unresolved = getUnresolvedPath();
        if (unresolved == null || unresolved.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<String> getUnresolvedPath() {
        Object unresolvedPath = getAttribute(UNRESOLVED_SUBPATH);
        if (unresolvedPath == null) {
            return null;
        }
        return (List<String>) unresolvedPath;
    }

    public void setCurrentSiteObject(SiteAwareObject siteObject) {
        currentSiteObject = siteObject;
    }

    public SiteAwareObject getCurrentSiteObject() {
        return currentSiteObject;
    }

    public void cancelRendering() {
        cancelRenderingFlag = true;
    }

    public boolean isRenderingCanceled() {
        return cancelRenderingFlag;
    }

    public void addToTraversalPath(SiteAwareObject siteOb) {
        traversedObjects.add(siteOb);
    }

    public List<SiteAwareObject> getTraversalPath() {
        return traversedObjects;
    }

    public String getSiteBaseUrl() {
        String servletBase = getContextPath();
        if (servletBase == null) {
            servletBase = "/nuxeo/site"; // for testing
        } else {
            servletBase += getServletPath();
        }

        return servletBase + '/';
    }

    public String getTraveredURL(SiteAwareObject siteObject) {
        SiteAwareObject parent = getTraversalParent(siteObject);
        if (parent == null) {
            return getSiteBaseUrl() + siteObject.getName();
        } else {
            return getTraveredURL(parent) + '/' + siteObject.getName();
        }
    }

    private Integer getSiteObjectIndexInTraversalPath(SiteAwareObject siteObject) {
        for (int i = traversedObjects.size() - 1; i >= 0; i--) {
            if (traversedObjects.get(i).getId().equals(siteObject.getId())) {
                return i;
            }
        }
        return null;
    }

    public SiteAwareObject getTraversalParent() {
        return getTraversalParent(currentSiteObject);
    }

    public SiteAwareObject getTraversalParent(SiteAwareObject siteObject) {
        if (traversedObjects == null || traversedObjects.isEmpty()) {
            return null;
        }

        Integer idx = getSiteObjectIndexInTraversalPath(siteObject);
        if (idx == null || idx == 0) {
            return null;
        } else {
            return traversedObjects.get(idx - 1);
        }
    }

    public SiteAwareObject getTraversalChild() {
        return getTraversalChild(currentSiteObject);
    }

    public SiteAwareObject getTraversalChild(SiteAwareObject siteObject) {
        if (traversedObjects == null || traversedObjects.isEmpty()) {
            return null;
        }

        Integer idx = getSiteObjectIndexInTraversalPath(siteObject);
        if (idx == null || idx == (traversedObjects.size() - 1)) {
            return null;
        } else {
            return traversedObjects.get(idx + 1);
        }
    }

    public String getLiefSiteObjectId() {
        if (traversedObjects == null || traversedObjects.isEmpty()) {
            return null;
        }
        return traversedObjects.get(traversedObjects.size() - 1).getId();
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public SiteRequest(HttpServletRequest request) {
        super(request);

        String requestedMode = getParameter(MODE_KEY);
        if (requestedMode != null) {
            setMode(requestedMode);
        }
    }

    public SiteRequest(HttpServletRequest request, CoreSession session) {
        super(request);

        String requestedMode = getParameter(MODE_KEY);
        if (requestedMode != null) {
            setMode(requestedMode);
        }
        documentManager = session;
    }

    public SiteRequest(HttpServletRequest request, CoreSession session,
            List<DocumentModel> docsToTraverse) {
        this(request, session);
        this.docsToTraverse = docsToTraverse;
    }

    public List<DocumentModel> getNonTraversedDocs() {
        return nonTraversedDocs;
    }

    public void setNonTraversedDocs(List<DocumentModel> nonTraversedDocs) {
        this.nonTraversedDocs = nonTraversedDocs;
    }

    public CoreSession getDocumentManager() {
        if (documentManager == null) {
            HttpSession httpSession = getSession(true);
            if (httpSession != null) {
                documentManager = (CoreSession) httpSession.getAttribute(
                        SiteServlet.CORESESSION_KEY);
            }
        }
        return documentManager;
    }

    public List<DocumentModel> getDocsToTraverse() {
        return docsToTraverse;
    }

    public void setDocsToTraverse(List<DocumentModel> docsToTraverse) {
        this.docsToTraverse = docsToTraverse;
    }

}
