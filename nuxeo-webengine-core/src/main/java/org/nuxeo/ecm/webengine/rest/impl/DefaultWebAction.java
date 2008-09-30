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

import java.util.Set;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.ActionType;
import org.nuxeo.ecm.webengine.rest.model.WebAction;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebResource;
import org.nuxeo.ecm.webengine.rest.model.WebResourceType;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebAction extends AbstractWebResource<ActionType> implements WebAction {


    
    @Override
    public WebResource initialize(WebContext2 ctx, WebResourceType<?> type) throws WebException {
        super.initialize(ctx, type);
        if (!this.type.getGuard().check(ctx)) {
            throw new WebSecurityException("Failed to get action: "+getName()+". Action is not accessible in the current context", getName());
        }
        return this;
    }
    
    public String getName() {
        return type.getName();
    }
    
    public WebObject getTargetObject() {
        return (WebObject)prev;
    }
    
    public boolean isEnabled() {
        return type.isEnabled(ctx);
    }
     
    public Set<String> getCategories() {
        return type.getCategories();
    }
   
    public boolean isAction() {
        return true;
    }
    
    public boolean isObject() {
        return false;
    }
}
