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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;

@XObject(value = "directoryTree")
public class DirectoryTreeDescriptor {

    private static final Log log = LogFactory.getLog(DirectoryTreeDescriptor.class);

    public static final String VOCABULARY_SCHEMA = "vocabulary";

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
     * Stateful QueryModel to be edited on node selection.
     *
     * @deprecated use {@link #contentView} instead
     */
    @Deprecated
    @XNode("@querymodel")
    protected String querymodel;

    /**
     * Content view to be updated on node selection
     */
    @XNode("@contentView")
    protected String contentView;

    /**
     * Name of the QueryModel field that will be used updated on node
     * selection.
     */
    @XNode("@field")
    protected String fieldName;

    /**
     * Name of the QueryModel schema for the field that will be used updated on
     * node selection.
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
        // check that each required directory exists and has the xvocabulary
        // schema
        DirectoryService directoryService = DirectoryHelper.getDirectoryService();
        if (directoryService == null) {
            log.warn("DirectoryService not ready, cannot check directories; skip validation");
        } else {
            boolean isFirst = true;
            for (String directoryName : directories) {
                Directory directory = directoryService.getDirectory(directoryName);
                if (directory == null) {
                    throw new DirectoryException(directoryName
                            + " is not a registered directory");
                }
                String directorySchema = directory.getSchema();
                if (isFirst) {
                    if (!(directorySchema.equals(VOCABULARY_SCHEMA) || directorySchema.equals(XVOCABULARY_SCHEMA))) {
                        throw new DirectoryException(directoryName
                                + "does not have the required schema: "
                                + VOCABULARY_SCHEMA + " or "
                                + XVOCABULARY_SCHEMA);
                    }
                } else {
                    if (!directorySchema.equals(XVOCABULARY_SCHEMA)) {
                        throw new DirectoryException(directoryName
                                + "does not have the required schema: "
                                + XVOCABULARY_SCHEMA);
                    }
                }
                isFirst = false;
            }
        }
        this.directories = directories;
    }

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

    /**
     * @deprecated use {@link #getContentView()} instead
     */
    @Deprecated
    public String getQuerymodel() {
        return querymodel;
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

    public void merge(DirectoryTreeDescriptor other) {

        if (other.schemaName != null) {
            this.schemaName = other.schemaName;
        }
        if (other.querymodel != null) {
            this.querymodel = other.querymodel;
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
    }

}
