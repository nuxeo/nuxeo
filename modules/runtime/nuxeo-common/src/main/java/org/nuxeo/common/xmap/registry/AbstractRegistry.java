/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Abstract class for {@link Registry} common logic.
 *
 * @since 11.5
 */
public abstract class AbstractRegistry implements Registry {

    // volatile for double-checked locking
    protected volatile boolean initialized;

    protected Set<String> tags = ConcurrentHashMap.newKeySet();

    protected List<RegistryContribution> registrations = new CopyOnWriteArrayList<>();

    protected AbstractRegistry() {
    }

    protected boolean isInitialized() {
        synchronized (this) {
            return initialized;
        }
    }

    protected void setInitialized(boolean initialized) {
        synchronized (this) {
            this.initialized = initialized;
        }
    }

    protected void checkInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialize();
                    initialized = true;
                }
            }
        }
    }

    @Override
    public void initialize() {
        registrations.forEach(
                rc -> doRegister(rc.getContext(), rc.getObject(), rc.getElement(), rc.getRuntimeExtensionFromTag()));
        setInitialized(true);
    }

    @Override
    public void tag(String id) {
        tags.add(id);
    }

    @Override
    public boolean isTagged(String id) {
        return tags.contains(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        tag(tag);
        registrations.add(new RegistryContribution(ctx, xObject, element, tag));
        setInitialized(false);
    }

    @Override
    public void unregister(String tag) {
        if (tag == null || !isTagged(tag)) {
            return;
        }
        tags.remove(tag);
        registrations.removeIf(reg -> tag.equals(reg.getTag()));
        setInitialized(false);
    }

    protected abstract <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId);

    protected boolean shouldRemove(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        XAnnotatedMember remove = xObject.getRemove();
        return remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element));
    }

    /**
     * Returns true if contribution should be enabled, false if it should be disabled, and null if the enablement status
     * should not change.
     */
    protected Boolean shouldEnable(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        XAnnotatedMember enable = xObject.getEnable();
        if (enable != null && enable.hasValue(ctx, element)) {
            Object enabled = enable.getValue(ctx, element);
            return enabled == null || Boolean.TRUE.equals(enabled);
        }
        return null;
    }

    protected boolean onlyHandlesEnablement(Context ctx, XAnnotatedObject xObject, Element element, boolean hasId) {
        // checks no children
        if (element.getChildNodes().getLength() > 0) {
            return false;
        }
        // checks no attribute other than enablement
        int nbAttr = element.getAttributes().getLength();
        int maxNbAttr = hasId ? 2 : 1;
        if (nbAttr > maxNbAttr) {
            return false;
        }
        XAnnotatedMember enable = xObject.getEnable();
        return enable != null && enable.hasValue(ctx, element);
    }

    protected boolean shouldMerge(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        XAnnotatedMember merge = xObject.getMerge();
        return merge != null && Boolean.TRUE.equals(merge.getValue(ctx, element));
    }

    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        return (T) xObject.newInstance(ctx, element, existing);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Context ctx, XAnnotatedObject xObject, Element element) {
        return (T) xObject.newInstance(ctx, element);
    }

}
