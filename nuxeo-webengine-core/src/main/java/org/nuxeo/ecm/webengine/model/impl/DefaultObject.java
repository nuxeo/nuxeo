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

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.ActionResource;
import org.nuxeo.ecm.webengine.model.NoSuchResourceException;
import org.nuxeo.ecm.webengine.model.ObjectResource;
import org.nuxeo.ecm.webengine.model.ObjectType;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebView;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultObject extends AbstractResource<ObjectType> implements ObjectResource {


    public DefaultObject() {
    }
    
    public boolean isAction() {
        return false;
    }
    
    public boolean isObject() {
        return true;
    }

    
    @Path(value=".{segment}")
    public ActionResource disptachAction(@PathParam("segment") String actionName) throws WebException {
        return ctx.newAction(type, actionName);
    }    


    public void setContext(WebContext ctx) {
        this.ctx = ctx;
    }
    
    public WebContext getContext() {
        return ctx;
    }

    /**
     * @return the path.
     */
    public String getPath() {
        return path;
    }

    public ObjectType getType() {
        return type;
    }

    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }


    public ActionTypeImpl getAction(String action) {
        return type.getAction(action); 
    }

    public ActionTypeImpl[] getActions() {
        return type.getActions(); 
    }

    public ActionTypeImpl[] getActions(String category) {
        return type.getActions(category);
    }

    public Map<String, Collection<ActionTypeImpl>> getActionsByCategory() throws WebException {
        return null; //TODO
    }
    
    @GET @POST @PUT @DELETE @HEAD
    public WebView getView() throws WebException {
        return getView(null);
    }
        
    public WebView getView(Map<String,Object> args) throws WebException{
        ScriptFile file = getTemplate();
        if (file == null) {
            throw new NoSuchResourceException("Default template for type: "+type.getName()+" and method "+ctx.getMethod()+" was not found");
        }
        return new WebView(this, file, args);
    }
  
    public ScriptFile getTemplate() {
        return ctx.getProfile().getObjectTemplate(this, null);
    }

    public ScriptFile getTemplate(String name) {
        return ctx.getProfile().getObjectTemplate(this, name);
    }

}
