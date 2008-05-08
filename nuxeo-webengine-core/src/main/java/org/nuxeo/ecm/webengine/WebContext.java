/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.rendering.SiteRenderingContext;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.servlet.WebConst;
import org.nuxeo.ecm.webengine.util.FormData;
import org.nuxeo.runtime.api.Framework;
import org.python.core.PyDictionary;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebContext extends HttpServletRequestWrapper implements WebConst {

    public static final String CORESESSION_KEY = "SiteCoreSession";

    protected final WebEngine engine;
    protected CoreSession session;
    protected boolean isCanceled = false;

    protected WebObject head; // the site root
    protected WebObject tail;
    protected WebObject lastResolved;

    protected final HttpServletResponse resp;

    protected String pathInfo;

    protected final WebRoot siteRoot;
    protected Mapping mapping;
    protected String action; // the current object view
    protected FormData form;

    protected final Map<String,Object> vars; // global vars to share between scripts

    public WebContext(WebRoot root, HttpServletRequest req, HttpServletResponse resp) {
        super(req);
        siteRoot = root;
        engine = root.getWebEngine();
        this.resp = resp;
        vars = new HashMap<String, Object>();
    }

    public void setVar(String name, Object value) {
        vars.put(name, value);
    }

    public Object getVar(String name) {
        return vars.get(name);
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    /**
     * @return the form.
     */
    public FormData getForm() {
        if (form == null) {
            form = new FormData(this);
        }
        return form;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public String getMappingVar(String name) {
        return mapping != null ? mapping.getValue(name) : null;
    }

    public String getMappingVar(int index) {
        return mapping != null ? mapping.getValue(index) : null;
    }

    public int getMappingVarCount() {
        return mapping != null ? mapping.size() : 0;
    }

    public ScriptFile getTargetScript() throws IOException {
        String type = (lastResolved != null) ? lastResolved.getDocument().getType() : null;
        String path = null;
        if (mapping != null) {
            return siteRoot.getScript(mapping.getScript(), type);
        } else if (action != null) {
            if (lastResolved != null) {
                path = lastResolved.getActionScript(action);
            }
        }
        if (path == null) {
            WebObject first = getFirstUnresolvedObject();
            if (first != null) {
                if (first != tail) {
                    path = getPath(first, null);
                } else {
                    path = first.getName();
                }
            } else {
                path = siteRoot.getDefaultPage();
            }
        }
        return siteRoot.getScript(path, type);
    }

    public WebEngine getWebEngine() {
        return engine;
    }

    public WebObject getSiteRoot() {
        return head;
    }

    public HttpServletResponse getResponse() {
        return resp;
    }

    @Override
    public HttpServletRequest getRequest() {
        return (HttpServletRequest) super.getRequest();
    }

    public CoreSession getCoreSession() throws Exception {
        if (session == null) {
            session = getCoreSession(this);
        }
        return session;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public String getSiteBaseUrl() {
        String servletBase = getContextPath();
        if (servletBase == null) {
            servletBase = "/nuxeo/site"; // for testing
        } else {
            servletBase += getServletPath();
        }
        return servletBase;
    }

    public String getAbsolutePath() {
        return getSiteBaseUrl()+getPathInfo();
    }

    public String getPath() {
        return getPathInfo();
    }

    public String getObjectPath() {
        return lastResolved == null ? null : lastResolved.getPath();
    }

    public String getObjectAbsolutePath() {
        return lastResolved == null ? null : lastResolved.getAbsolutePath();
    }


    public WebObject getLastResolvedObject() {
        return lastResolved;
    }

    public WebObject getFirstUnresolvedObject() {
        return lastResolved == null ? head : lastResolved.next;
    }

    public boolean hasUnresolvedObjects() {
        return lastResolved != tail;
    }

    public String getFirstUnresolvedSegment() {
        WebObject obj = getFirstUnresolvedObject();
        return obj != null ? obj .getName() : null;
    }

    public WebObject addSiteObject(String name, DocumentModel doc) {
        WebObject object = new WebObject(this, name, doc);
        if (head == null) {
            head = tail = object;
            object.prev = null;
        } else {
            tail.next = object;
            object.prev = tail;
        }
        object.next = null;
        tail = object;
        if (doc != null) {
            lastResolved = object;
        }
        return object;
    }

    public List<WebObject> getSiteObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<WebObject> getUnresolvedSiteObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<WebObject> getResolvedSiteObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public WebObject getLastObject() {
        return tail;
    }

    public boolean isRootRequest() {
        return head != null && head.next == null;
    }

    public WebRoot getRoot() {
        return siteRoot;
    }

    /**
     *
     * @return the last traversed object
     */
    public WebObject traverse() throws WebException {
       if (head == null || lastResolved == null) {
           return null;
       }
       WebObject lastTraversed = head;
       WebObject p = head;
       while (p != lastResolved.next) {
           if (!p.traverse()) {
               return lastTraversed;
           }
           lastTraversed = p;
           p = p.next;
       }
       return lastTraversed;
   }

   /**
    *
    * @param start inclusive
    * @param end exclusive
    * @return
    */
   public static String getPath(WebObject start, WebObject end) {
       if (start == null || start == end) {
           return "";
       }
       StringBuilder buf = new StringBuilder(256);
       WebObject p = start;
       while (p != end) {
           buf.append('/').append(p.name);
           p = p.next;
       }
       return buf.toString();
   }

   public String getUnresolvedPath() {
       if (lastResolved == null) {
           return getPath(head, null);
       }
       return getPath(lastResolved.next, null);
   }

   public String getResolvedPath() {
       if (lastResolved == null) {
           return "";
       }
       return getPath(head, lastResolved.next);
   }

   @Override
   public String toString() {
       return "Resolved Path: " + getResolvedPath() + "; Unresolved Path:" + getUnresolvedPath()
               + "; Action: " + action + "; Mapping: " + (mapping == null ? "none" : mapping.getScript());
   }

    public void render(String template) throws Exception {
       render(template, vars);
   }

    public void render(String template, Object ctx) throws Exception {
            Map map = null;
            if (ctx instanceof Map) {
                map = (Map) ctx;
            } else if (ctx instanceof PyDictionary) {
                map = Scripting.convertPythonMap((PyDictionary) ctx);
            }
            if (lastResolved != null) {
            engine.getScripting().getRenderingEngine().render(template, lastResolved,
                    (Map<String, Object>) map);
        } else {
            engine.getScripting().getRenderingEngine().render(template, new SiteRenderingContext(this),
                    (Map<String, Object>) map);
        }
    }

    public DocumentModelList query(String query) throws Exception {
       SearchService search = Framework.getService(SearchService.class);
       if (search == null) {
           return session.query(query);
       }
       ResultSet result = search.searchQuery(new ComposedNXQueryImpl(query), 0, Integer.MAX_VALUE);
       DocumentModelList docs = new DocumentModelListImpl();
       for (ResultItem item : result) {
           String id = (String)item.get("ecm:uuid");
           DocumentModel doc = session.getDocument(new IdRef(id));
           docs.add(doc);
       }
       return docs;
   }

    public static CoreSession getCoreSession(HttpServletRequest request)
            throws Exception {

//     for testing
       CoreSession session = (CoreSession) request.getAttribute("TestCoreSession");

       HttpSession httpSession = request.getSession(true);
       if (session == null) {
           session = (CoreSession) httpSession.getAttribute(CORESESSION_KEY);
       }
       if (session == null) {
           String repoName = getTargetRepositoryName(request);
           RepositoryManager rm = Framework.getService(RepositoryManager.class);
           Repository repo = rm.getRepository(repoName);
           if (repo == null) {
               throw new ClientException("Unable to get " + repoName
                       + " repository");
           }
           session = repo.open();
       }
       if (httpSession != null) {
           httpSession.setAttribute(CORESESSION_KEY, session);
       }
       return session;
   }

   public static String getTargetRepositoryName(HttpServletRequest req) {
       return "default";
   }

   public void cancel(int status) {
       resp.setStatus(status);
       isCanceled = true;
   }

   public void cancel() {
       resp.setStatus(200);
       isCanceled = true;
   }

}
