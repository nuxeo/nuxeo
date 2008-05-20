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

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
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
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.util.FormData;
import org.nuxeo.ecm.webengine.util.JSonHelper;
import org.nuxeo.runtime.api.Framework;
import org.python.core.PyDictionary;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebContext implements WebContext {

    private final static Log log = LogFactory.getLog(WebContext.class);

    public static final String CORESESSION_KEY = "SiteCoreSession";

    public static boolean USE_CORE_SEARCH = false;

    protected final WebEngine engine;
    protected CoreSession session;
    protected boolean isCanceled = false;

    protected WebObject head; // the site root
    protected WebObject tail;
    protected WebObject lastResolved;

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;

    protected String pathInfo;

    protected WebApplication app;
    protected Mapping mapping;
    protected String action; // the current object view
    protected FormData form;

    protected final Map<String,Object> vars; // global vars to share between scripts

    protected List<File> scriptExecutionStack;


    public DefaultWebContext(WebApplication app, HttpServletRequest req, HttpServletResponse resp) {
        this.request = req;
        this.app = app;
        engine = app.getWebEngine();
        this.response = resp;
        vars = new HashMap<String, Object>();
        this.scriptExecutionStack = new ArrayList<File>();
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel() {
        isCanceled = true;
    }

    public void cancel(int errorCode) {
        isCanceled = true;
        response.setStatus(errorCode);
    }

    public String getActionName() {
        return action;
    }

    public Collection<ActionDescriptor> getActions() throws WebException {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getActions() : null;
    }

    public Collection<ActionDescriptor> getActions(String category) throws WebException {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getActions(category) : null;
    }

    public Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getActionsByCategory() : null;
    }

    public DocumentModel getTargetDocument() {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getDocument() : null;
    }

    public Map<String, Object> getEnvironment() {
        return engine.getEnvironment();
    }

    public FormData getForm() {
        if (form == null) {
            form = new FormData(request);
        }
        return form;
    }

    public Mapping getMapping() {
        return mapping;
    }


    public String getUrlPath(DocumentModel document) {
        if (head == null || !head.isResolved()) return null;
        Path rootPath = head.getDocument().getPath().makeAbsolute();
        Path path = document.getPath().makeAbsolute();
        int cnt = path.matchingFirstSegments(rootPath);
        if (cnt == rootPath.segmentCount()) {
            path = path.removeFirstSegments(cnt).makeAbsolute();
            return head.getUrlPath()+path.toString();
        } else {
            return null;
        }
    }

    public String getPathInfo() {
        String path = request.getPathInfo();
        return path == null ? "/" : path;
    }

    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public WebApplication getApplication() {
        return app;
    }

    public WebObject getFirstObject() {
        return head;
    }

    public CoreSession getCoreSession() throws WebException {
        if (session == null) {
            try {
                session = getCoreSession(request);
            } catch (WebException e) {
                throw e;
            } catch (Exception e) {
                throw new WebException("Failed to get core session", e);
            }
        }
        return session;
    }

    public WebObject getTargetObject() {
        return lastResolved;
    }

    public String getTargetObjectUrlPath() {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getUrlPath() : null;
    }

    public ScriptFile getTargetScript() throws IOException {
        File file = null;
        if (mapping != null) {
            return getScriptFile(mapping.getScript());
        } else if (action != null) {
            if (lastResolved != null) {
                file = lastResolved.getActionScript(action);
            }
        }
        if (file != null) {
            return new ScriptFile(file);
        }
        String path = null;
        WebObject first = getFirstUnresolvedObject();
        if (first != null) {
            if (first != tail) {
                path = getPath(first, null);
            } else {
                path = first.getName();
            }
            if (path != null) {
                ScriptFile script = getScriptFile(path);
                if (script != null) {
                    return script;
                }
            }
        }
        path = app.getDefaultPage();
        if (path == null) {
            return null; // by using a null default page we should fallback on a 404
        }
        return getScriptFile(path);
    }

    public ScriptFile getScriptFile(String path) throws IOException {
        File file = getFile(path);
        return file != null ? new ScriptFile(file) : null;
    }

    public File getFile(String path) throws IOException {
        if (path == null || path.length() == 0) return null;
        char c = path.charAt(0);
        if (c == '.') { // local path - use the path stack to resolve it
            File file = getCurrentScriptDirectory();
            if (file != null) {
                // get the file local path - TODO this should be done in ScriptFile?
                file = new File(file, path).getCanonicalFile();
                if (file.isFile()) {
                    return file;
                }
                // try using stacked roots
                String rootPath = engine.getRootDirectory().getAbsolutePath();
                String filePath = file.getAbsolutePath();
                path = filePath.substring(rootPath.length());
            } else {
                log.warn("Relative path used but there is any running script");
                path = new Path(path).makeAbsolute().toString();
            }
        }
        return app.getFile(path);
    }

    public void pushScriptFile(File file) {
        if (scriptExecutionStack.size() > 64) { // stack limit
            throw new IllegalStateException("Script execution stack overflowed. More than 64 calls between scripts");
        }
        if (file == null) {
            throw new IllegalArgumentException("Cannot push a null file");
        }
        scriptExecutionStack.add(file);
    }

    public File popScriptFile() {
        int size = scriptExecutionStack.size();
        if (size == 0) {
            throw new IllegalStateException("Script execution stack underflowed. No script path to pop");
        }
        return scriptExecutionStack.remove(size-1);
    }

    public File getCurrentScriptFile() {
        int size = scriptExecutionStack.size();
        if (size == 0) {
            return null;
        }
        return scriptExecutionStack.get(size-1);
    }

    public File getCurrentScriptDirectory() {
        int size = scriptExecutionStack.size();
        if (size == 0) {
            return null;
        }
        return scriptExecutionStack.get(size-1).getParentFile();
    }


    public String getURI() {
        return request.getRequestURI();
    }

    public String getURL() {
        return request.getRequestURL().toString();
    }

    public String getUrlPath() {
        StringBuilder buf = new StringBuilder(request.getRequestURI().length());
        String path = request.getContextPath();
        if (path == null) {
            path = "/nuxeo/site"; // for testing
        }
        buf.append(path).append(request.getServletPath());
        path = request.getPathInfo();
        if (path != null) {
            buf.append(path);
        }
        return buf.toString();
    }

    public String getApplicationPath() {
        StringBuilder buf = new StringBuilder(request.getRequestURI().length());
        String path = request.getContextPath();
        if (path == null) {
            path = "/nuxeo/site"; // for testing
        }
        buf.append(path).append(request.getServletPath());
        return buf.toString();
    }

    public Object getProperty(String key) {
        return vars.get(key);
    }

    public Object getProperty(String key, Object defaultValue) {
        Object value = vars.get(key);
        return value == null ? defaultValue : value;
    }

    public WebEngine getWebEngine() {
        return engine;
    }

    /**
     * XXX implement this method
     */
    public String makeAbsolutePath(String relPath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Yet Implemented");
        //return null;
    }

    public void print(String text) throws IOException {
        response.getWriter().write(text);
    }

    public void redirect(String url) throws IOException {
        response.sendRedirect(url);
        isCanceled = true;
    }


    public void render(String template) throws WebException {
        render(template, null);
    }

    @SuppressWarnings("unchecked")
    public void render(String template, Object ctx) throws WebException {
        try {
            ScriptFile script = getScriptFile(template);
            if (script != null) {
                render(script, ctx);
            } else {
                throw new WebResourceNotFoundException("Template not found: "+template);
            }
        } catch (IOException e) {
            throw new WebException("Failed to get script file for: "+template);
        }
    }

    @SuppressWarnings("unchecked")
    public void render(ScriptFile script, Object ctx) throws WebException {
        Map map = null;
        if (ctx != null) {
            if (ctx instanceof Map) {
                map = (Map) ctx;
            } else if (ctx instanceof PyDictionary) {
                map = Scripting.convertPythonMap((PyDictionary) ctx);
            }
        }
        try {
            String template = script.getURL();
            Bindings bindings = createBindings(map);
            if (log.isDebugEnabled()) {
                log.debug("## Rendering: "+template);
            }
            pushScriptFile(script.getFile());
            app.getRendering().render(template, bindings, response.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException("Failed to render template: "+script.getAbsolutePath(), e);
        } finally {
            popScriptFile();
        }
    }

    public Object runScript(String script) throws WebException {
        return runScript(script, null);
    }

    public Object runScript(String script, Map<String, Object> args) throws WebException {
        try {
            ScriptFile sf = getScriptFile(script);
            if (sf != null) {
                return runScript(sf, args);
            } else {
                throw new WebResourceNotFoundException("Script not found: "+script);
            }
        } catch (IOException e) {
            throw new WebException("Failed to get script file: "+script, e);
        }
    }

    public Object runScript(ScriptFile script, Map<String, Object> args) throws WebException {
        try {
            pushScriptFile(script.getFile());
            return engine.getScripting().runScript(this, script, createBindings(args));
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException("Failed to run script "+script, e);
        } finally {
            popScriptFile();
        }
    }

    public Object exec(ScriptFile script, Map<String, Object> args) throws WebException {
        String ext = script.getExtension();
        if ("ftl".equals(ext)) {
            render(script, args);
            return null;
        } else {
            return runScript(script, args);
        }
    }

// TODO Work in progress
//    public Object runScript(ScriptFile script, Map<String,Object> args) throws WebException {
//        String ext = script.getExtension();
//        if ("ftl".equals(ext)) {
//            render(script.getFile().getAbsolutePath(), args); //TODO
//        } else {
//            app.getScripting().runScript(this, script, args);
//        }
//    }


    public void setActionName(String name) {
        this.action = name;
    }


    public void setProperty(String key, Object value) {
        vars.put(key, value);
    }


    public WebObject getLastObject() {
        return tail;
    }

    public boolean isRootRequest() {
        return head != null && head.next == null;
    }

    public WebObject getFirstResolvedObject() {
        if (head == null) {
            return null;
        }
        return head.isResolved() ? head : null;
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

    public boolean resolveObject(WebObject object, DocumentModel doc) {
        if (getFirstUnresolvedObject() == object) {
            object.doc = doc;
            lastResolved = object;
            return true;
        }
        return false;
    }

    public List<WebObject> getTraversalPath() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<WebObject> getUnresolvedObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public List<WebObject> getResolvedObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public DocumentModelList search(String query) throws WebException {
        CoreSession session = getCoreSession();
        try {
            if (USE_CORE_SEARCH) {
                return session.query(query);
            } else {
                SearchService search = Framework.getService(SearchService.class);
                if (search == null) {
                    USE_CORE_SEARCH = true;
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
        } catch (Exception e) {
            throw new WebException("Failed to perform search: "+query, e);
        }
    }

    public JSONObject toJSon(DocumentModel doc) throws WebException {
        return toJSon(doc, (String[])null);
    }

    public JSONObject toJSon(DocumentModel doc, String ... schemas) throws WebException {
        return JSonHelper.doc2JSon(doc, schemas);
    }

    //--------------------------------------------------------------------------- TODO internal API

    public Bindings createBindings(Map<String, Object> vars) {
        Bindings bindings = new SimpleBindings();
        if (vars != null) {
            bindings.putAll(vars);
        }
        initDefaultBindings(bindings);
        return bindings;
    }

    protected void initDefaultBindings(Bindings bindings) {
        bindings.put("Context", this);
        bindings.put("Request", request);
        bindings.put("Response", response);
        bindings.put("This", getTargetObject());
        bindings.put("Root", getFirstResolvedObject());
        bindings.put("Document", getTargetDocument());
        bindings.put("Engine", engine);
        try {
            bindings.put("Session", getCoreSession());
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
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

   /**
    * XXX should be this made part of the API? or may be createa WebContexFactory ..
    * @param name
    * @param doc
    * @return
    */
   public WebObject addWebObject(String name, DocumentModel doc) {
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
   /**
    * XXX remove this method and pass mapping through ctor?
    * @param mapping
    */
   public void setMapping(Mapping mapping) {
       this.mapping = mapping;
   }

   /**
    * XXX this is a shortcut metod we need to remove
    * @return
    */
   public String getFirstUnresolvedSegment() {
       WebObject obj = getFirstUnresolvedObject();
       return obj != null ? obj .getName() : null;
   }


   @Override
   public String toString() {
       return "Resolved Path: " + getResolvedPath() + "; Unresolved Path:" + getUnresolvedPath()
               + "; Action: " + action + "; Mapping: " + (mapping == null ? "none" : mapping.getScript());
   }



    public static CoreSession getCoreSession(HttpServletRequest request)
    throws Exception {

//      for testing
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
