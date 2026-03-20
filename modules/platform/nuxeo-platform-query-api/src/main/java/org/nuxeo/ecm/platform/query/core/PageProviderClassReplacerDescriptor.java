/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 6.0
 */
@XObject(value = "replacer")
public class PageProviderClassReplacerDescriptor implements PageProviderClassReplacerDefinition {

    protected Class<? extends PageProvider<?>> providerClass;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeList(value = "provider", type = String[].class, componentType = String.class)
    protected String[] names = new String[0];

    @Override
    public String getPageProviderClassName() {
        return providerClass.getName();
    }

    @XNode("@withClass")
    @SuppressWarnings("unchecked")
    protected void setPageProviderClassName(String className) {
        try {
            var ret = (Class<? extends PageProvider<?>>) Class.forName(className);
            if (!PageProvider.class.isAssignableFrom(ret)) {
                throw new IllegalStateException(
                        String.format("Class: %s does not implement PageProvider interface", className));
            }
            providerClass = ret;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Class: %s not found", className), e);
        }

    }

    @Override
    public Class<? extends PageProvider<?>> getPageProviderClass() {
        return providerClass;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getPageProviderNames() {
        return List.of(names);
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (PageProviderClassReplacerDescriptor) o;
        var merged = new PageProviderClassReplacerDescriptor();
        merged.providerClass = getIfNull(other.providerClass, providerClass);
        merged.enabled = other.enabled;
        // merge names
        var namesSet = new LinkedHashSet<>(Set.of(nullToEmpty(names)));
        namesSet.addAll(Set.of(nullToEmpty(other.names)));
        merged.names = namesSet.toArray(String[]::new);
        return merged;
    }
}
