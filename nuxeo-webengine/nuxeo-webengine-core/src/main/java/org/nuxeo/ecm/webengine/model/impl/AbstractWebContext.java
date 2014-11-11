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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.Writer;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.AdapterResource;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.Messages;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractWebContext implements WebContext {

    protected static final Log log = LogFactory.getLog(WebContext.class);

    protected final WebEngine engine;
    protected final UserSession us;
    protected final LinkedList<File> scriptExecutionStack;
    protected final HttpServletRequest request;
    protected final Map<String, Object> vars;
    protected AbstractResource<?> head;
    protected AbstractResource<?> tail;
    protected AbstractResource<?> root;
    protected Module module;
    protected FormData form;
    protected String basePath;

    protected AbstractWebContext(HttpServletRequest request) {
        us = UserSession.getCurrentSession(request);
        engine = Framework.getLocalService(WebEngine.class);
        scriptExecutionStack = new LinkedList<File>();
        this.request = request;
        vars = new HashMap<String, Object>();
    }

//    public abstract HttpServletRequest getHttpServletRequest();
//    public abstract HttpServletResponse getHttpServletResponse();


    public Resource getRoot() {
        return root;
    }

    public <T> T getAdapter(Class<T> adapter) {
        if (Principal.class == adapter) {
            return adapter.cast(getPrincipal());
        } else if (Resource.class == adapter) {
            return adapter.cast(tail());
        } else if (WebContext.class == adapter) {
            return adapter.cast(this);
        } else if (Module.class == adapter) {
            return adapter.cast(module);
        } else if (WebEngine.class == adapter) {
            return adapter.cast(engine);
        }
        return null;
    }

    public Module getModule() {
        return module;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public UserSession getUserSession() {
        return us;
    }

    public CoreSession getCoreSession() {
        return us.getCoreSession();
    }

    public Principal getPrincipal() {
        return us.getPrincipal();
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getModulePath() {
        return head.getPath();
    }

    public String getMessage(String key) {
        Messages messages = module.getMessages();
        try {
            return messages.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getMessage(String key, String ... args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key);
            if (args != null && args.length > 0) { // format the string using given args
                msg = MessageFormat.format(msg, (Object[]) args);
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getMessageL(String key, String language) {
        Messages messages = module.getMessages();
        try {
            return messages.getString(key, language);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getMessageL(String key, String language, String ... args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key, language);
            if (args != null && args.length > 0) { // format the string using given args
                msg = MessageFormat.format(msg, (Object[]) args);
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public Resource newObject(String typeName, Object ...  args) {
        ResourceType type = module.getType(typeName);
        if (type == null) {
            throw new WebResourceNotFoundException("No Such Object Type: "+typeName);
        }
        return newObject(type, args);
    }

    public Resource newObject(ResourceType type, Object ...  args) {
        Resource obj = type.newInstance();
        obj.initialize(this, type, args);
        push(obj);
        return obj;
    }

    public AdapterResource newAdapter(Resource ctx, String serviceName, Object ...  args) {
        AdapterType st = module.getAdapter(ctx, serviceName);
        AdapterResource service = (AdapterResource)st.newInstance();
        service.initialize(this, st, args);
        push(service);
        return service;
    }

    public void setProperty(String key, Object value) {
        vars.put(key, value);
    }


    //TODO: use FormData to get query params?
    public Object getProperty(String key) {
        Object value = getUriInfo().getPathParameters().getFirst(key);
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

    public FormData getForm() {
        if (form == null) {
            form = new FormData(request);
        }
        return form;
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

    public StringBuilder getServerURL() {
        StringBuilder buf = new StringBuilder();
        String scheme = request.getScheme ();
        int port = request.getServerPort ();
        String urlPath = request.getRequestURI();
        if (urlPath.length() == 0) {
            urlPath = "/";
        }
        buf.append(scheme).append("://").append(request.getServerName());
        if ("http".equals(scheme) && port != 80 || "https".equals(scheme) && port != 443) {
            buf.append(':');
            buf.append(request.getServerPort());
        }
        return buf;
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


    public String getUrlPath(DocumentModel document) {
        return getModuleInstance().getLink(document);
    }

    public Log getLog() {
        return log;
    }

    /** object stack API */

    public ModuleResource getModuleInstance() {
        return (ModuleResource)head;
    }


    public Resource push(Resource obj) {
        AbstractResource<?> rs = (AbstractResource<?>)obj;
        if (tail != null) {
            tail.next = rs;
            rs.prev = tail;
            tail = rs;
        } else {
            module = obj.getModule();
            rs.prev = tail;
            head = tail = rs;
        }
        return obj;
    }

    public Resource pop() {
        if (tail == null) {
            return null;
        }
        AbstractResource<?> rs = tail;
        if (tail == head) {
            head = tail = null;
        } else {
            tail = rs.prev;
            tail.next = null;
        }
        rs.dispose();
        return rs;
    }

    public Resource tail() {
        return tail;
    }

    public Resource head() {
        return head;
    }

    /** template and script resolver */

    public ScriptFile getFile(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // local path - use the path stack to resolve it
            File file = getCurrentScriptDirectory();
            if (file != null) {
                try {
                    // get the file local path - TODO this should be done in ScriptFile?
                    file = new File(file, path).getCanonicalFile();
                    if (file.isFile()) {
                        return new ScriptFile(file);
                    }
                } catch (Exception e) {
                    throw WebException.wrap(e);
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
        return module.getFile(path);
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

    /** running scripts and rendering templates */

    public void render(String template, Writer writer) {
        render(template, null, writer);
    }

    public void render(String template, Object ctx, Writer writer) {
        ScriptFile script = getFile(template);
        if (script != null) {
            render(script, ctx, writer);
        } else {
            throw new WebResourceNotFoundException("Template not found: "+template);
        }
    }

    @SuppressWarnings("unchecked")
    public void render(ScriptFile script, Object ctx, Writer writer) {
        Map map = null;
        if (ctx instanceof Map) {
            map = (Map) ctx;
        }
        try {
            String template = script.getURL();
            Bindings bindings = createBindings(map);
            if (log.isDebugEnabled()) {
                log.debug("## Rendering: "+template);
            }
            pushScriptFile(script.getFile());
            engine.getRendering().render(template, bindings, writer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException("Failed to render template: "
                    + (script == null ? script : script.getAbsolutePath()), e);
        } finally {
            if (!scriptExecutionStack.isEmpty()) {
                popScriptFile();
            }
        }
    }

    public Object runScript(String script) {
        return runScript(script, null);
    }

    public Object runScript(String script, Map<String, Object> args) {
        ScriptFile sf = getFile(script);
        if (sf != null) {
            return runScript(sf, args);
        } else {
            throw new WebResourceNotFoundException("Script not found: "+script);
        }
    }

    public Object runScript(ScriptFile script, Map<String, Object> args) {
        try {
            pushScriptFile(script.getFile());
            return engine.getScripting().runScript(script, createBindings(args));
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException("Failed to run script "+script, e);
        } finally {
            if (!scriptExecutionStack.isEmpty()) {
                popScriptFile();
            }
        }
    }

    public Bindings createBindings(Map<String, Object> vars) {
        Bindings bindings = new SimpleBindings();
        if (vars != null) {
            bindings.putAll(vars);
        }
        initializeBindings(bindings);
        return bindings;
    }

    public Resource getTargetObject() {
        Resource t = tail;
        while (t != null) {
            if (!t.isAdapter()) {
                return t;
            }
            t = t.getPrevious();
        }
        return null;
    }

    public AdapterResource getTargetAdapter() {
        Resource t = tail;
        while (t != null) {
            if (t.isAdapter()) {
                return (AdapterResource)t;
            }
            t = t.getPrevious();
        }
        return null;
    }

    protected void initializeBindings(Bindings bindings) {
        Resource obj = getTargetObject();
        bindings.put("Context", this);
        bindings.put("Module", module);
        bindings.put("Engine", engine);
        bindings.put("basePath", getBasePath());
        bindings.put("skinPath", module.getSkinPathPrefix());
        bindings.put("Root", getRoot());
        if (obj != null) {
            bindings.put("This", obj);
            DocumentModel doc = obj.getAdapter(DocumentModel.class);
            if (doc != null) {
                bindings.put("Document", doc);
            }
            Resource adapter = getTargetAdapter();
            if (adapter != null) {
                bindings.put("Adapter", adapter);
            }
        }
        try {
            bindings.put("Session", getCoreSession());
        } catch (Exception e) {
            throw WebException.wrap("Failed to get a core session", e);
        }
    }

}
