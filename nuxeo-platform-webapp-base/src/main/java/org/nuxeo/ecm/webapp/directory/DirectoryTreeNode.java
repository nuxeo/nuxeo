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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
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

    protected boolean open = false;

    protected final DirectoryTreeDescriptor config;

    protected String identifier;

    protected String description;

    protected boolean leaf = false;

    protected String type = "defaultDirectoryTreeNode";

    protected DirectoryService directoryService;

    protected ContentView contentView;

    /**
     * @deprecated use {@link #contentView} instead
     */
    @Deprecated
    protected QueryModel queryModel;

    protected DocumentModelList childrenEntries;

    protected List<DirectoryTreeNode> children;

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

    protected List<String> processSelectedValuesOnMultiSelect(String value,
            List<String> values) {
        if (values.contains(value)) {
            values.remove(value);
        } else {
            // unselect all previous selection that are either more
            // generic or more specific
            List<String> valuesToRemove = new ArrayList<String>();
            String valueSlash = value + "/";
            for (String existingSelection : values) {
                String existingSelectionSlash = existingSelection + "/";
                if (existingSelectionSlash.startsWith(valueSlash)
                        || valueSlash.startsWith(existingSelectionSlash)) {
                    valuesToRemove.add(existingSelection);
                }
            }
            values.removeAll(valuesToRemove);

            // add the new selection
            values.add(value);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public String selectNode() throws ClientException {
        if (config.hasContentViewSupport()) {
            DocumentModel searchDoc = getContentViewSearchDocumentModel();
            if (searchDoc != null) {
                String fieldName = config.getFieldName();
                String schemaName = config.getSchemaName();
                if (config.isMultiselect()) {
                    List<String> values = (List<String>) searchDoc.getProperty(
                            schemaName, fieldName);
                    values = processSelectedValuesOnMultiSelect(path, values);
                    searchDoc.setProperty(schemaName, fieldName, values);
                } else {
                    searchDoc.setProperty(schemaName, fieldName, path);
                }
                if (contentView != null) {
                    contentView.refreshPageProvider();
                }
            } else {
                log.error("Cannot select node: search document model is null");
            }
        } else {
            lookupQueryModel();
            String fieldName = config.getFieldName();
            String schemaName = config.getSchemaName();
            if (config.isMultiselect()) {
                List<String> values = (List<String>) queryModel.getProperty(
                        schemaName, fieldName);
                values = processSelectedValuesOnMultiSelect(path, values);
                queryModel.setProperty(schemaName, fieldName, values);
            } else {
                queryModel.setProperty(schemaName, fieldName, path);
            }
            Events.instance().raiseEvent(EventNames.QUERY_MODEL_CHANGED,
                    queryModel);
        }
        // raise this event in order to reset the documents lists from
        // 'conversationDocumentsListsManager'
        Events.instance().raiseEvent(
                EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED,
                new DocumentModelImpl("Folder"));
        pathProcessing();
        return config.getOutcome();
    }

    @SuppressWarnings("unchecked")
    public boolean isSelected() throws ClientException {
        if (config.hasContentViewSupport()) {
            DocumentModel searchDoc = getContentViewSearchDocumentModel();
            if (searchDoc != null) {
                String fieldName = config.getFieldName();
                String schemaName = config.getSchemaName();
                if (config.isMultiselect()) {
                    List<Object> values = (List<Object>) searchDoc.getProperty(
                            schemaName, fieldName);
                    return values.contains(path);
                } else {
                    return path.equals(searchDoc.getProperty(schemaName,
                            fieldName));
                }
            } else {
                log.error("Cannot check if node is selected: "
                        + "search document model is null");
                return false;
            }
        } else {
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
    }

    /**
     * Returns true if current node is a parent from selected value(s).
     */
    public boolean isOpened() throws ClientException {
        if (config.isMultiselect()) {
            return isOpen();
        } else {
            if (config.hasContentViewSupport()) {
                DocumentModel searchDoc = getContentViewSearchDocumentModel();
                if (searchDoc != null) {
                    String fieldName = config.getFieldName();
                    String schemaName = config.getSchemaName();
                    Object value = searchDoc.getProperty(schemaName, fieldName);
                    if (value instanceof String) {
                        return ((String) value).startsWith(path);
                    }
                } else {
                    log.error("Cannot check if node is selected: "
                            + "search document model is null");
                }
            } else {
                lookupQueryModel();
                String fieldName = config.getFieldName();
                String schemaName = config.getSchemaName();
                Object value = queryModel.getProperty(schemaName, fieldName);
                if (value instanceof String) {
                    return ((String) value).startsWith(path);
                }
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
            // memorized directory lookup since directory content is not
            // suppose to change
            // XXX: use the cache manager instead of field caching strategy
            return childrenEntries;
        }
        Session session = getDirectorySession();
        try {
            if (level == 0) {
                String schema = getDirectorySchema();
                if (DirectoryTreeDescriptor.XVOCABULARY_SCHEMA.equals(schema)) {
                    // filter on empty parent
                    Map<String, Serializable> filter = new HashMap<String, Serializable>();
                    filter.put(PARENT_FIELD_ID, "");
                    childrenEntries = session.query(filter);
                } else {
                    childrenEntries = session.getEntries();
                }
            } else {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                String[] bitsOfPath = path.split("/");
                filter.put(PARENT_FIELD_ID, bitsOfPath[level - 1]);
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

    protected void lookupContentView() throws ClientException {
        if (contentView != null) {
            return;
        }
        SeamContextHelper seamContextHelper = new SeamContextHelper();
        ContentViewActions cva = (ContentViewActions) seamContextHelper.get("contentViewActions");
        contentView = cva.getContentView(config.getContentView());
        if (contentView == null) {
            throw new ClientException("no content view registered as "
                    + config.getContentView());
        }
    }

    protected DocumentModel getContentViewSearchDocumentModel()
            throws ClientException {
        lookupContentView();
        if (contentView != null) {
            return contentView.getSearchDocumentModel();
        }
        return null;
    }

    /**
     * @deprecated ise {@link #lookupContentView()} instead
     */
    @Deprecated
    protected void lookupQueryModel() throws ClientException {
        if (queryModel != null) {
            return;
        }
        SeamContextHelper seamContextHelper = new SeamContextHelper();
        QueryModelActions qma = (QueryModelActions) seamContextHelper.get("queryModelActions");
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
        if (config.isMultiselect()) {
            // no breadcrumbs management with multiselect
            return;
        }
        String aPath = null;
        if (config.hasContentViewSupport()) {
            try {
                DocumentModel searchDoc = getContentViewSearchDocumentModel();
                if (searchDoc != null) {
                    aPath = (String) searchDoc.getProperty(
                            config.getSchemaName(), config.getFieldName());
                } else {
                    log.error("Cannot perform path preprocessing: "
                            + "search document model is null");
                }
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        } else if (queryModel != null) {
            aPath = (String) queryModel.getProperty(config.getSchemaName(),
                    config.getFieldName());
        }
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
                try {
                    // take first schema: directory entries only have one
                    final String schemaName = docMod.getSchemas()[0];
                    property = (String) docMod.getProperty(schemaName,
                            LABEL_FIELD_ID);
                } catch (ClientException e) {
                    throw new DirectoryException(e);
                }
                myPath = myPath + property + '/';

                session.close();
            }
            Events.instance().raiseEvent("PATH_PROCESSED", myPath);

        } else {
            Events.instance().raiseEvent("PATH_PROCESSED", "");
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

}
