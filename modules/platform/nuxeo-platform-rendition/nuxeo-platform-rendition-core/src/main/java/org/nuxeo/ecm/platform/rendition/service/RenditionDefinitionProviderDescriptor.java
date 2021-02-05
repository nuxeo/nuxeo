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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor contribution {@link RenditionDefinitionProvider}s.
 *
 * @since 7.2
 */
@XObject("renditionDefinitionProvider")
@XRegistry(enable = false)
public class RenditionDefinitionProviderDescriptor {

    private static final Log log = getLog(RenditionDefinitionProviderDescriptor.class);

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected Boolean enabled;

    @XNode("@class")
    protected Class<? extends RenditionDefinitionProvider> providerClass;

    // @since 11.5: provider instance cache
    protected RenditionDefinitionProvider provider;

    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    public String getName() {
        return name;
    }

    public Class<? extends RenditionDefinitionProvider> getProviderClass() {
        return providerClass;
    }

    protected RenditionDefinitionProvider createProvider() {
        if (providerClass != null) {
            try {
                return providerClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error(String.format("Unable to instantiate RenditionDefinitionProvider for '%s'", getName()), e);
            }
        }
        return null;
    }

    public RenditionDefinitionProvider getProvider() {
        if (provider != null) {
            return provider;
        }
        return createProvider();
    }

    public List<String> getFilterIds() {
        return Collections.unmodifiableList(filterIds);
    }

    /** @since 11.5 */
    public RenditionDefinitionProvider initProvider() {
        provider = createProvider();
        return provider;
    }

}
