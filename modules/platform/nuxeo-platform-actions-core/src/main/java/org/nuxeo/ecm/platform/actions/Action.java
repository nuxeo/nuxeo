/*
 * (C) Copyright 2006-2026 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.actions;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for action.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("action")
public class Action implements Serializable, Cloneable, Comparable<Action>, Descriptor {

    public static final String[] EMPTY_CATEGORIES = new String[0];

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    protected String id = "";

    protected String link = null;

    @XNode("@enabled")
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

    /**
     * @since 5.6
     */
    @XNode("@type")
    protected String type = null;

    /**
     * @since 5.6
     */
    @XNode("properties")
    protected ActionPropertiesDescriptor properties;

    /**
     * Extra set of properties to be used by API, when creating actions on the fly without contributions to the service.
     *
     * @since 5.6
     */
    protected Map<String, Serializable> localProperties;

    /**
     * @since 5.6
     */
    protected Map<String, Serializable> propertiesCache;

    protected boolean available = true;

    /**
     * @since 8.2
     */
    protected boolean filtered = false;

    /**
     * Provides a hint for action ordering.
     * <p>
     * :XXX: Action ordering remains a problem. We will continue to use the existing strategy of, by default, ordering
     * actions by specificity of registration and order of definition.
     */
    @XNode("@order")
    protected int order = 0;

    @XNodeList(value = "category", type = String[].class, componentType = String.class)
    protected String[] categories = EMPTY_CATEGORIES;

    @XNodeList(value = "filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    @XNodeList(value = "filter", type = ActionFilter[].class, componentType = DefaultActionFilter.class)
    protected ActionFilter[] filters;

    public Action() {
    }

    public Action(String id, String[] categories) {
        this.id = id;
        this.categories = categories;
    }

    public Action(Action other) {
        id = other.id;
        link = other.link;
        enabled = other.enabled;
        label = other.label;
        icon = other.icon;
        confirm = other.confirm;
        help = other.help;
        immediate = other.immediate;
        accessKey = other.accessKey;
        type = other.type;
        if (other.properties != null) {
            properties = other.properties.clone();
        }
        available = other.available;
        filtered = other.filtered;
        order = other.order;
        if (other.categories != null) {
            categories = other.categories.clone();
        }
        if (other.filterIds != null) {
            filterIds = new ArrayList<>(other.filterIds);
        }
        if (other.filters != null) {
            filters = new ActionFilter[other.filters.length];
            for (int i = 0; i < other.filters.length; i++) {
                filters[i] = other.filters[i].clone();
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns true if the enabled element was set on the descriptor, useful for merging.
     *
     * @since 5.8
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public boolean isEnableSet() {
        return enabled != null;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected String getStringProperty(String prop) {
        Map<String, Serializable> props = getProperties();
        if (props != null && props.containsKey(prop)) {
            Object obj = props.get(prop);
            if (obj instanceof String) {
                return (String) obj;
            }
        }
        return null;
    }

    public String getLabel() {
        if (label == null) {
            return getIfNull(getStringProperty("label"), getId());
        }
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        if (icon == null) {
            return getStringProperty("icon");
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Returns the link for this action.
     * <p>
     * Since 5.7.3, fallbacks on properties when link is not set and retrieve it using key "link".
     */
    public String getLink() {
        if (link == null) {
            return getStringProperty("link");
        }
        return link;
    }

    @XNode("@link")
    public void setLink(String link) {
        if (link != null) {
            this.link = Framework.expandVars(link);
        }
    }

    public String[] getCategories() {
        return categories;
    }

    /**
     * Returns the categories as a list.
     *
     * @since 7.2
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public List<String> getCategoryList() {
        if (categories == null) {
            return null;
        }
        return List.of(categories);
    }

    @Override
    public String toString() {
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

    /**
     * Sets the order of the action.
     *
     * @param order order of the action
     */
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(Action anotherAction) {
        int cmp = order - anotherAction.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = id.compareTo(anotherAction.id);
        }
        return cmp;
    }

    public List<String> getFilterIds() {
        var filterIds = new LinkedHashSet<>(getIfNull(this.filterIds, List::of));
        for (var filter : getIfNull(getFilters(), () -> new ActionFilter[] {})) {
            filterIds.add(filter.getId());
        }
        return List.copyOf(filterIds);
    }

    /**
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    public ActionFilter[] getFilters() {
        return filters;
    }

    public void setFilters(ActionFilter[] filters) {
        this.filters = filters;
    }

    /**
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    /**
     * Returns the confirm javascript for this element.
     * <p>
     * Since 5.7.3, fallbacks on properties when link is not set and retrieve it using key "confirm".
     */
    public String getConfirm() {
        if (confirm == null) {
            String conf = getStringProperty("confirm");
            if (conf == null) {
                conf = "";
            }
            return conf;
        } else {
            return confirm;
        }
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public boolean getAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * @since 8.2
     */
    public boolean isFiltered() {
        return filtered;
    }

    /**
     * @since 8.2
     */
    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public String getHelp() {
        if (help == null) {
            String conf = getStringProperty("help");
            if (conf == null) {
                conf = "";
            }
            return conf;
        } else {
            return help;
        }
    }

    public void setHelp(String title) {
        help = title;
    }

    /**
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public boolean isImmediate() {
        if (immediate == null) {
            Map<String, Serializable> props = getProperties();
            if (props != null && props.containsKey("immediate")) {
                Object obj = props.get("immediate");
                if (obj instanceof String s) {
                    return Boolean.parseBoolean(s);
                } else if (obj instanceof Boolean b) {
                    return b;
                }
            }
            return false;
        }
        return immediate;
    }

    /**
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    /**
     * @since 5.6
     */
    public String getType() {
        return type;
    }

    /**
     * @since 5.6
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @since 5.6
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public ActionPropertiesDescriptor getPropertiesDescriptor() {
        return properties;
    }

    /**
     * @since 5.6
     */
    public void setPropertiesDescriptor(ActionPropertiesDescriptor properties) {
        this.properties = properties;
        this.propertiesCache = null;
    }

    /**
     * Sets local properties programmatically
     *
     * @since 5.6
     */
    public void setProperties(Map<String, Serializable> localProperties) {
        this.localProperties = localProperties;
        this.propertiesCache = null;
    }

    /**
     * Returns an aggregate of {@link #localProperties} and {@link #properties} set via descriptors.
     *
     * @since 5.6
     */
    public Map<String, Serializable> getProperties() {
        if (propertiesCache == null) {
            propertiesCache = new HashMap<>();
            if (properties != null) {
                propertiesCache.putAll(properties.getAllProperties());
            }
            if (localProperties != null) {
                propertiesCache.putAll(localProperties);
            }
        }
        return propertiesCache;
    }

    /**
     * @since 5.6
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * @since 5.6
     */
    public String getAccessKey() {
        if (accessKey == null) {
            return getStringProperty("accessKey");
        }
        return accessKey;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Action otherAction)) {
            return false;
        }
        return Objects.equals(id, otherAction.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    /**
     * @deprecated since 2025.0 This is not the purpose of clone. use {@link Action#Action(Action)} instead
     */
    @Override
    @Deprecated(since = "2025.0")
    public Action clone() {
        return new Action(this);
    }

    @Override
    public Action merge(Descriptor o) {
        var other = (Action) o;
        var merged = new Action();
        merged.id = getIfNull(other.id, id);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.icon = getIfNull(other.icon, icon);
        var categoriesSet = new LinkedHashSet<>(List.of(getIfNull(categories, () -> new String[] {})));
        categoriesSet.addAll(List.of(getIfNull(other.categories, () -> new String[] {})));
        merged.categories = categoriesSet.toArray(String[]::new);
        merged.label = getIfNull(other.label, label);
        merged.link = getIfNull(other.link, link);
        merged.confirm = defaultIfBlank(other.confirm, confirm);
        merged.help = getIfNull(other.help, help);
        merged.immediate = getIfNull(other.immediate, immediate);
        other.accessKey = getIfNull(other.accessKey, accessKey);
        merged.type = getIfNull(other.type, type);
        merged.order = other.order > 0 ? other.order : order;
        var filterIdsSet = new LinkedHashSet<>(getIfNull(filterIds, List::of));
        filterIdsSet.addAll(getIfNull(other.filterIds, List::of));
        merged.filterIds = new ArrayList<>(filterIdsSet);
        var filtersSet = new LinkedHashSet<>(List.of(getIfNull(filters, () -> new ActionFilter[] {})));
        filtersSet.addAll(List.of(getIfNull(other.filters, () -> new ActionFilter[] {})));
        merged.filters = filtersSet.toArray(ActionFilter[]::new);
        if (other.properties == null) {
            if (properties != null) {
                merged.properties = properties.clone();
            }
        } else {
            if (other.properties.isAppend()) {
                if (properties == null) {
                    merged.properties = other.properties.clone();
                } else {
                    merged.properties = properties.clone();
                    merged.properties.merge(other.properties.clone());
                }
            } else {
                merged.properties = other.properties.clone();
            }
        }
        return merged;
    }
}
