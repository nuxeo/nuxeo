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

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.WebAction;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebResource;
import org.nuxeo.ecm.webengine.rest.model.WebResourceType;
import org.nuxeo.ecm.webengine.rest.model.WebView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// DO NOT MODIFY class declaration! Cannot use WebResourceType<?> since groovy doesn't supports wildcards for now
@SuppressWarnings("unchecked")
public abstract class AbstractWebResource<T extends WebResourceType> implements WebResource {

    protected WebContext2 ctx;
    protected AbstractWebResource<?> next;
    protected AbstractWebResource<?> prev;
    protected String path;
    protected T type;

  
    public AbstractWebResource(T type) {
        this.type = type;
    }
    
    public WebResource initialize(WebContext2 ctx, String path) {
        this.ctx = ctx;
        this.path = path;
        System.out.println("@@@ RES: "+getClass()+" >> ancestors res>> "+ctx.getUriInfo().getMatchedResources());
        System.out.println("@@@ RES: "+getClass()+" >> ancestors uris>> "+ctx.getUriInfo().getMatchedURIs());
        return this;
    }

    public T getType() {
        return type;
    }
    
    public WebContext2 getContext() {
        return ctx;
    }
    
    public WebApplication getApplication() {
        return ctx.getApplication();
    }
    

    public WebResource getNext() {
        return next;
    }

    public WebResource getPrevious() {
        return prev;
    }

    public String getPath() {
        return path;
    }


    public WebView getView() {
//        String method = ctx.getMethod().toLowerCase();
//        ScriptFile file = ctx.getApplication().getFile(method+getType().getName()+'.'+getApplication().getTemplateExtension());
//        return new DefaultWebView(file, args);
        return null;
    }

    public <A> A getAdapter(Class<A> adapter) {
        if (adapter == WebContext2.class) {
            return adapter.cast(ctx);
        }
        if (adapter == WebObject.class) {
            return isObject() ? adapter.cast(this) : null;
        }
        if (adapter == WebAction.class) {
            return isAction() ? adapter.cast(this) : null;
        }
        return null;
    }

    public WebObject newObject(String type, String path) throws WebException {
        return ctx.newObject(type, path);
    }
    
    public WebAction newAction(String name) throws WebException {        
        return ctx.newAction(type.getName(), name);
    }
    
}
