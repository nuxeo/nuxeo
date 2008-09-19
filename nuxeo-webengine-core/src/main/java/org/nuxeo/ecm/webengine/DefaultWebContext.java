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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.ecm.webengine.util.JSonHelper;
import org.python.core.PyDictionary;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebContext implements WebContext {

    private static final Log log = LogFactory.getLog(WebContext.class);

    protected final WebEngine engine;
    protected CoreSession session;
    protected boolean isCanceled = false;

    protected WebObject head; // the site root
    protected WebObject tail;

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;

    protected final PathInfo pathInfo;

    protected final WebApplication app;
    protected FormData form;

    protected final Map<String,Object> vars; // global vars to share between scripts

    protected final List<File> scriptExecutionStack;

    protected String basePath;
    protected String applicationPath;
    private boolean isInitialized;

    // used to pass vars between client and server
    protected HashMap<String, Object> clientVars;


    public DefaultWebContext(WebApplication app, PathInfo pathInfo,
            HttpServletRequest req, HttpServletResponse resp) {
        this.pathInfo = pathInfo;
        request = req;
        this.app = app;
        engine = app.getWebEngine();
        response = resp;
        vars = new HashMap<String, Object>();
        scriptExecutionStack = new ArrayList<File>();
    }

    public void initialize() throws WebException {
        if (isInitialized) {
            throw new IllegalStateException("Context already initialized");
        }
        isInitialized = true;
        if (request.getParameter("context") == null) { // if in top level context initialize client URL path
            setClientUrlPath(getUrlPath());
        }
        DocumentRef documentRoot = pathInfo.getDocument();
        if (documentRoot == null) { //TODO XXX: here we can add a custom root resolver
            pathInfo.setTrailingPath(pathInfo.getTraversalPath());
            return;
        }
        CoreSession session = getCoreSession();
        DocumentModel doc;
        try {
            doc = session.getDocument(documentRoot);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        addWebObject(doc.getName(), doc);
        Path traversalPath = pathInfo.getTraversalPath();

        for (int i=0, len=traversalPath.segmentCount(); i<len; i++) {
            String name = traversalPath.segment(i);
            doc = getLastObject().traverse(name); // get next object if any
            if (doc != null) {
                addWebObject(name, doc);
            } else if (i == 0) {
                pathInfo.setTrailingPath(traversalPath);
                break;
            } else {
                pathInfo.setTrailingPath(traversalPath.removeFirstSegments(i));
                break;
            }
        }
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

    public String getApplicationPath() {
        if (applicationPath == null) {
            applicationPath = new StringBuilder(256).append(getBasePath())
                .append(pathInfo.getApplicationPath().toString()).toString();
        }
        return applicationPath;
    }

    public String getUrlPath(DocumentModel document) {
        if (head != null) {
            String appPath = getApplicationPath();
            StringBuilder buf = new StringBuilder(512);
            // resolve the document relative to the root
            Path docPath = document.getPath().makeAbsolute();
            Path path = null;
            path = getRelativePath(head.getDocument().getPath().makeAbsolute(), docPath);
            if (path != null) {
                buf.append(appPath);
            } else {
                buf.append(getBasePath());
                path = engine.getUrlPath(docPath);
            }
            if (path == null) {
                path = docPath;
            }
            if (path.segmentCount() > 0) {
                buf.append(path.removeTrailingSeparator().toString());
            }
            return buf.toString();
        }
        return null;
    }

    public PathInfo getPathInfo() {
        return pathInfo;
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

    public UserSession getUserSession() {
        return UserSession.getCurrentSession(request.getSession(true));
    }

    public CoreSession getCoreSession() throws WebException {
        if (session == null) {
            try {
                UserSession us = getUserSession();
                if (us != null) {
                    session = us.getCoreSession();
                } else {
                    session = UserSession.openSession(app.getRepositoryName());
                }
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
        }
        return session;
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public WebObject getTargetObject() {
        return tail;
    }

    public String getTargetObjectUrlPath() {
        WebObject obj = getTargetObject();
        return obj != null ? obj.getUrlPath() : null;
    }


    public ScriptFile getTargetScript() throws IOException {
        ScriptFile script = computeTargetScript();
        if (script == null && tail != null) { // show default page only when the target document exists
            script = getFile(app.getDefaultPage());
        }
        return script;
    }

    protected ScriptFile computeTargetScript() throws IOException {
        String targetScriptPath = pathInfo.getScript();
        if (targetScriptPath != null) { // custom script
            return getFile(targetScriptPath);
        } else if (tail == null) { // there is no traversal path
            if (pathInfo.hasTrailingPath()) { // use the trail path
                Path trailingPath = pathInfo.getTrailingPath();
                String path = trailingPath.toString();
                ScriptFile script = getFile(path);
                if (script != null) {
                    return script;
                }
            } else { // use the index page
                String path = app.getIndexPage();
                ScriptFile script = getFile(path);
                if (script != null) {
                    return script;
                }
            }
        } else if (pathInfo.getAction() != null) { // there is a contextual object and an action
            return tail.getActionScript(pathInfo.getAction());
        } else if (pathInfo.hasTrailingPath()) { // there is no action - use the trailing path
            Path trailingPath = pathInfo.getTrailingPath();
            String path = trailingPath.toString();
            ScriptFile script = getFile(path);
            if (script != null) {
                return script;
            }
        }
        return null;
    }

    public ScriptFile getFile(String path) throws IOException {
        if (path == null || path.length() == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // local path - use the path stack to resolve it
            File file = getCurrentScriptDirectory();
            if (file != null) {
                // get the file local path - TODO this should be done in ScriptFile?
                file = new File(file, path).getCanonicalFile();
                if (file.isFile()) {
                    return new ScriptFile(file);
                }
                // try using stacked roots
                String rootPath = engine.getRootDirectory().getAbsolutePath();
                String filePath = file.getAbsolutePath();
                path = filePath.substring(rootPath.length());
            } else {
                log.warn("Relative path used but there is any running script");
                path = new Path(path).makeAbsolute().toString();
            }
        }  else if (c == '@' && tail != null && path.length() > 2 && path.charAt(1) == '@') {
            // workaround to support action references
            // an action shortcut
            ScriptFile script = tail.getActionScript(path.substring(2));
            if (script != null) {
                return script;
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

    public String getBasePath() {
        if (basePath == null) {
            StringBuilder buf = new StringBuilder(request.getRequestURI().length());
            String path = request.getContextPath();
            if (path == null) {
                path = "/nuxeo/site"; // for testing
            }
            buf.append(path).append(request.getServletPath());
            int len = buf.length();
            if (len > 0 && buf.charAt(len-1) == '/') {
                buf.setLength(len-1);
            }
            basePath = buf.toString();
        }
        return basePath;
    }

    public String getBaseURL() {
        StringBuffer sb = request.getRequestURL();
        int p = sb.indexOf(getBasePath());
        if (p > -1) {
            return sb.substring(0, p);
        }
        return sb.toString();
    }

    public Object getProperty(String key) {
        Object value = pathInfo.getAttributes().getValue(key);
        if (value == null) {
            value = request.getParameter(key);
            if (value == null) {
                value =  vars.get(key);
            }
        }
        return value;
    }

    public Object getProperty(String key, Object defaultValue) {
        Object value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    public String getCookie(String name) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cooky : cookies) {
            if (name.equals(cooky.getName())) {
                return cooky.getValue();
            }
        }
        return null;
    }

    public String getCookie(String name, String defaultValue) {
        String value = getCookie(name);
        return value == null ? defaultValue : value;
    }

    public void setCookie(String name, String value) {
        response.addCookie(new Cookie(name, value));
    }

    public WebEngine getWebEngine() {
        return engine;
    }

    public void print(String text) throws IOException {
        response.getWriter().write(text);
    }

    public Log getLog() {
        return DefaultWebContext.log;
    }

    public void redirect(String url) throws IOException {
        response.sendRedirect(url);
        isCanceled = true;
    }

    public void render(String template) throws WebException {
        render(template, null);
    }

    public void render(String template, Object ctx) throws WebException {
        try {
            ScriptFile script = getFile(template);
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
            ScriptFile sf = getFile(script);
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

    public void setProperty(String key, Object value) {
        vars.put(key, value);
    }

    public WebObject getLastObject() {
        return tail;
    }

    public boolean hasTraversalPath() {
        return head != null;
    }

    public void removeLastTraversalObject() {
        if (head != null) {
            tail.prev.next = null;
            tail = tail.prev;
        }
    }

    public List<WebObject> getTraversalObjects() {
        ArrayList<WebObject> objects = new ArrayList<WebObject>();
        WebObject p = head;
        while (p != null) {
            objects.add(p);
            p = p.next;
        }
        return objects;
    }

    public JSONObject toJSon(DocumentModel doc) throws WebException {
        return toJSon(doc, (String[])null);
    }

    public JSONObject toJSon(DocumentModel doc, String ... schemas) throws WebException {
        return JSonHelper.doc2JSon(doc, schemas);
    }

    public void resolveFirstUnresolvedSegment(DocumentModel doc) {
        if (pathInfo.hasTrailingPath()) {
            Path trailingPath = pathInfo.getTrailingPath();
            String name = trailingPath.lastSegment();
            trailingPath = trailingPath.removeLastSegments(1);
            pathInfo.setTrailingPath(trailingPath);
            addWebObject(name, doc);
        }
    }

    /**
     * XXX this is a shortcut method we need to remove
     * @return
     */
    public String getFirstUnresolvedSegment() {
        if (pathInfo.hasTrailingPath()) {
            return pathInfo.getTrailingPath().segment(0);
        }
        return null;
    }

    /**
     * We must support at least: CoreSession, DocumentModel, Principal.
     */
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DocumentModel.class) {
            return adapter.cast(getTargetDocument());
        } else if (adapter == CoreSession.class) {
            try {
            return adapter.cast(getCoreSession());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (adapter == Principal.class) {
            return adapter.cast(request.getUserPrincipal());
        }
        return null;
    }

    public void setClientUrlPath(String path) {
        request.getSession(true).setAttribute("clientUrlPath", path);
    }

    public String getClientUrlPath() {
        String path = (String)request.getSession(true).getAttribute("clientUrlPath");
        if (path == null) {
            path = pathInfo.path.toString();
        }
        return path;
    }

    public String getClientContext() {
        try {
            return request.getParameter("context");
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    public Object getClientVariable(String key) {
        Object value = null;
        if (clientVars != null) {
          value = clientVars.get(key);
        } else {
            clientVars = new HashMap<String,Object>();
        }
        if (value == null) {
            String str = getCookie("vars."+key);
            if (str == null) {
                return null;
            } else if (str.length() > 1) {
                char c = str.charAt(0);
                if (c == '{' || c == '[') {
                    value = JSONSerializer.toJSON(str);
                } else {
                    value = str;
                }
            }
            clientVars.put(key, value);
        }
        return value;
    }

    public Object getClientVariable(String key, Object defaultValue) {
        Object value = getClientVariable(key);
        return value == null ? defaultValue : value;
    }

    public void setClientVariable(String key, Object value) {
        if (clientVars == null) {
            clientVars = new HashMap<String, Object>();
        }
        clientVars.put(key, value);
        // TODO set cookie lazy when response is sent to the client
        if (value == null) {
            setCookie("vars."+key, null);
        } else if (value.getClass() == String.class) {
            setCookie("vars."+key, (String)value);
        } else if (value instanceof Number || value instanceof Boolean || value instanceof CharSequence) {
            setCookie("vars."+key, value.toString());
        } else {
            setCookie("vars."+key, JSONSerializer.toJSON(value).toString());
        }
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
        bindings.put("Root", getFirstObject());
        bindings.put("Document", getTargetDocument());
        bindings.put("Engine", engine);
        bindings.put("basePath", getBasePath());
        bindings.put("appPath", getApplicationPath());
        try {
            bindings.put("Session", getCoreSession());
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
    }

   /**
     * XXX should be this made part of the API? or maybe create
     * WebContexFactory ..
     *
     * @param name
     * @param doc
     * @return
     */
    public WebObject addWebObject(String name, DocumentModel doc) {
        if (name == null) {
            name = ""; // this happens for the root node
        }
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
        return object;
    }

    @Override
    public String toString() {
        return "PathInfo: " + pathInfo.toString();
    }

    /**
     * Given an absolute path, returns its relative path to the given base path
     * if any.
     * <p>
     * If the path cannot be made relative to the basePath, then null is
     * returned.
     * <p>
     * The returned path is always starting with a '/'.
     *
     * @param basePath the base path
     * @param path the path
     * @return the relative path, or null if the relative path cannot be computed
     */
    public static Path getRelativePath(Path basePath, Path path) {
        int cnt = basePath.matchingFirstSegments(path);
        if (cnt == basePath.segmentCount()) {
            return path.removeFirstSegments(cnt).makeAbsolute();
        }
        return null;
    }

}
