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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * @since TODO
 */
public abstract class AbstractRegistry implements Registry {

    protected boolean initialized = false;

    protected Set<String> flags = new HashSet<>();

    protected List<RegistryContribution> registrations = new ArrayList<>();

    public AbstractRegistry() {
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    protected void checkInitialized() {
        if (isInitialized()) {
            return;
        }
        initialize();
    }

    protected void initialize() {
        registrations.forEach(rc -> register(rc.getContext(), rc.getObject(), rc.getElement()));
        setInitialized(true);
    }

    @Override
    public void flag(String id) {
        flags.add(id);
    }

    @Override
    public boolean isFlagged(String id) {
        return flags.contains(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String flag) {
        flags.add(flag);
        registrations.add(new RegistryContribution(ctx, xObject, element, flag));
        setInitialized(false);
    }

    @Override
    public void unregister(String flag) {
        if (flag == null || !isFlagged(flag)) {
            return;
        }
        flags.remove(flag);
        Iterator<RegistryContribution> it = registrations.iterator();
        while (it.hasNext()) {
            RegistryContribution reg = it.next();
            if (flag.equals(reg.getFlag())) {
                it.remove();
            }
        }
        setInitialized(false);
    }

    protected abstract void register(Context ctx, XAnnotatedObject xObject, Element element);

}
