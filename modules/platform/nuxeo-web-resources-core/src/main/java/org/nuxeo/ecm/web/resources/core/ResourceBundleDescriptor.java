/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.core;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 7.3
 */
@XObject("bundle")
public class ResourceBundleDescriptor implements ResourceBundle {

    private static final Logger log = LogManager.getLogger(ResourceBundleDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("resources@append")
    protected boolean append;

    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = String.class)
    protected List<String> resources;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getResources() {
        if (resources.removeIf(StringUtils::isBlank)) {
            log.error("Some resources references were null or blank while setting: {} and have been suppressed. "
                    + "This probably happened because some <resource> tags were empty in the xml declaration. "
                    + "The correct form is <resource>resource name</resource>.", name);
        }
        return resources;
    }

    public boolean isAppend() {
        return append;
    }

    /**
     * @since 7.4
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @since 7.4
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * @since 7.4
     */
    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (ResourceBundleDescriptor) o;
        var merged = new ResourceBundleDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.resources = new ArrayList<>();
        if (other.isAppend()) {
            merged.resources.addAll(emptyIfNull(resources));
        }
        merged.resources.addAll(emptyIfNull(other.resources));
        return merged;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceBundleDescriptor b)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return new EqualsBuilder().append(name, b.name)
                                  .append(append, b.append)
                                  .append(resources, b.resources)
                                  .isEquals();
    }

}
