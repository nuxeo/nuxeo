/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor to associate resources and flavors to a page.
 *
 * @since 7.4
 */
@XObject("page")
@XRegistry
public class PageDescriptor {

    public static final String RESOURCE_BUNDLE_PREFIX = "pageResourceBundle_";

    @XNode("@name")
    @XRegistryId
    protected String name;

    /**
     * @since 7.4
     */
    @XNode("@charset")
    protected String charset;

    @XNode("defaultFlavor")
    protected String defaultFlavor;

    @XNodeList(value = "flavors/flavor", type = ArrayList.class, componentType = String.class)
    @XMerge(value = XMerge.MERGE, fallback = "flavors@append", defaultAssignment = false)
    protected List<String> flavors = new ArrayList<>();

    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = String.class)
    @XMerge(value = XMerge.MERGE, fallback = "resources@append", defaultAssignment = false)
    protected List<String> resources = new ArrayList<>();

    /**
     * @since 7.4
     */
    @XNodeList(value = "resources/bundle", type = ArrayList.class, componentType = String.class)
    List<String> bundles = new ArrayList<>();

    // needed by XMap
    public PageDescriptor() {
    }

    // needed by service when building a page holding global resources too
    public PageDescriptor(String name, String charset, String defaultFlavor, List<String> flavors,
            List<String> resources, List<String> bundles) {
        this.name = name;
        this.charset = charset;
        this.defaultFlavor = defaultFlavor;
        if (flavors != null) {
            this.flavors.addAll(flavors);
        }
        if (resources != null) {
            this.resources.addAll(resources);
        }
        if (bundles != null) {
            this.bundles.addAll(bundles);
        }
    }

    public String getName() {
        return name;
    }

    public String getDefaultFlavor() {
        return defaultFlavor;
    }

    public List<String> getFlavors() {
        return flavors;
    }

    public boolean hasResources() {
        return !getResources().isEmpty();
    }

    public List<String> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public String getComputedResourceBundleName() {
        if ("*".equals(getName())) {
            return RESOURCE_BUNDLE_PREFIX + "*";
        }
        return RESOURCE_BUNDLE_PREFIX + getName().replaceAll("[^a-zA-Z]+", "_");
    }

    /**
     * @since 11.5
     */
    public List<String> getDeclaredResourceBundles() {
        return Collections.unmodifiableList(bundles);
    }

    /**
     * @since 7.4
     */
    public List<String> getResourceBundles() {
        Set<String> all = new LinkedHashSet<>();
        if (bundles != null) {
            all.addAll(bundles);
        }
        if (hasResources()) {
            all.add(getComputedResourceBundleName());
        }
        return new ArrayList<>(all);
    }

    /**
     * @since 7.4
     */
    public String getCharset() {
        return charset;
    }

}
