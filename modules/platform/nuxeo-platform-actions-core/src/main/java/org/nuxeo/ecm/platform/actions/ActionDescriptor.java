/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.w3c.dom.Element;

/**
 * Action descriptor.
 *
 * @since 11.5
 */
@XObject("action")
@XRegistry(enable = false)
public class ActionDescriptor {

    @XNode("@id")
    @XRegistryId
    protected String id = "";

    @XNode("@link")
    protected String link;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected Boolean enabled;

    @XNode("@label")
    protected String label;

    @XNode("@icon")
    protected String icon;

    @XNode("@confirm")
    protected String confirm;

    @XNode("@help")
    protected String help;

    @XNode("@immediate")
    protected Boolean immediate;

    @XNode("@accessKey")
    protected String accessKey;

    /** @since 5.6 */
    @XNode("@type")
    protected String type;

    /**
     * Attribute that provides a hint for action ordering.
     */
    @XNode("@order")
    protected int order = 0;

    @XNodeList(value = "category", type = LinkedHashSet.class, componentType = String.class)
    protected Set<String> categories;

    @XNodeList(value = "filter-id", type = ArrayList.class, componentType = String.class)
    @XMerge("filters@merge")
    protected List<String> filterIds;

    @XNodeList(value = "filter@id", type = ArrayList.class, componentType = String.class)
    @XMerge("filters@merge")
    protected List<String> filterElementIds;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = Element.class)
    @XMerge("filters@merge")
    protected List<Element> filterElements;

    protected static final String PROPERTIES_MERGE_FALLBACK = "properties@append";

    /** @since 5.6 */
    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class)
    @XMerge(value = "properties" + XMerge.MERGE, fallback = PROPERTIES_MERGE_FALLBACK)
    protected Map<String, String> properties;

    /** @since 5.6 */
    @XNodeMap(value = "properties/propertyList", key = "@name", type = HashMap.class, componentType = ActionPropertyListDescriptor.class)
    @XMerge(value = "properties" + XMerge.MERGE, fallback = PROPERTIES_MERGE_FALLBACK)
    protected Map<String, ActionPropertyListDescriptor> listProperties;

    protected Map<String, Serializable> propertiesCache;

    protected String getStringProperty(String prop, String defaultValue) {
        Map<String, Serializable> props = getProperties();
        if (props != null && props.containsKey(prop)) {
            Object obj = props.get(prop);
            if (obj instanceof String) {
                return (String) obj;
            }
        }
        return defaultValue;
    }

    public String getLabel() {
        if (label == null) {
            return getStringProperty("label", null);
        }
        return label;
    }

    public String getIcon() {
        if (icon == null) {
            return getStringProperty("icon", null);
        }
        return icon;
    }

    /**
     * Returns the link for this action.
     * <p>
     * Since 5.7.3, fallbacks on properties when link is not set and retrieve it using key "link".
     */
    public String getLink() {
        if (link == null) {
            return getStringProperty("link", null);
        }
        return link;
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    public String getId() {
        return id;
    }

    /**
     * Returns the action order.
     *
     * @return the action order as an integer value
     */
    public int getOrder() {
        return order;
    }

    public List<String> getAllFilterIds() {
        Set<String> all = new HashSet<>(filterIds);
        all.addAll(filterElementIds);
        return new ArrayList<>(all);
    }

    public List<Element> getFilterElements() {
        return Collections.unmodifiableList(filterElements);
    }

    /**
     * Returns the confirm javascript for this element.
     * <p>
     * Since 5.7.3, fallbacks on properties when link is not set and retrieve it using key "confirm".
     */
    public String getConfirm() {
        if (confirm == null) {
            return getStringProperty("confirm", "");
        }
        return confirm;
    }

    public String getHelp() {
        if (help == null) {
            return getStringProperty("help", "");
        }
        return help;
    }

    public boolean isImmediate() {
        if (immediate == null) {
            Map<String, Serializable> props = getProperties();
            if (props != null && props.containsKey("immediate")) {
                Object obj = props.get("immediate");
                if (obj instanceof String) {
                    return Boolean.parseBoolean((String) obj);
                } else if (obj instanceof Boolean) {
                    return ((Boolean) obj).booleanValue();
                }
            }
            return false;
        }
        return immediate.booleanValue();
    }

    public String getType() {
        return type;
    }

    /**
     * Returns {@link #properties} set via descriptors.
     */
    public Map<String, Serializable> getProperties() {
        if (propertiesCache == null) {
            propertiesCache = new HashMap<>();
            if (properties != null) {
                propertiesCache.putAll(properties);
            }
            for (Map.Entry<String, ActionPropertyListDescriptor> prop : listProperties.entrySet()) {
                propertiesCache.put(prop.getKey(), prop.getValue().getValues());
            }
        }
        return propertiesCache;
    }

    public String getAccessKey() {
        if (accessKey == null) {
            return getStringProperty("accessKey", null);
        }
        return accessKey;
    }

}
