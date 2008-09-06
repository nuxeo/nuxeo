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
 * $Id: DirectoryTreeNode.java 29611 2008-01-24 16:51:03Z gracinet $
 */
package org.nuxeo.ecm.webapp.directory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

/**
 * Register directory tree configurations to make them available to the
 * DirectoryTreeManagerBean to build DirectoryTreeNode instances.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class DirectoryTreeNode {

    private static final Log log = LogFactory.getLog(DirectoryTreeNode.class);

    private static final String PARENT_FIELD_ID = "parent";

    private static final String LABEL_FIELD_ID = "label";

    protected final String path;

    protected final int level;

    protected final DirectoryTreeDescriptor config;

    protected String identifier;

    protected String description;

    protected boolean leaf = false;

    protected String type = "defaultDirectoryTreeNode";

    protected DirectoryService directoryService;

    private QueryModel queryModel;

    private DocumentModelList childrenEntries;

    private List<DirectoryTreeNode> children;

    public DirectoryTreeNode(int level, DirectoryTreeDescriptor config,
            String identifier, String description, String path,
            DirectoryService directoryService) {
        this.level = level;
        this.config = config;
        this.identifier = identifier;
        this.description = description;
        this.path = path;
        this.directoryService = directoryService;
    }

    @SuppressWarnings("unchecked")
    public String selectNode() throws ClientException {
        lookupQueryModel();
        String fieldName = config.getFieldName();
        String schemaName = config.getSchemaName();
        String value = path;
        if (config.isMultiselect()) {
            List<Object> values = (List<Object>) queryModel.getProperty(
                    schemaName, fieldName);
            if (!values.contains(value)) {
                values.add(value);
                queryModel.setProperty(schemaName, fieldName, values);
                Events.instance().raiseEvent(EventNames.QUERY_MODEL_CHANGED,
                        queryModel);
            }
        } else {
            queryModel.setProperty(schemaName, fieldName, value);
            Events.instance().raiseEvent(EventNames.QUERY_MODEL_CHANGED,
                    queryModel);
        }
        pathProcessing();
        return config.getOutcome();
    }

    @SuppressWarnings("unchecked")
    public boolean isSelected() throws ClientException {
        lookupQueryModel();
        String fieldName = config.getFieldName();
        String schemaName = config.getSchemaName();
        if (config.isMultiselect()) {
            List<Object> values = (List<Object>) queryModel.getProperty(
                    schemaName, fieldName);
            return values.contains(path);
        } else {
            return path.equals(queryModel.getProperty(schemaName, fieldName));
        }
    }

    /**
     * Returns true if current node is a parent from selected value(s).
     */
    @SuppressWarnings("unchecked")
    public boolean isOpened() throws ClientException {
        lookupQueryModel();
        String fieldName = config.getFieldName();
        String schemaName = config.getSchemaName();
        if (config.isMultiselect()) {
            List<Object> values = (List<Object>) queryModel.getProperty(
                    schemaName, fieldName);
            for (Object value : values) {
                if (value instanceof String
                        && ((String) value).startsWith(path)) {
                    return true;
                }
            }
        } else {
            Object value = queryModel.getProperty(schemaName, fieldName);
            if (value instanceof String) {
                return ((String) value).startsWith(path);
            }
        }
        return false;
    }

    public int getChildCount() {
        if (isLastLevel()) {
            return 0;
        }
        try {
            return getChildrenEntries().size();
        } catch (ClientException e) {
            log.error(e);
            return 0;
        }
    }

    public List<DirectoryTreeNode> getChildren() {
        if (children != null) {
            // return last computed state
            return children;
        }
        children = new ArrayList<DirectoryTreeNode>();
        if (isLastLevel()) {
            return children;
        }
        try {
            String schema = getDirectorySchema();
            DocumentModelList results = getChildrenEntries();
            for (DocumentModel result : results) {
                String childIdendifier = result.getId();
                String childDescription = (String) result.getProperty(schema,
                        LABEL_FIELD_ID);
                String childPath;
                if ("".equals(path)) {
                    childPath = childIdendifier;
                } else {
                    childPath = path + '/' + childIdendifier;
                }
                children.add(new DirectoryTreeNode(level + 1, config,
                        childIdendifier, childDescription, childPath,
                        getDirectoryService()));
            }
            return children;
        } catch (ClientException e) {
            log.error(e);
            return children;
        }
    }

    protected DocumentModelList getChildrenEntries() throws ClientException {
        if (childrenEntries != null) {
            // memorized directory lookup since directory content is not suppose
            // to change
            // XXX: use the cache manager instead of field caching strategy
            return childrenEntries;
        }
        Session session = getDirectorySession();
        try {
            if (level == 0) {
                childrenEntries = session.getEntries();
            } else {
                Map<String, Object> filter = new HashMap<String, Object>();
                filter.put(PARENT_FIELD_ID, path);
                childrenEntries = session.query(filter);
            }
            return childrenEntries;
        } finally {
            session.close();
        }
    }

    public String getDescription() {
        return description;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public boolean isLeaf() {
        return leaf || isLastLevel() || getChildCount() == 0;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public void setType(String type) {
        this.type = type;
    }

    protected DirectoryService getDirectoryService() {
        if (directoryService == null) {
            directoryService = DirectoryHelper.getDirectoryService();
        }
        return directoryService;
    }

    protected String getDirectoryName() throws ClientException {
        String name = config.getDirectories()[level];
        if (name == null) {
            throw new ClientException(
                    "could not find directory name for level=" + level);
        }
        return name;
    }

    protected String getDirectorySchema() throws ClientException {
        return getDirectoryService().getDirectorySchema(getDirectoryName());
    }

    protected Session getDirectorySession() throws ClientException {
        return getDirectoryService().open(getDirectoryName());
    }

    protected void lookupQueryModel() throws ClientException {
        if (queryModel != null) {
            return;
        }
        QueryModelActions qma = (QueryModelActions) Contexts.lookupInStatefulContexts("queryModelActions");
        queryModel = qma.get(config.getQuerymodel());
        if (queryModel == null) {
            throw new ClientException("no query model registered as "
                    + config.getQuerymodel());
        }
    }

    protected boolean isLastLevel() {
        return config.getDirectories().length == level;
    }

    public void pathProcessing() throws DirectoryException {

        String aPath = (String) queryModel.getProperty(config.getSchemaName(),
                config.getFieldName());
        if (aPath != null && aPath != "") {
            String[] bitsOfPath = aPath.split("/");
            String myPath = "";
            String property = "";
            for (int b = 0; b < bitsOfPath.length; b++) {
                String dirName = config.getDirectories()[b];
                if (dirName == null) {
                    throw new DirectoryException(
                            "Could not find directory name for key=" + b);
                }
                Session session = getDirectoryService().open(dirName);
                DocumentModel docMod = session.getEntry(bitsOfPath[b]);
                if (b == 0) {
                    property = (String) docMod.getProperty("vocabulary",
                            "label");
                } else {
                    property = (String) docMod.getProperty("xvocabulary",
                            "label");
                }
                myPath = myPath + property + '/';

                session.close();
            }
            Events.instance().raiseEvent("PATH_PROCESSED", myPath);

        } else {
            Events.instance().raiseEvent("PATH_PROCESSED", "");
        }
    }

}
