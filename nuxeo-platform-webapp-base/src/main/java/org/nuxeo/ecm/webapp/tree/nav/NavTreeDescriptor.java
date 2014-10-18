/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree.nav;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionPropertiesDescriptor;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeDescriptor;

/**
 * Descriptor for navigation tree contributions.
 * <p>
 * Moved from module nuxeo-platform-virtual-navigation-web, originally added in
 * 5.6.
 *
 * @since 6.0
 */
@XObject("navTree")
public class NavTreeDescriptor implements Serializable,
        Comparable<NavTreeDescriptor> {

    /**
     * @since 6.0
     */
    public static final String ACTION_ID_PREFIX = "navtree_";

    private static final long serialVersionUID = 1L;

    @XNode("@treeId")
    private String treeId;

    @XNode("@treeLabel")
    private String treeLabel;

    @XNode("@xhtmlview")
    private String xhtmlview;

    @XNode("@directoryTreeBased")
    private boolean directoryTreeBased = false;

    @XNode("@order")
    private Integer order = Integer.valueOf(100);

    @XNode("@enabled")
    private boolean enabled = true;

    public boolean isDirectoryTreeBased() {
        return directoryTreeBased;
    }

    public void setDirectoryTreeBased(boolean directoryTreeBased) {
        this.directoryTreeBased = directoryTreeBased;
    }

    public String getXhtmlview() {
        return xhtmlview;
    }

    public void setXhtmlview(String xhtmlview) {
        this.xhtmlview = xhtmlview;
    }

    public NavTreeDescriptor() {
    }

    public NavTreeDescriptor(String treeId, String treeLabel) {
        this(treeId, treeLabel, false);
    }

    public NavTreeDescriptor(String treeId, String treeLabel,
            boolean directoryTreeBased) {
        this.treeId = treeId;
        this.treeLabel = treeLabel;
        this.directoryTreeBased = directoryTreeBased;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public String getTreeLabel() {
        return treeLabel;
    }

    public void setTreeLabel(String treeLabel) {
        this.treeLabel = treeLabel;
    }

    public Integer getOrder() {
        return order;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @since 5.6
     */
    @Override
    public int compareTo(NavTreeDescriptor o) {
        return getOrder().compareTo(o.getOrder());
    }

    /**
     * Helper to register a simple action based on the given descriptor
     *
     * @since 6.0
     */
    protected Action getAction() {
        Action a = new Action(ACTION_ID_PREFIX + getTreeId(),
                new String[] { DirectoryTreeDescriptor.NAV_ACTION_CATEGORY });
        a.setType("rest_document_link");
        a.setLabel(getTreeLabel());
        Map<String, String> props = new HashMap<String, String>();
        props.put("ajaxSupport", "true");
        if (isDirectoryTreeBased()) {
            props.put("link", "/incl/single_directory_tree_explorer.xhtml");
        } else {
            props.put("link", getXhtmlview());
        }
        ActionPropertiesDescriptor pdesc = new ActionPropertiesDescriptor();
        pdesc.setProperties(props);
        a.setPropertiesDescriptor(pdesc);
        Integer order = getOrder();
        if (order != null) {
            a.setOrder(order.intValue());
        }
        a.setEnabled(isEnabled());
        a.setIcon(String.format("/img/%s.png", getTreeId()));
        // need to set a non-empty list
        a.setFilterIds(new ArrayList<String>());
        return a;
    }

}