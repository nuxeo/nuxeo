/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
                provider = providerClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
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
