/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import static org.nuxeo.ecm.webengine.WebEngine.SKIN_PATH_PREFIX_KEY;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.SocketException;
import java.security.Principal;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import javax.script.ScriptException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.login.WebEngineFormAuthenticator;
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
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.ResourceContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractWebContext implements WebContext {

    private static final Log log = LogFactory.getLog(WebContext.class);

    // TODO: this should be made configurable through an extension point
    public static Locale DEFAULT_LOCALE = Locale.ENGLISH;

    public static final String LOCALE_SESSION_KEY = "webengine_locale";

    private static boolean isRepositoryDisabled = false;

    protected final WebEngine engine;

    private UserSession us;

    protected final LinkedList<File> scriptExecutionStack;

    protected final HttpServletRequest request;

    protected final HttpServletResponse response;

    protected final Map<String, Object> vars;

    protected Resource head;

    protected Resource tail;

    protected Resource root;

    protected Module module;

    protected FormData form;

    protected String basePath;

    private String repoName;

    protected AbstractWebContext(HttpServletRequest request, HttpServletResponse response) {
        engine = Framework.getLocalService(WebEngine.class);
        scriptExecutionStack = new LinkedList<File>();
        this.request = request;
        this.response = response;
        vars = new HashMap<String, Object>();
    }

    // public abstract HttpServletRequest getHttpServletRequest();
    // public abstract HttpServletResponse getHttpServletResponse();

    public void setModule(Module module) {
        this.module = module;
    }

    @Override
    public Resource getRoot() {
        return root;
    }

    @Override
    public void setRoot(Resource root) {
        this.root = root;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (CoreSession.class == adapter) {
            return adapter.cast(getCoreSession());
        } else if (Principal.class == adapter) {
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

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public WebEngine getEngine() {
        return engine;
    }

    @Override
    public UserSession getUserSession() {
        if (us == null) {
            us = UserSession.getCurrentSession(request);
        }
        return us;
    }

    @Override
    public CoreSession getCoreSession() {
        if (StringUtils.isNotBlank(repoName)) {
            return SessionFactory.getSession(request, repoName);
        } else {
            return SessionFactory.getSession(request);
        }
    }

    @Override
    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getModulePath() {
        return head.getPath();
    }

    @Override
    public String getMessage(String key) {
        Messages messages = module.getMessages();
        try {
            return messages.getString(key, getLocale().getLanguage());
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public String getMessage(String key, Object... args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key, getLocale().getLanguage());
            if (args != null && args.length > 0) {
                // format the string using given args
                msg = MessageFormat.format(msg, args);
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public String getMessage(String key, List<Object> args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key, getLocale().getLanguage());
            if (args != null && args.size() > 0) {
                // format the string using given args
                msg = MessageFormat.format(msg, args.toArray());
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public String getMessageL(String key, String language) {
        Messages messages = module.getMessages();
        try {
            return messages.getString(key, language);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public String getMessageL(String key, String locale, Object... args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key, locale);
            if (args != null && args.length > 0) {
                // format the string using given args
                msg = MessageFormat.format(msg, args);
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public String getMessageL(String key, String locale, List<Object> args) {
        Messages messages = module.getMessages();
        try {
            String msg = messages.getString(key, locale);
            if (args != null && !args.isEmpty()) {
                // format the string using given args
                msg = MessageFormat.format(msg, args.toArray());
            }
            return msg;
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    @Override
    public Locale getLocale() {
        LocaleProvider localeProvider = Framework.getLocalService(LocaleProvider.class);
        if (localeProvider != null && request.getUserPrincipal() != null) {
            Locale userPrefLocale = localeProvider.getLocale(getCoreSession());
            if (userPrefLocale != null) {
                return userPrefLocale;
            }
        }

        UserSession us = getUserSession();
        if (us != null) {
            Object locale = us.get(LOCALE_SESSION_KEY);
            if (locale instanceof Locale) {
                return (Locale) locale;
            }
        }

        // take the one on request
        Locale locale = request.getLocale();
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    @Override
    public void setLocale(Locale locale) {
        UserSession us = getUserSession();
        if (us != null) {
            us.put(LOCALE_SESSION_KEY, locale);
        }
    }

    @Override
    public Resource newObject(String typeName, Object... args) {
        ResourceType type = module.getType(typeName);
        if (type == null) {
            throw new WebResourceNotFoundException("No Such Object Type: " + typeName);
        }
        return newObject(type, args);
    }

    ResourceContext resources;

    public void setResourceContext(ResourceContext resources) {
        this.resources = resources;
    }

    @Override
    public Resource newObject(ResourceType type, Object... args) {
        Resource obj = type.newInstance(resources);
        try {
            obj.initialize(this, type, args);
        } finally {
            // we must be sure the object is pushed even if an error occurred
            // otherwise we may end up with an empty object stack and we will
            // not be able to
            // handle errors based on objects handleError() method
            push(obj);
        }
        return obj;
    }

    @Override
    public AdapterResource newAdapter(Resource ctx, String serviceName, Object... args) {
        AdapterType st = module.getAdapter(ctx, serviceName);
        AdapterResource service = (AdapterResource) st.newInstance(resources);
        try {
            service.initialize(this, st, args);
        } finally {
            // we must be sure the object is pushed even if an error occurred
            // otherwise we may end up with an empty object stack and we will
            // not be able to
            // handle errors based on objects handleError() method
            push(service);
        }
        return service;
    }

    @Override
    public void setProperty(String key, Object value) {
        vars.put(key, value);
    }

    // TODO: use FormData to get query params?
    @Override
    public Object getProperty(String key) {
        Object value = getUriInfo().getPathParameters().getFirst(key);
        if (value == null) {
            value = request.getParameter(key);
            if (value == null) {
                value = vars.get(key);
            }
        }
        return value;
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public String getCookie(String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public String getCookie(String name, String defaultValue) {
        String value = getCookie(name);
        return value == null ? defaultValue : value;
    }

    @Override
    public FormData getForm() {
        if (form == null) {
            form = new FormData(request);
        }
        return form;
    }

    @Override
    public String getBasePath() {
        if (basePath == null) {
            String webenginePath = request.getHeader(NUXEO_WEBENGINE_BASE_PATH);
            if (",".equals(webenginePath)) {
                // when the parameter is empty, request.getHeader return ',' on
                // apache server.
                webenginePath = "";
            }
            basePath = webenginePath != null ? webenginePath : getDefaultBasePath();
        }
        return basePath;
    }

    private String getDefaultBasePath() {
        StringBuilder buf = new StringBuilder(request.getRequestURI().length());
        String path = request.getContextPath();
        if (path == null) {
            path = "/nuxeo/site"; // for testing
        }
        buf.append(path).append(request.getServletPath());
        if ("/".equals(path)) {
            return "";
        }
        int len = buf.length();
        if (len > 0 && buf.charAt(len - 1) == '/') {
            buf.setLength(len - 1);
        }
        return buf.toString();
    }

    @Override
    public String getBaseURL() {
        StringBuffer sb = request.getRequestURL();
        int p = sb.indexOf(getBasePath());
        if (p > -1) {
            return sb.substring(0, p);
        }
        return sb.toString();
    }

    @Override
    public StringBuilder getServerURL() {
        StringBuilder url = new StringBuilder(VirtualHostHelper.getServerURL(request));
        if (url.toString().endsWith("/")) {
            url.deleteCharAt(url.length() - 1);
        }
        return url;
    }

    @Override
    public String getURI() {
        return request.getRequestURI();
    }

    @Override
    public String getURL() {
        StringBuffer sb = request.getRequestURL();
        if (sb.charAt(sb.length() - 1) == '/') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public StringBuilder getUrlPathBuffer() {
        StringBuilder buf = new StringBuilder(getBasePath());
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            buf.append(pathInfo);
        }
        return buf;
    }

    @Override
    public String getUrlPath() {
        return getUrlPathBuffer().toString();
    }

    @Override
    public String getLoginPath() {
        StringBuilder buf = getUrlPathBuffer();
        int len = buf.length();
        if (len > 0 && buf.charAt(len - 1) == '/') { // remove trailing /
            buf.setLength(len - 1);
        }
        buf.append(WebEngineFormAuthenticator.LOGIN_KEY);
        return buf.toString();
    }

    /**
     * This method is working only for root objects that implement {@link ModuleResource}
     */
    @Override
    public String getUrlPath(DocumentModel document) {
        return ((ModuleResource) head).getLink(document);
    }

    @Override
    public Log getLog() {
        return log;
    }

    /* object stack API */

    @Override
    public Resource push(Resource rs) {
        if (tail != null) {
            tail.setNext(rs);
            rs.setPrevious(tail);
            tail = rs;
        } else {
            rs.setPrevious(tail);
            head = tail = rs;
        }
        return rs;
    }

    @Override
    public Resource pop() {
        if (tail == null) {
            return null;
        }
        Resource rs = tail;
        if (tail == head) {
            head = tail = null;
        } else {
            tail = rs.getPrevious();
            tail.setNext(null);
        }
        rs.dispose();
        return rs;
    }

    @Override
    public Resource tail() {
        return tail;
    }

    @Override
    public Resource head() {
        return head;
    }

    /** template and script resolver */

    @Override
    public ScriptFile getFile(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // local path - use the path stack to resolve it
            File file = getCurrentScriptDirectory();
            if (file != null) {
                try {
                    // get the file local path - TODO this should be done in
                    // ScriptFile?
                    file = new File(file, path).getCanonicalFile();
                    if (file.isFile()) {
                        return new ScriptFile(file);
                    }
                } catch (IOException e) {
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
        return scriptExecutionStack.remove(size - 1);
    }

    public File getCurrentScriptFile() {
        int size = scriptExecutionStack.size();
        if (size == 0) {
            return null;
        }
        return scriptExecutionStack.get(size - 1);
    }

    public File getCurrentScriptDirectory() {
        int size = scriptExecutionStack.size();
        if (size == 0) {
            return null;
        }
        return scriptExecutionStack.get(size - 1).getParentFile();
    }

    /* running scripts and rendering templates */

    @Override
    public void render(String template, Writer writer) {
        render(template, null, writer);
    }

    @Override
    public void render(String template, Object ctx, Writer writer) {
        ScriptFile script = getFile(template);
        if (script != null) {
            render(script, ctx, writer);
        } else {
            throw new WebResourceNotFoundException("Template not found: " + template);
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void render(ScriptFile script, Object ctx, Writer writer) {
        Map map = null;
        if (ctx instanceof Map) {
            map = (Map) ctx;
        }
        try {
            String template = script.getURL();
            Map<String, Object> bindings = createBindings(map);
            if (log.isDebugEnabled()) {
                log.debug("## Rendering: " + template);
            }
            pushScriptFile(script.getFile());
            engine.getRendering().render(template, bindings, writer);
        } catch (IOException | RenderingException e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof SocketException) {
                log.debug("Output socket closed: failed to write response", e);
                return;
            }
            throw WebException.wrap("Failed to render template: "
                    + (script == null ? script : script.getAbsolutePath()), e);
        } finally {
            if (!scriptExecutionStack.isEmpty()) {
                popScriptFile();
            }
        }
    }

    @Override
    public Object runScript(String script) {
        return runScript(script, null);
    }

    @Override
    public Object runScript(String script, Map<String, Object> args) {
        ScriptFile sf = getFile(script);
        if (sf != null) {
            return runScript(sf, args);
        } else {
            throw new WebResourceNotFoundException("Script not found: " + script);
        }
    }

    @Override
    public Object runScript(ScriptFile script, Map<String, Object> args) {
        try {
            pushScriptFile(script.getFile());
            return engine.getScripting().runScript(script, createBindings(args));
        } catch (WebException e) {
            throw e;
        } catch (ScriptException e) {
            throw WebException.wrap("Failed to run script " + script, e);
        } finally {
            if (!scriptExecutionStack.isEmpty()) {
                popScriptFile();
            }
        }
    }

    @Override
    public boolean checkGuard(String guard) throws ParseException {
        return PermissionService.parse(guard).check(this);
    }

    public Map<String, Object> createBindings(Map<String, Object> vars) {
        Map<String, Object> bindings = new HashMap<String, Object>();
        if (vars != null) {
            bindings.putAll(vars);
        }
        initializeBindings(bindings);
        return bindings;
    }

    @Override
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

    @Override
    public AdapterResource getTargetAdapter() {
        Resource t = tail;
        while (t != null) {
            if (t.isAdapter()) {
                return (AdapterResource) t;
            }
            t = t.getPrevious();
        }
        return null;
    }

    protected void initializeBindings(Map<String, Object> bindings) {
        Resource obj = getTargetObject();
        bindings.put("Context", this);
        bindings.put("Module", module);
        bindings.put("Engine", engine);
        bindings.put("Runtime", Framework.getRuntime());
        bindings.put("basePath", getBasePath());
        bindings.put("skinPath", getSkinPathPrefix());
        bindings.put("contextPath", VirtualHostHelper.getContextPathProperty());
        bindings.put("Root", root);
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
        if (!isRepositoryDisabled && getPrincipal() != null) {
            bindings.put("Session", getCoreSession());
        }
    }

    private String getSkinPathPrefix() {
        if (Framework.getProperty(SKIN_PATH_PREFIX_KEY) != null) {
            return module.getSkinPathPrefix();
        }
        String webenginePath = request.getHeader(NUXEO_WEBENGINE_BASE_PATH);
        if (webenginePath == null) {
            return module.getSkinPathPrefix();
        } else {
            return getBasePath() + "/" + module.getName() + "/skin";
        }
    }

    public static boolean isRepositorySupportDisabled() {
        return isRepositoryDisabled;
    }

    /**
     * Can be used by the application to disable injecting repository sessions in scripting context. If the application
     * is not deploying a repository injecting a repository session will throw exceptions each time rendering is used.
     *
     * @param isRepositoryDisabled true to disable repository session injection, false otherwise
     */
    public static void setIsRepositorySupportDisabled(boolean isRepositoryDisabled) {
        AbstractWebContext.isRepositoryDisabled = isRepositoryDisabled;
    }

    @Override
    public void setRepositoryName(String repoName) {
        RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
        if (rm.getRepository(repoName) != null) {
            this.repoName = repoName;
        } else {
            throw new IllegalArgumentException("Repository " + repoName + " not found");
        }

    }
}
