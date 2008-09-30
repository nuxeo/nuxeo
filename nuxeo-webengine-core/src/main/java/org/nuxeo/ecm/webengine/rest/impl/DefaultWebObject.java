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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.NoSuchResourceException;
import org.nuxeo.ecm.webengine.rest.model.WebAction;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebResource;
import org.nuxeo.ecm.webengine.rest.model.WebType;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebObject extends AbstractWebResource<WebType> implements WebObject {


    public DefaultWebObject(WebType type) {
        super (type);
    }
    
    public boolean isAction() {
        return false;
    }
    
    public boolean isObject() {
        return true;
    }

    @Path(value="{segment}")
    public Object dispatch(@PathParam("segment") String segment) throws WebException {
      System.out.println(">>>>>>>>>>>> "+segment);
      Object result = null; 
      if (segment.startsWith("@")) {
          System.out.println(">>>>>>>>>>>> "+segment+" - dispatch action");
          result = resolveAction(segment.substring(1));    
      } else {
          System.out.println(">>>>>>>>>>>> "+segment+" - dispatch object");
          result = resolveObject(segment);
      }
      if (result == null) {
          throw new NoSuchResourceException("No Such object "+segment);
      }
      return result;
    }
    
    protected WebAction resolveAction(String actionName) throws WebException {
        return newAction(actionName);
    }
    
    protected WebObject resolveObject(String segment) throws WebException {
        return null;
    }
    

    @Path(value="testPath")
    public WebResource testInheritedPath() throws WebException {
        return newObject(getType().getName(), "testPath");
    }

    
    @GET
    public String testInheritedGET() {
        return "testInheritedGet";
    }

    @GET
    @Path(value="testGetPath")
    public String testInheritedGETWithPath(@Context UriInfo info) {
        System.out.println("uinfo: "+info);
        return "testInheritedGetWithPath";
    }


    public void setContext(WebContext2 ctx) {
        this.ctx = ctx;
    }
    
    public WebContext2 getContext() {
        return ctx;
    }

    /**
     * @return the path.
     */
    public String getPath() {
        return path;
    }

    public WebType getType() {
        return type;
    }

    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }
    



    public ScriptFile getActionScript(String action) {
        WebApplication app = ctx.getApplication();
        StringBuilder path = new StringBuilder();
        path.append('/').append(getType().getName()).append('/')
            .append(action).append('.').append(app.getScriptExtension());
        try {
            return app.getFile(path.toString());
        } catch (IOException e) {
            return null;
        }
    }

    public ScriptFile getTemplate(String action) {
        return getTemplateScript(action, null);
    }

    public ScriptFile getTemplateScript(String action, String format) {
        WebApplication app = ctx.getApplication();
        StringBuilder path = new StringBuilder();
        path.append('/').append(getType().getName()).append('/')
            .append(action).append('.');
        if (format != null) {
          path.append(format).append('.');
        }
        path.append(app.getTemplateExtension());
        try {
            return app.getFile(path.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public ActionDescriptor getAction(String action) {
        return type.getAction(action); 
    }

    public ActionDescriptor[] getActions() {
        return type.getActions(); 
    }

    public ActionDescriptor[] getActions(String category) {
        return type.getActions(category);
    }

    public Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
        return null; //TODO
    }


}
