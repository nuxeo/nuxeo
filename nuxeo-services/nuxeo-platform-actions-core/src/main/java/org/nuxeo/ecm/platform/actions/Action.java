/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: Action.java 28512 2008-01-06 11:52:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Descriptor for action.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("action")
public class Action implements Serializable, Cloneable, Comparable<Action> {

    public static final String[] EMPTY_CATEGORIES = new String[0];

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    protected String id = "";

    protected String link = null;

    @Deprecated
    @XNodeList(value = "link-params/param", type = Class[].class, componentType = Class.class)
    private Class<?>[] linkParams;

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
     * Attribute that provides a hint for action ordering.
     * <p>
     * :XXX: Action ordering remains a problem. We will continue to use the existing strategy of, by default, ordering
     * actions by specificity of registration and order of definition.
     */
    @XNode("@order")
    protected int order = 0;

    @XNodeList(value = "category", type = String[].class, componentType = String.class)
    protected String[] categories = EMPTY_CATEGORIES;

    // 'action -> filter(s)' association

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

    /**
     * Returns true if the enabled element was set on the descriptor, useful for merging.
     *
     * @since 5.8
     */
    public boolean isEnableSet() {
        return enabled != null;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = Boolean.valueOf(enabled);
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
            return getStringProperty("label");
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
     */
    public List<String> getCategoryList() {
        if (categories == null) {
            return null;
        }
        return Arrays.asList(categories);
    }

    public String getId() {
        return id;
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

    public int compareTo(Action anotherAction) {
        int cmp = order - anotherAction.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = id.compareTo(anotherAction.id);
        }
        return cmp;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    public ActionFilter[] getFilters() {
        return filters;
    }

    public void setFilters(ActionFilter[] filters) {
        this.filters = filters;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    /**
     * @deprecated since 5.6: useless now that EL expressions support parameters
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public Class[] getLinkParams() {
        return linkParams;
    }

    /**
     * @deprecated since 5.6: useless now that EL expressions support parameters
     */
    @Deprecated
    public void setLinkParams(Class<?>[] linkParams) {
        this.linkParams = linkParams;
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

    public boolean isImmediate() {
        if (immediate == null) {
            Map<String, Serializable> props = getProperties();
            if (props != null && props.containsKey("immediate")) {
                Object obj = props.get("immediate");
                if (obj instanceof String) {
                    return Boolean.valueOf((String) obj).booleanValue();
                } else if (obj instanceof Boolean) {
                    return ((Boolean) obj).booleanValue();
                }
            }
            return false;
        }
        return immediate.booleanValue();
    }

    public void setImmediate(boolean immediate) {
        this.immediate = Boolean.valueOf(immediate);
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
     */
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
     * Sets local properties programatically
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
            propertiesCache = new HashMap<String, Serializable>();
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
     */
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
        if (!(other instanceof Action)) {
            return false;
        }
        Action otherAction = (Action) other;
        return id == null ? otherAction.id == null : id.equals(otherAction.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public Action clone() {
        Action clone = new Action();
        clone.id = id;
        clone.link = link;
        if (linkParams != null) {
            clone.linkParams = linkParams.clone();
        }
        clone.enabled = enabled;
        clone.label = label;
        clone.icon = icon;
        clone.confirm = confirm;
        clone.help = help;
        clone.immediate = immediate;
        clone.accessKey = accessKey;
        clone.type = type;
        if (properties != null) {
            clone.properties = properties.clone();
        }
        clone.available = available;
        clone.filtered = filtered;
        clone.order = order;
        if (categories != null) {
            clone.categories = categories.clone();
        }
        if (filterIds != null) {
            clone.filterIds = new ArrayList<String>(filterIds);
        }
        if (filters != null) {
            clone.filters = new ActionFilter[filters.length];
            for (int i = 0; i < filters.length; i++) {
                clone.filters[i] = filters[i].clone();
            }
        }
        return clone;
    }

}
