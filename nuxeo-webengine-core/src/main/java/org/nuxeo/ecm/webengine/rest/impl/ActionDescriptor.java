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

import java.text.ParseException;
import java.util.HashSet;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.WebRuntimeException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.annotations.Action;
import org.nuxeo.ecm.webengine.rest.model.WebType;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("action")
public class ActionDescriptor implements TypeDescriptorBase {

    protected volatile String actionId;
    @XNode("@name")
    public String name;
    @XNode("@fragment")
    public String fragment;    
    @XNode("@type")
    public String type = WebType.ROOT_TYPE_NAME;
    @XNode("@enabled")
    public boolean enabled = true;
    @XNode("guard")
    public String guardExpression;
    @XNode("@class")
    public Class<?> klass;
    @XNodeList(value="category", type=HashSet.class, componentType=String.class, nullByDefault=false)
    public HashSet<String> categories;
        
    private volatile Guard guard;
    
    
    public ActionDescriptor() {
        // TODO Auto-generated constructor stub
    }
    
    public ActionDescriptor(Class<?> klass, String name, String type, String guard, boolean enabled) {
        this.klass = klass;
        this.name = name;
        this.type = type;
        this.guardExpression = guard;
        this.enabled = enabled;
        this.categories = new HashSet<String>();
    }
    
    public Guard getGuard() {
        if (guard == null) {
            if (guardExpression == null) {
                guard = Guard.DEFAULT;
            } else {
                try {
                    guard = PermissionService.parse(guardExpression);
                } catch (ParseException e) {
                    throw new WebRuntimeException("Parse error for action guard: "+guardExpression, e);
                }
            }
        }
        return guard;        
    }
    
    public boolean isEnabled(WebContext2 ctx) {
        if (!enabled) {
            return false;
        }        
        return getGuard().check(ctx);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj instanceof ActionDescriptor) {
            ActionDescriptor ad = (ActionDescriptor)obj;
            return name.equals(ad.name) && type.equals(ad.type) && TypeDescriptor.streq(fragment, ad.fragment);
        }
        return false;
    }
    
    @Override
    public ActionDescriptor clone() {
        try {
            ActionDescriptor ad  = (ActionDescriptor)super.clone();
            ad.categories = new HashSet<String>(categories); 
            return ad;
        } catch (CloneNotSupportedException e) {
            throw new Error("Canot happen");
        }
    }


    public String getId() {
        if (actionId == null) {
            actionId = new StringBuilder().append(name).append('@').append(type).toString();
        }
        return actionId;
    }
    
    public String getFragment() {
        return fragment;
    }
    
    public ActionDescriptor asActionDescriptor() {
        return this;
    }
    
    public TypeDescriptor asTypeDescriptor() {
        return null;
    }
    
    public boolean isMainFragment() {
        return fragment == null;
    }
    
    public static ActionDescriptor fromAnnotation(Class<?> clazz, Action action) {
        ActionDescriptor ad = new ActionDescriptor(clazz, action.value(), action.type(), action.guard(), action.enabled());
        for (String cat : action.categories()) {
            ad.categories.add(cat);
        }
        return ad;
    }
    
}
