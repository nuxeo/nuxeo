/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action implementation, that can be instantiated from an {@link ActionDescriptor} or programmatically.
 */
public class Action implements Serializable, Comparable<Action> {

    private static final long serialVersionUID = 1L;

    public static final String[] EMPTY_CATEGORIES = new String[0];

    protected final String id;

    protected String link;

    protected String label;

    protected String icon;

    protected String confirm = "";

    protected String help = "";

    protected boolean immediate;

    protected String accessKey;

    protected String type;

    protected Map<String, Serializable> properties = new HashMap<>();

    /**
     * Extra set of properties to be used by API, when creating actions on the fly without contributions to the service.
     */
    protected Map<String, Serializable> localProperties = new HashMap<>();

    protected boolean available = true;

    /** @since 8.2 */
    protected boolean filtered = false;

    /**
     * Attribute that provides a hint for action ordering.
     * <p>
     * :XXX: Action ordering remains a problem. We will continue to use the existing strategy of, by default, ordering
     * actions by specificity of registration and order of definition.
     */
    protected int order = 0;

    protected List<String> categories = new ArrayList<>();

    protected List<String> filterIds = new ArrayList<>();

    /**
     * @deprecated since 11.5
     */
    @Deprecated(since = "11.5")
    public Action() {
        this.id = "";
    }

    /** @since 11.5 */
    public Action(String id) {
        this.id = id;
    }

    /**
     * @deprecated since 11.5
     */
    @Deprecated(since = "11.5")
    public Action(String id, String[] categories) {
        this(id);
        if (categories != null) {
            setCategories(Arrays.asList(categories));
        }
    }

    /** @since 11.5 */
    public Action(ActionDescriptor desc) {
        this(desc.getId());
        setLink(desc.getLink());
        // add a default label if not set
        setLabel(desc.getLabel() == null ? desc.getId() : desc.getLabel());
        setIcon(desc.getIcon());
        setConfirm(desc.getConfirm());
        setHelp(desc.getHelp());
        setImmediate(desc.isImmediate());
        setAccessKey(desc.getAccessKey());
        setType(desc.getType());
        properties.putAll(desc.getProperties());
        setOrder(desc.getOrder());
        setCategories(desc.getCategories());
        setFilterIds(desc.getAllFilterIds());
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Returns the link for this action.
     */
    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String[] getCategories() {
        return categories.toArray(new String[0]);
    }

    /**
     * Returns the categories as a list.
     *
     * @since 7.2
     */
    public List<String> getCategoryList() {
        return Collections.unmodifiableList(categories);
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
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets the order of the action.
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
        return Collections.unmodifiableList(filterIds);
    }

    public void setFilterIds(List<String> filterIds) {
        this.filterIds.clear();
        this.filterIds.addAll(filterIds);
    }

    public void setCategories(List<String> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    /**
     * Returns the confirm javascript for this element.
     * <p>
     * Since 5.7.3, fallbacks on descriptor properties when link is not set and retrieve it using key "confirm".
     */
    public String getConfirm() {
        return confirm;
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
        return help;
    }

    public void setHelp(String title) {
        help = title;
    }

    public boolean isImmediate() {
        return immediate;
    }

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
     * Sets local properties programmatically.
     *
     * @since 5.6
     */
    public void setProperties(Map<String, Serializable> localProperties) {
        this.localProperties.clear();
        this.localProperties.putAll(localProperties);
    }

    /**
     * Returns an aggregate of {@link #localProperties} and {@link #properties} set via descriptors.
     *
     * @since 5.6
     */
    public Map<String, Serializable> getProperties() {
        Map<String, Serializable> res = new HashMap<>();
        res.putAll(properties);
        res.putAll(localProperties);
        return res;
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
        return accessKey;
    }

    /**
     * @deprecated since 11.5: use {@link #getAvailable()} instead.
     */
    @Deprecated(since = "11.5")
    public boolean isEnabled() {
        return getAvailable();
    }

    /**
     * @deprecated since 11.5: use {@link #setAvailable(boolean)} instead.
     */
    @Deprecated(since = "11.5")
    public void setEnabled(boolean enabled) {
        setAvailable(enabled);
    }

}
