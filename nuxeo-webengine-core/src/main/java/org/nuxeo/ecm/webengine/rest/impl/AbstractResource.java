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
import org.nuxeo.ecm.webengine.rest.model.ActionResource;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.ObjectResource;
import org.nuxeo.ecm.webengine.rest.model.Resource;
import org.nuxeo.ecm.webengine.rest.model.ResourceType;
import org.nuxeo.ecm.webengine.rest.model.WebView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// DO NOT MODIFY class declaration! Cannot use WebResourceType<?> since groovy doesn't supports wildcards for now
@SuppressWarnings("unchecked")
public abstract class AbstractResource<T extends ResourceType> implements Resource {

    protected WebContext2 ctx;
    protected AbstractResource<?> next;
    protected AbstractResource<?> prev;
    protected String path;
    protected T type;

  
    
    public Resource initialize(WebContext2 ctx, ResourceType<?> type) throws WebException {
        this.ctx = ctx;
        this.type = (T)type;
        this.path = ctx.getUriInfo().getMatchedURIs().get(0);
        System.out.println("@@@ RES: "+getClass().getSimpleName()+" >> "+path+" >> "+ctx.getUriInfo().getMatchedResources() + " => "+ctx.getUriInfo().getMatchedURIs());
        return this;
    }
    
    public void dispose() {
        this.ctx = null;
        this.type = null;
        this.path = null;
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
    

    public Resource getNext() {
        return next;
    }

    public Resource getPrevious() {
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
        if (adapter == ObjectResource.class) {
            return isObject() ? adapter.cast(this) : null;
        }
        if (adapter == ActionResource.class) {
            return isAction() ? adapter.cast(this) : null;
        }
        return null;
    }

    
}
