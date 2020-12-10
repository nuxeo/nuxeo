/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.common.xmap;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.common.xmap.registry.XRemove;
import org.w3c.dom.Element;

/**
 * Processor for annotated type into an object.
 */
public class XAnnotatedObject {

    final XMap xmap;

    final Class<?> klass;

    final Constructor<?> constructor;

    final Path path;

    final List<XAnnotatedMember> members;

    Sorter sorter;

    protected boolean hasRegistry;

    protected XAnnotatedMember registryId;

    protected XAnnotatedMember merge;

    protected XAnnotatedMember remove;

    protected XAnnotatedMember enable;

    public XAnnotatedObject(XMap xmap, Class<?> klass, XObject xob) {
        try {
            this.xmap = xmap;
            this.klass = klass;
            this.constructor = this.klass.getDeclaredConstructor();
            constructor.setAccessible(true);
            path = new Path(xob.value());
            members = new ArrayList<>();
            String[] order = xob.order();
            if (order.length > 0) {
                sorter = new Sorter(order);
            }
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Invalid xmap class - no default constructor found", e);
        }
    }

    public void addMember(XAnnotatedMember member) {
        members.add(member);
    }

    public Path getPath() {
        return path;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public Object newInstance(Context ctx, Element element) {
        return newInstance(ctx, element, null);
    }

    /**
     * Returns a new instance for given element, and given existing object, potentially applying merge logics.
     *
     * @since 11.5
     */
    public Object newInstance(Context ctx, Element element, Object existing) {
        if (existing == null) {
            Object ob;
            try {
                ob = constructor.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new IllegalArgumentException(e);
            }
            ctx.push(ob);
        } else {
            ctx.push(existing);
        }

        if (sorter != null) {
            Collections.sort(members, sorter);
            sorter = null; // sort only once
        }

        // set annotated members
        for (XAnnotatedMember member : members) {
            member.process(ctx, element, existing);
        }

        return ctx.pop();
    }

    /**
     * Returns true if a {@link XRegistry} annotation was resolved on this object.
     *
     * @since 11.5
     */
    public boolean hasRegistry() {
        return hasRegistry;
    }

    /**
     * Sets whether a {@link XRegistry} annotation was resolved on this object.
     *
     * @since 11.5
     */
    public void setHasRegistry(boolean hasRegistry) {
        this.hasRegistry = hasRegistry;
    }

    /**
     * Returns the {@link XRegistryId} annotation that was resolved for this object.
     *
     * @since 11.5
     */
    public XAnnotatedMember getRegistryId() {
        return registryId;
    }

    /**
     * Sets the {@link XRegistryId} annotation for this object.
     *
     * @since 11.5
     */
    public void setRegistryId(XAnnotatedMember registryId) {
        this.registryId = registryId;
    }

    /**
     * Returns the {@link XMerge} annotation that was resolved for this object.
     *
     * @since 11.5
     */
    public XAnnotatedMember getMerge() {
        return merge;
    }

    /**
     * Sets the {@link XMerge} annotation for this object.
     *
     * @since 11.5
     */
    public void setMerge(XAnnotatedMember merge) {
        this.merge = merge;
    }

    /**
     * Returns the {@link XRemove} annotation that was resolved for this object.
     *
     * @since 11.5
     */
    public XAnnotatedMember getRemove() {
        return remove;
    }

    /**
     * Sets the {@link XRemove} annotation for this object.
     *
     * @since 11.5
     */
    public void setRemove(XAnnotatedMember remove) {
        this.remove = remove;
    }

    /**
     * Returns the {@link XEnable} annotation that was resolved for this object.
     *
     * @since 11.5
     */
    public XAnnotatedMember getEnable() {
        return enable;
    }

    /**
     * Sets the {@link XEnable} annotation for this object.
     *
     * @since 11.5
     */
    public void setEnable(XAnnotatedMember enable) {
        this.enable = enable;
    }

}

class Sorter implements Comparator<XAnnotatedMember>, Serializable {

    private static final long serialVersionUID = -2546984283687927308L;

    private final Map<String, Integer> order = new HashMap<>();

    Sorter(String[] order) {
        for (int i = 0; i < order.length; i++) {
            this.order.put(order[i], i);
        }
    }

    @Override
    public int compare(XAnnotatedMember o1, XAnnotatedMember o2) {
        String p1 = o1.path == null ? "" : o1.path.path;
        String p2 = o2.path == null ? "" : o2.path.path;
        Integer order1 = order.get(p1);
        Integer order2 = order.get(p2);
        int n1 = order1 == null ? Integer.MAX_VALUE : order1;
        int n2 = order2 == null ? Integer.MAX_VALUE : order2;
        return n1 - n2;
    }

}
