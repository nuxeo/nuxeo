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

package org.nuxeo.ecm.webengine.rest.impl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.LinkedList;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.impl.model.DocumentObject;
import org.nuxeo.ecm.webengine.rest.model.ActionResource;
import org.nuxeo.ecm.webengine.rest.model.MainResource;
import org.nuxeo.ecm.webengine.rest.model.NoSuchResourceException;
import org.nuxeo.ecm.webengine.rest.model.ObjectResource;
import org.nuxeo.ecm.webengine.rest.model.ObjectType;
import org.nuxeo.ecm.webengine.rest.model.Resource;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;
import org.python.core.PyDictionary;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractWebContext implements WebContext2 {

    protected static final Log log = LogFactory.getLog(WebContext2.class);

    protected WebEngine2 engine;
    protected WebApplication app;
    protected UserSession us;
    protected final LinkedList<File> scriptExecutionStack;
    protected AbstractResource<?> head;
    protected AbstractResource<?> tail;
    protected MainResource root;
    protected HttpServletRequest request;

    
    public AbstractWebContext(HttpServletRequest request) {
        this.us = UserSession.getCurrentSession(request.getSession(true));
        this.engine = Framework.getLocalService(WebEngine2.class);
        this.scriptExecutionStack = new LinkedList<File>();
        this.request = request;
    }

//    public abstract HttpServletRequest getHttpServletRequest();
//    public abstract HttpServletResponse getHttpServletResponse();


    public <T> T getAdapter(Class<T> adapter) {
        if (ObjectResource.class == adapter) {
            return (T)tail();
        } else if (WebEngine2.class == adapter) {
            return (T)this;
        }
        return null;
    }
    
    public void setApplication(WebApplication config) {
        this.app = config;
    }

    public WebApplication getApplication() {
        return app;
    }

    public WebEngine2 getEngine() {
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

    public HttpServletRequest getHttpServletRequest() {
        return request;
    }
    
    public String getMethod() {
        return request.getMethod();
    }
    
    public String getApplicationPath() {
        return root.getPath();
    }

    
    public ObjectResource newObject(String typeName) throws WebException {
        ObjectType type = app.getType(typeName);
        if (type == null) {
            throw new NoSuchResourceException("No Such Object Type: "+typeName);
        }
        return newObject(type);
    }

    public ObjectResource newObject(ObjectType type) throws WebException {
        ObjectResource obj = type.newInstance();
        obj.initialize(this, type);
        push(obj);
        return obj;
    }

    public ActionResource newAction(String typeName, String actionName) throws WebException {
        ObjectType type = app.getType(typeName);
        if (type == null) {
            throw new NoSuchResourceException("No Such Object Type: "+typeName);
        }
        return newAction(type, actionName);
    }

    public ActionResource newAction(ObjectResource obj, String actionName) throws WebException {
        return newAction(obj.getType(), actionName);
    }

    public ActionResource newAction(ObjectType type, String actionName) throws WebException {
        ActionTypeImpl actionType = type.getAction(actionName);
        if (actionType == null) {
            throw new NoSuchResourceException("No Such Action: "+actionName);
        }
        ActionResource action = actionType.newInstance();
        action.initialize(this, actionType);
        push(action);
        return action;
    }

   
    /** object stack API */

    public void setRootResource(MainResource root) {
        this.root = root;
    }
    
    public MainResource getRootResource() {
        return root;
    }
    
    
    public Resource push(Resource obj) {
        AbstractResource<?> rs = (AbstractResource<?>)obj;
        if (tail != null) {
            tail.next = rs;
            rs.prev = tail;
            tail = rs;
        } else {
            rs.prev = tail;
            head = tail = rs;
        }
        return obj;
    }

    public Resource pop() {        
        if (tail == null) {
            return null;
        }
        AbstractResource<?> rs = (AbstractResource<?>)tail;
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
                String rootPath = app.getEngine().getRootDirectory().getAbsolutePath();
                String filePath = file.getAbsolutePath();
                path = filePath.substring(rootPath.length());
            } else {
                log.warn("Relative path used but there is any running script");
                path = new Path(path).makeAbsolute().toString();
            }
        }  else if (c == '@' && tail() != null && path.length() > 2 && path.charAt(1) == '@') {
            // workaround to support action references
            // an action shortcut
            //TODO xxxxxx
//            ScriptFile script = tail().getActionScript(path.substring(2));
//            if (script != null) {
//                return script;
//            }
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



    /** running scripts and rendering templates */

    public void render(String template, Writer writer) throws WebException {
        render(template, null, writer);
    }

    public void render(String template, Object ctx, Writer writer) throws WebException {
        try {
            ScriptFile script = getFile(template);
            if (script != null) {
                render(script, ctx, writer);
            } else {
                throw new WebResourceNotFoundException("Template not found: "+template);
            }
        } catch (IOException e) {
            throw new WebException("Failed to get script file for: "+template);
        }
    }

    @SuppressWarnings("unchecked")
    public void render(ScriptFile script, Object ctx, Writer writer) throws WebException {
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
            //TODO =========== fix rendering ============
            app.getEngine().getRendering().render(template, bindings, writer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebException("Failed to render template: "+script.getAbsolutePath(), e);
        } finally {
            if (!scriptExecutionStack.isEmpty()) {
                popScriptFile();
            }
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
            return app.getEngine().getScripting().runScript(script, createBindings(args));
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

    public ObjectResource getTargetObject() {
        if (tail != null) {            
            if (tail.isObject()) {
                return (ObjectResource)tail;
            } else {
                AbstractResource<?> rs = tail.prev;
                while (rs != null) {
                    if (rs.isObject()) {
                        return (ObjectResource)rs;
                    }
                    rs = rs.prev;
                } 
            }
        }
        return null;        
    }
    
    public ActionResource getAction() {
        if (tail != null) {
            return tail.isAction() ? (ActionResource)tail : null;
        }
        return null;
    }
    
    protected void initializeBindings(Bindings bindings) {
        Resource obj = getTargetObject();
        bindings.put("Context", this);
        //TODO uncomment for compatibility
        //bindings.put("Request", request);
        //bindings.put("Response", response);
        if (obj != null) {
            bindings.put("This", obj);
            bindings.put("Root", head());
            if (obj instanceof DocumentObject) {
                bindings.put("Document", ((DocumentObject)obj).getDocument());
            }
        }
        bindings.put("Config", app);
        bindings.put("Engine", app.getEngine());
        //TODO
        //bindings.put("basePath", getBasePath());
        //bindings.put("appPath", getApplicationPath());
        try {
            bindings.put("Session", getCoreSession());
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
    }


    /** deprecated methods used for compatibility */

//    /**
//     * @deprecated use {@link WebObject#getActions()}
//     */
//    Collection<ActionDescriptor> getActions() throws WebException {
//        if (!stack.isEmpty()) {
//            return stack.getLast().getActions();
//        }
//        return null;
//    }
//
//    /**
//     * @deprecated use {@link WebObject#getActions(String)}
//     * @param category
//     * @return
//     * @throws WebException
//     */
//    Collection<ActionDescriptor> getActions(String category) throws WebException {
//        if (!stack.isEmpty()) {
//            return stack.getLast().getActions(category);
//        }
//        return null;
//    }
//
//    /**
//     * @deprecated use {@link WebObject#getActionsByCategory()}
//     * @return
//     * @throws WebException
//     */
//    Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
//        if (!stack.isEmpty()) {
//            return stack.getLast().getActionsByCategory();
//        }
//        return null;
//    }
}
