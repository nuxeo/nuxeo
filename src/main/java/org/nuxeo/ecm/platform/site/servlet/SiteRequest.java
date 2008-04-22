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

package org.nuxeo.ecm.platform.site.servlet;

import java.util.ArrayList;
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
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.mapping.Mapping;
import org.nuxeo.ecm.platform.site.template.ScriptFile;
import org.nuxeo.ecm.platform.site.template.SiteManager;
import org.nuxeo.ecm.platform.site.template.SiteRoot;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRequest extends HttpServletRequestWrapper implements SiteConst {

    public static final String CORESESSION_KEY = "SiteCoreSession";

    protected SiteManager siteManager;
    protected CoreSession session;
    protected boolean isRenderingCanceled = false;
    protected String mode = VIEW_MODE;

    protected SiteObject head; // the site root
    protected SiteObject tail;
    protected SiteObject lastResolved;

    protected HttpServletResponse resp;

    protected String pathInfo;

    protected SiteRoot siteRoot;
    protected ScriptFile script;
    protected Mapping mapping;

    public SiteRequest(SiteRoot root, HttpServletRequest req, HttpServletResponse resp) {
        super (req);
        this.siteRoot = root;
        this.siteManager = root.getSiteManager();
        String requestedMode = getParameter(MODE_KEY);
        if (requestedMode != null) {
            this.mode = requestedMode;
        }
        this.resp = resp;
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

    public ScriptFile getScript() {
        if (script == null) {
            String path = null;
            SiteObject first = getFirstUnresolvedObject();
          if (first != null) {
              if (first != tail) {
                  path = getPath(first, null);
              } else {
                  path = first.getName();
              }
          } else {
              path = "view.ftl";
          }
          script  = siteRoot.getScript(path, lastResolved == null ? null : lastResolved.getDocument().getType());
        }
        return script;
    }

    public void setScript(String path) {
        String type = (lastResolved != null) ? lastResolved.getDocument().getType() : null;
        this.script = siteRoot.getScript(path, type);
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    public SiteObject getSiteRoot() {
        return head;
    }

    public HttpServletResponse getResponse() {
        return resp;
    }

    public HttpServletRequest getRequest() {
        return (HttpServletRequest) super.getRequest();
    }

    public CoreSession getCoreSession() throws Exception {
        if (session == null) {
            session = getCoreSession(this);
        }
        return session;
    }

    public boolean isRenderingCanceled() {
        return isRenderingCanceled;
    }

    public void cancelRendering() {
        isRenderingCanceled = true;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public SiteObject getLastResolvedObject() {
        return lastResolved;
    }

    public SiteObject getFirstUnresolvedObject() {
        return lastResolved == null ? head : lastResolved.next;
    }

    public boolean hasUnresolvedObjects() {
        return lastResolved != tail;
    }

    public SiteObject addSiteObject(String name, DocumentModel doc) {
        SiteObject object = new SiteObject(this, name, doc);
        if (head == null) {
            this.head = this.tail = object;
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

    public List<SiteObject> getSiteObjects() {
        ArrayList<SiteObject> objects = new ArrayList<SiteObject>();
        SiteObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<SiteObject> getUnresolvedSiteObjects() {
        ArrayList<SiteObject> objects = new ArrayList<SiteObject>();
        SiteObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<SiteObject> getResolvedSiteObjects() {
        ArrayList<SiteObject> objects = new ArrayList<SiteObject>();
        SiteObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public SiteObject getLastObject() {
        return tail;
    }

    public boolean isRootRequest() {
        return head != null && head.next == null;
    }

    public SiteRoot getRoot() {
        return siteRoot;
    }


    /**
    *
    * @return the last traversed object
    */
   public SiteObject traverse() throws SiteException {
       if (head == null || lastResolved == null) {
           return null;
       }
       SiteAwareObject adapter = head.getAdapter();
       if (adapter == null || !adapter.traverse(this, resp)) {
           return null;
       }
       SiteObject lastTraversed = head;
       SiteObject p = head.next;
       while (p != lastResolved.next) {
           adapter = p.getAdapter();
           if (adapter == null || !adapter.traverse(this, resp)) {
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
   public String getPath(SiteObject start, SiteObject end) {
       if (start == null || start == end) return "";
       StringBuilder buf = new StringBuilder(256);
       SiteObject p = start;
       while (p != end) {
           buf.append("/").append(p.name);
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
       if (lastResolved == null) return "";
       return getPath(head, lastResolved.next);
   }

   public String toString() {
       return "Resolved Path: "+getResolvedPath()+"; Unresolved Path:"+getUnresolvedPath()+"; Script: "+(script == null? "none" : script.getPath());
   }

   public void render(String template) throws Exception {
       render(template, null);
   }

   public void render(String template, Map<String,Object> ctx) throws Exception {
       if (lastResolved != null) {
           siteManager.getScripting().getRenderingEngine().render(template, lastResolved);
       } else {
           throw new SiteException("Rendering outside doc context not impl yet");
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


}

