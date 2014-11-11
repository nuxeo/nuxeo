/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for action.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("action")
public class Action implements Serializable, Cloneable, Comparable<Action> {

    public static final String[] EMPTY_CATEGORIES = new String[0];

    private static final long serialVersionUID = 742479401240977268L;

    @XNode("@id")
    private String id = "";

    @XNode("@link")
    private String link;

    // XXX AT: param types still buggy, to fix eventually for optim
    @XNodeList(value = "link-params/param", type = Class[].class, componentType = Class.class)
    private Class<?>[] linkParams;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("@label")
    private String label;

    @XNode("@icon")
    private String icon;

    @XNode("@confirm")
    private String confirm;

    @XNode("@help")
    private String help;

    private boolean available = true;

    /**
     * Attribute that provides a hint for action ordering.
     * <p>
     * :XXX: Action ordering remains a problem. We will continue to use the
     * existing strategy of, by default, ordering actions by specificity of
     * registration and order of definition.
     */
    @XNode("@order")
    private int order = 0;

    @XNodeList(value = "category", type = String[].class, componentType = String.class)
    private String[] categories = EMPTY_CATEGORIES;

    // 'action -> filter(s)' association

    @XNodeList(value = "filter-id", type = ArrayList.class, componentType = String.class)
    private List<String> filterIds;

    @XNodeList(value = "filter", type = ActionFilter[].class, componentType = DefaultActionFilter.class)
    private ActionFilter[] filters;


    public Action() {
    }

    public Action(String id, String[] categories) {
        this.id = id;
        this.categories = categories;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String[] getCategories() {
        return categories;
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
     * @param order
     *            order of the action
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

    public Class[] getLinkParams() {
        return linkParams;
    }

    public void setLinkParams(Class<?>[] linkParams) {
        this.linkParams = linkParams;
    }

    public String getConfirm() {
        if (confirm == null) {
            return "";
        }
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
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
        return id == null ? otherAction.id == null
                : id.equals(otherAction.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    public boolean getAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getHelp() {
        if (help == null){
            return "";
        }
        return help;
    }

    public void setHelp(String title) {
        help = title;
    }

    @Override
    public Action clone() throws CloneNotSupportedException {
        Action clone = (Action)super.clone();

        return clone;
    }


    public void mergeWith(Action newOne) {
        // Icon
        String newIcon = newOne.getIcon();
        if (newIcon != null && !newIcon.equals(getIcon())) {
            setIcon(newIcon);
        }

        // Enabled ?
        if (newOne.isEnabled() != isEnabled()) {
            setEnabled(newOne.isEnabled());
        }

        // Merge categories without duplicates
        Set<String> mergedCategories = new HashSet<String>(Arrays.asList(getCategories()));
        mergedCategories.addAll(Arrays.asList(newOne.getCategories()));
        setCategories(mergedCategories.toArray(new String[mergedCategories.size()]));

        // label
        String newLabel = newOne.getLabel();
        if (newLabel != null && !newLabel.equals(getLabel())) {
            setLabel(newLabel);
        }

        // link
        String newLink = newOne.getLink();
        if (newLink != null && !newLink.equals(getLink())) {
            setLink(newLink);
        }

        // confirm
        String newConfirm = newOne.getConfirm();
        if (newConfirm != null && !newConfirm.equals(getConfirm())) {
            setConfirm(newConfirm);
        }

        // title (tooltip)
        String tooltip = newOne.getHelp();
        if (tooltip != null && !tooltip.equals(getHelp())) {
            setHelp(tooltip);
        }

        // XXX AT: maybe update param types but it seems a bit critical to do it
        // without control: a new action should be registered for this kind of
        // uses cases.

        // order
        int newOrder = newOne.getOrder();
        if (newOrder > 0 && newOrder != getOrder()) {
            setOrder(newOrder);
        }

        // filter ids
        HashSet<String> newFilterIds = new HashSet<String>();
        if (getFilterIds() != null) {
            newFilterIds.addAll(getFilterIds());
        }
        if (newOne.getFilterIds() != null) {
            newFilterIds.addAll(newOne.getFilterIds());
        }
        setFilterIds(new ArrayList<String>(newFilterIds));

        // filters
        // we are not using filters on merged actions - filterIds were already merged - this is all we need.
        setFilters(null);
    }


}
