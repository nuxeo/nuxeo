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

package org.nuxeo.ecm.webengine.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.security.PermissionService;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("link")
public class LinkDescriptor implements Cloneable, LinkHandler {

    @XNode("@id")
    protected String id;

    @XNode("@path")
    protected String path;

    @XNode("@fragment")
    protected String fragment = null;

    protected volatile LinkHandler handler;

    @XNode("@handler")
    protected String handlerClass;

    @XNodeList(value="category", type=ArrayList.class, componentType=String.class, nullByDefault=false)
    protected List<String> categories;

    @XNode(value="type")
    protected String type = ResourceType.ROOT_TYPE_NAME;

    @XNodeList(value="facet", type=String[].class, componentType=String.class, nullByDefault=true)
    protected String[] facets;

    protected org.nuxeo.ecm.webengine.security.Guard guard = org.nuxeo.ecm.webengine.security.Guard.DEFAULT;

    /**
     *
     */
    public LinkDescriptor() {
    }

    public LinkDescriptor(String id) {
        this (id, null);
    }

    public LinkDescriptor(String id, String fragment) {
        this.id = id;
        this.fragment = fragment;
    }


    @XNode("guard")
    public void setGuard(String expr) throws ParseException {
        guard = PermissionService.parse(expr);
    }


    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * @param handler the handler to set.
     */
    public void setHandler(LinkHandler handler) {
        this.handler = handler;
    }

    public String getCode(Resource resource) {
        try {
            if (handler == null) {
                if (handlerClass != null) {
                    Object obj = resource.getModule().loadClass(handlerClass).newInstance();
                    if (obj instanceof LinkHandlerFactory) {
                        handler = ((LinkHandlerFactory)obj).getHandler(this, resource);
                    } else {
                        handler = (LinkHandler)obj;
                    }
                } else {
                    handler = this;
                }
            }
            return handler.getCode(this, resource);
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate link handler", e);
        }
    }

    /**
     * @return the handler.
     */
    public LinkHandler getHandler() {
        return handler;
    }

    /**
     * @param categories the categories to set.
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void addCategories(Collection<String> categories) {
        this.categories.addAll(categories);
    }

    public void addCategory(String category) {
        categories.add(category);
    }

    /**
     * @return the categories.
     */
    public List<String> getCategories() {
        return categories;
    }

    public boolean hasCategory(String category) {
        return categories != null && categories.contains(category);
    }

    public boolean acceptResource(Resource context) {
        if (type == ResourceType.ROOT_TYPE_NAME && facets == null) {
            return true;
        }
        if (facets != null && facets.length > 0) {
            for (String facet : facets) {
                if (!context.hasFacet(facet)) {
                    return false;
                }
            }
        }
        if (type != ResourceType.ROOT_TYPE_NAME) {
            return context.isInstanceOf(type);
        }
        return true;
    }

    public boolean isEnabled(Resource context) {
        if (acceptResource(context)) {
            return guard == null || guard.check(context);
        }
        return false;
    }

    public String getCode(LinkDescriptor link, Resource resource) {
        return new StringBuilder().append(resource.getPath()).append(path).toString();
    }

    public boolean isFragment() {
        return fragment != null;
    }

    @SuppressWarnings("unchecked")
    public void applyFragment(LinkDescriptor fragment) {
        if (fragment.categories != null && !fragment.categories.isEmpty()) {
            if (categories == null) {
                categories = new ArrayList<String>(fragment.categories);
            } else {
                categories.addAll(fragment.categories);
            }
        }
        if (fragment.type != null && !fragment.type.equals(ResourceType.ROOT_TYPE_NAME)) {
            type = fragment.type;
        }
        if (fragment.facets != null && fragment.facets.length > 0) {
            if (facets == null) {
                facets = fragment.facets;
            } else {
                HashSet<String> set = new HashSet<String>();
                set.addAll(Arrays.asList(facets));
                set.addAll(Arrays.asList(fragment.facets));
                facets = set.toArray(new String[set.size()]);
            }
        }
        if (fragment.handlerClass != null) {
            handler = null;
            handlerClass = fragment.handlerClass;
        }
        if (fragment.guard != null) {
            guard = fragment.guard;
        }
        if (fragment.path != null) {
            path = fragment.path;
        }
        this.fragment = fragment.fragment;
    }

    @Override
    public LinkDescriptor clone() throws CloneNotSupportedException {
        return (LinkDescriptor)super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof LinkDescriptor) {
            LinkDescriptor ld = (LinkDescriptor) obj;
            return id.equals(ld.id) && Utils.streq(fragment, ld.fragment);
        }
        return false;
    }

}
