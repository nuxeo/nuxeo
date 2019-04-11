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

package org.nuxeo.ecm.webengine.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("link")
public class LinkDescriptor implements Cloneable, LinkHandler {

    @XNode("@id")
    protected String id;

    @XNode("@path")
    protected String path;

    @XNode("@fragment")
    protected String fragment;

    protected volatile LinkHandler handler;

    @XNode("@handler")
    protected String handlerClass;

    @XNodeList(value = "category", type = ArrayList.class, componentType = String.class, nullByDefault = false)
    protected List<String> categories;

    @XNode(value = "type")
    protected String type = ResourceType.ROOT_TYPE_NAME;

    /**
     * The object adapter the link may have as owner
     */
    @XNode(value = "adapter")
    protected String adapter = ResourceType.ROOT_TYPE_NAME;

    @XNodeList(value = "facet", type = String[].class, componentType = String.class, nullByDefault = true)
    protected String[] facets;

    protected Guard guard = Guard.DEFAULT;

    public LinkDescriptor() {
    }

    public LinkDescriptor(String id) {
        this(id, null);
    }

    public LinkDescriptor(String id, String fragment) {
        this.id = id;
        this.fragment = fragment;
    }

    @XNode("guard")
    public void setGuard(String expr) throws ParseException {
        guard = PermissionService.parse(expr);
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public void setHandler(LinkHandler handler) {
        this.handler = handler;
    }

    public String getCode(Resource resource) {
        try {
            if (handler == null) {
                if (handlerClass != null) {
                    Object obj = resource.getModule().loadClass(handlerClass).getDeclaredConstructor().newInstance();
                    if (obj instanceof LinkHandlerFactory) {
                        handler = ((LinkHandlerFactory) obj).getHandler(this, resource);
                    } else {
                        handler = (LinkHandler) obj;
                    }
                } else {
                    handler = this;
                }
            }
            return handler.getCode(this, resource);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Failed to instantiate link handler", e);
        }
    }

    public LinkHandler getHandler() {
        return handler;
    }

    public String getAdapter() {
        return adapter;
    }

    public String getType() {
        return type;
    }

    public String[] getFacets() {
        return facets;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void addCategories(Collection<String> categories) {
        this.categories.addAll(categories);
    }

    public void addCategory(String category) {
        categories.add(category);
    }

    public List<String> getCategories() {
        return categories;
    }

    public boolean hasCategory(String category) {
        return categories != null && categories.contains(category);
    }

    public boolean acceptResource(Resource context) {
        if (type == ResourceType.ROOT_TYPE_NAME && adapter == ResourceType.ROOT_TYPE_NAME && facets == null) {
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
            if (adapter != ResourceType.ROOT_TYPE_NAME) {
                if (!context.isInstanceOf(type)) {
                    return false;
                }
            } else {
                return context.isInstanceOf(type);
            }
        }
        if (adapter != ResourceType.ROOT_TYPE_NAME) {
            Resource adapterRs = context.getNext();
            if (adapterRs != null && adapterRs.isAdapter()) {
                return adapterRs.isInstanceOf(adapter);
            }
            return false;
        }
        return true;
    }

    public boolean isEnabled(Resource context) {
        if (acceptResource(context)) {
            return guard == null || guard.check(context);
        }
        return false;
    }

    // TODO: here we are using absolute paths -> will be better to use relative
    // paths?
    @Override
    public String getCode(LinkDescriptor link, Resource resource) {
        String parentPath;
        if (adapter != ResourceType.ROOT_TYPE_NAME) {
            parentPath = resource.getActiveAdapter().getPath();
        } else {
            parentPath = resource.getPath();
        }
        StringBuilder res = new StringBuilder();
        res.append(parentPath);
        // avoid adding duplicate '/' character
        if (parentPath != null && parentPath.endsWith("/") && path != null && path.startsWith("/")) {
            res.append(path.substring(1));
        } else {
            res.append(path);
        }
        return res.toString();
    }

    public boolean isFragment() {
        return fragment != null;
    }

    public void applyFragment(LinkDescriptor fragment) {
        if (fragment.categories != null && !fragment.categories.isEmpty()) {
            if (categories == null) {
                categories = new ArrayList<>(fragment.categories);
            } else {
                categories.addAll(fragment.categories);
            }
        }
        if (fragment.type != null && !fragment.type.equals(ResourceType.ROOT_TYPE_NAME)) {
            type = fragment.type;
        }
        if (fragment.adapter != null && !fragment.adapter.equals(ResourceType.ROOT_TYPE_NAME)) {
            adapter = fragment.adapter;
        }
        if (fragment.facets != null && fragment.facets.length > 0) {
            if (facets == null) {
                facets = fragment.facets;
            } else {
                Set<String> set = new HashSet<>();
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
        return (LinkDescriptor) super.clone();
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

    @Override
    public String toString() {
        return id;
    }

}
