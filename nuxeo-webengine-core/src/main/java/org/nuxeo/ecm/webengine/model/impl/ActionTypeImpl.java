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

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.ActionResource;
import org.nuxeo.ecm.webengine.model.ActionType;
import org.nuxeo.ecm.webengine.model.ObjectType;
import org.nuxeo.ecm.webengine.model.WebAction;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@XObject("action")
public class ActionTypeImpl implements TypeDescriptorBase, ActionType {

    @XNode("@name")
    public String name;

    @XNode("@fragment")
    public String fragment;

    @XNode("@type")
    public String type = ObjectType.ROOT_TYPE_NAME;

    @XNode("@enabled")
    public boolean enabled = true;

    @XNode("guard")
    public String guardExpression;

    @XNode("@class")
    public Class<ActionResource> clazz;

    @XNodeList(value = "category", type = HashSet.class, componentType = String.class, nullByDefault = false)
    public HashSet<String> categories;

    private volatile Guard guard;

    public ActionTypeImpl() {
    }

    public ActionTypeImpl(Class<ActionResource> klass, String name, String type,
            String guard, boolean enabled) {
        this.clazz = klass;
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
                    throw new WebException(
                            "Parse error for action guard: " + guardExpression,
                            e);
                }
            }
        }
        return guard;
    }

    public boolean isEnabled(WebContext ctx) {
        if (!enabled) {
            return false;
        }
        return getGuard().check(ctx);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof ActionTypeImpl) {
            ActionTypeImpl ad = (ActionTypeImpl) obj;
            return name.equals(ad.name) && type.equals(ad.type)
                    && Utils.streq(fragment, ad.fragment);
        }
        return false;
    }

    @Override
    public ActionTypeImpl clone() {
        try {
            ActionTypeImpl ad = (ActionTypeImpl) super.clone();
            ad.categories = new HashSet<String>(categories);
            return ad;
        } catch (CloneNotSupportedException e) {
            throw new Error("Canot happen");
        }
    }

    public String getId() {
        return new StringBuilder().append(name).append('@').append(type).toString();
    }

    public String getFragment() {
        return fragment;
    }

    public ActionTypeImpl asActionDescriptor() {
        return this;
    }

    public TypeDescriptor asTypeDescriptor() {
        return null;
    }

    public boolean isMainFragment() {
        return fragment == null;
    }

    @SuppressWarnings("unchecked")
    public static ActionTypeImpl fromAnnotation(Class<?> clazz, WebAction action) {
        ActionTypeImpl ad = new ActionTypeImpl((Class<ActionResource>)clazz, action.name(),
                action.type(), Utils.nullIfEmpty(action.guard()), action.enabled());
        String[] cats = action.categories();
        if (cats != null) {
            for (String cat : cats) {
                ad.categories.add(cat);
            }
        }
        return ad;
    }

    
    public Set<String> getCategories() {
        return categories;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getName() {
        return name;
    }

    public Class<ActionResource> getResourceClass() {
        return clazz;
    }
    
    public ActionResource newInstance() throws WebException {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate action type: "+name, e);           
        }
    }

}
