/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DirectoryTreeDescriptor.java 29556 2008-01-23 00:59:39Z jcarsique $
 */
package org.nuxeo.ecm.webapp.directory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionPropertiesDescriptor;

@XObject(value = "directoryTree")
public class DirectoryTreeDescriptor {

    /**
     * @since 6.0
     */
    public static final String ACTION_ID_PREFIX = "dirtree_";

    /**
     * @since 6.0
     */
    public static final String NAV_ACTION_CATEGORY = "TREE_EXPLORER";

    /**
     * @since 6.0
     */
    public static final String DIR_ACTION_CATEGORY = "DIRECTORY_TREE_EXPLORER";

    /**
     * @deprecated since 5.6, supports other schemas than 'vocabulary' and 'xvocabulary'.
     */
    @Deprecated
    public static final String VOCABULARY_SCHEMA = "vocabulary";

    /**
     * @deprecated since 5.6, supports other schemas than 'vocabulary' and 'xvocabulary'.
     */
    @Deprecated
    public static final String XVOCABULARY_SCHEMA = "xvocabulary";

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled = true;

    @XNode("@isNavigationTree")
    protected boolean isNavigationTree = true;

    /**
     * Label to be displayed as the root of the tree (description field).
     */
    @XNode("@label")
    protected String label;

    /**
     * Content view to be updated on node selection
     */
    @XNode("@contentView")
    protected String contentView;

    /**
     * Name of the QueryModel field that will be used updated on node selection.
     */
    @XNode("@field")
    protected String fieldName;

    /**
     * Name of the QueryModel schema for the field that will be used updated on node selection.
     */
    @XNode("@schema")
    protected String schemaName;

    /**
     * Id of the faces navigation case to return on node selection.
     */
    @XNode("@outcome")
    protected String outcome;

    /**
     * Allows the selection of several nodes of the tree.
     */
    @XNode("@multiselect")
    protected Boolean multiselect;

    /**
     * List of directories ids used to build the classification tree.
     */
    protected String[] directories;

    @XNodeList(value = "directory", componentType = String.class, type = String[].class)
    public void setDirectories(String[] directories) throws DirectoryException {
        this.directories = directories;
    }

    /**
     * @since 6.0
     */
    @XNode("@order")
    protected Integer order;

    public String getFieldName() {
        return fieldName;
    }

    public String getName() {
        return name;
    }

    public String[] getDirectories() {
        return directories;
    }

    public String getLabel() {
        return label;
    }

    public boolean isMultiselect() {
        if (multiselect == null) {
            return false;
        }
        return multiselect;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getContentView() {
        return contentView;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isNavigationTree() {
        return isNavigationTree;
    }

    public boolean hasContentViewSupport() {
        return contentView != null;
    }

    /**
     * @since 6.0
     */
    public Integer getOrder() {
        return order;
    }

    public void merge(DirectoryTreeDescriptor other) {
        if (other.schemaName != null) {
            this.schemaName = other.schemaName;
        }
        if (other.contentView != null) {
            this.contentView = other.contentView;
        }
        if (other.outcome != null) {
            this.outcome = other.outcome;
        }
        if (other.multiselect != null) {
            this.multiselect = other.multiselect;
        }
        if (other.label != null) {
            this.label = other.label;
        }
        if (other.directories != null) {
            this.directories = other.directories;
        }
        if (other.fieldName != null) {
            this.fieldName = other.fieldName;
        }
        this.enabled = other.enabled;
        this.isNavigationTree = other.isNavigationTree;
        if (other.order != null) {
            this.order = other.order;
        }
    }

    public DirectoryTreeDescriptor clone() {
        DirectoryTreeDescriptor clone = new DirectoryTreeDescriptor();
        clone.name = name;
        clone.enabled = enabled;
        clone.isNavigationTree = isNavigationTree;
        clone.label = label;
        clone.contentView = contentView;
        clone.fieldName = fieldName;
        clone.schemaName = schemaName;
        clone.outcome = outcome;
        clone.multiselect = multiselect;
        if (directories != null) {
            clone.directories = directories.clone();
        }
        clone.order = order;
        return clone;
    }

    /**
     * Helper to register a simple action based on the given descriptor
     *
     * @since 6.0
     */
    protected Action getAction() {
        String[] cats;
        if (isNavigationTree()) {
            cats = new String[] { NAV_ACTION_CATEGORY, DIR_ACTION_CATEGORY };
        } else {
            cats = new String[] { DIR_ACTION_CATEGORY };
        }
        Action a = new Action(ACTION_ID_PREFIX + getName(), cats);
        a.setType("rest_document_link");
        a.setLabel(getLabel());
        Map<String, String> props = new HashMap<String, String>();
        props.put("ajaxSupport", "true");
        props.put("link", "/incl/single_directory_tree_explorer.xhtml");
        ActionPropertiesDescriptor pdesc = new ActionPropertiesDescriptor();
        pdesc.setProperties(props);
        a.setPropertiesDescriptor(pdesc);
        Integer order = getOrder();
        if (order != null) {
            a.setOrder(order.intValue());
        } else {
            // use a default high default order for directory trees so that
            // they're displayed after standard navigation trees
            a.setOrder(1000);
        }
        a.setIcon("/img/" + getName() + ".png");
        // need to set a non-empty list
        a.setEnabled(Boolean.TRUE.equals(getEnabled()));
        a.setFilterIds(new ArrayList<String>());
        return a;
    }

}