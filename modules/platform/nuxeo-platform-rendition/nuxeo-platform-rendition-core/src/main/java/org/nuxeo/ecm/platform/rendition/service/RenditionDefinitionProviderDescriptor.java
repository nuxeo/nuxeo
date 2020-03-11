/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.rendition.service;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor contribution {@link RenditionDefinitionProvider}s.
 *
 * @since 7.2
 */
@XObject("renditionDefinitionProvider")
public class RenditionDefinitionProviderDescriptor {

    private static final Log log = getLog(RenditionDefinitionProviderDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    Boolean enabled;

    @XNode("@class")
    protected Class<? extends RenditionDefinitionProvider> providerClass;

    protected RenditionDefinitionProvider provider;

    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public boolean isEnabledSet() {
        return enabled != null;
    }

    public Class<? extends RenditionDefinitionProvider> getProviderClass() {
        return providerClass;
    }

    public RenditionDefinitionProvider getProvider() {
        if (provider == null && providerClass != null) {
            try {
                provider = providerClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error(String.format("Unable to instantiate RenditionDefinitionProvider for '%s'", getName()), e);
            }
        }
        return provider;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setProviderClass(Class<? extends RenditionDefinitionProvider> providerClass) {
        this.providerClass = providerClass;
    }

    public void setProvider(RenditionDefinitionProvider provider) {
        this.provider = provider;
    }

    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    @Override
    public RenditionDefinitionProviderDescriptor clone() {
        RenditionDefinitionProviderDescriptor clone = new RenditionDefinitionProviderDescriptor();
        clone.name = name;
        clone.enabled = enabled;
        clone.providerClass = providerClass;
        if (filterIds != null) {
            clone.filterIds = new ArrayList<>();
            clone.filterIds.addAll(filterIds);
        }
        return clone;
    }
}
