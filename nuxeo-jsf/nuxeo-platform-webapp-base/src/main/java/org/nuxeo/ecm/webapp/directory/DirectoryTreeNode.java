/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: DirectoryTreeNode.java 29611 2008-01-24 16:51:03Z gracinet $
 */
package org.nuxeo.ecm.webapp.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.tree.TreeActions;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.runtime.api.Framework;

/**
 * Register directory tree configurations to make them available to the DirectoryTreeManagerBean to build
 * DirectoryTreeNode instances.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class DirectoryTreeNode {

    private static final Log log = LogFactory.getLog(DirectoryTreeNode.class);

    public static final String PARENT_FIELD_ID = "parent";

    private static final String LABEL_FIELD_ID = "label";

    protected final String path;

    protected final int level;

    protected Boolean open = null;

    protected final DirectoryTreeDescriptor config;

    protected String identifier;

    protected String description;

    protected boolean leaf = false;

    protected String type = "defaultDirectoryTreeNode";

    protected DirectoryService directoryService;

    protected ContentView contentView;

    protected DocumentModelList childrenEntries;

    protected List<DirectoryTreeNode> children;

    public DirectoryTreeNode(int level, DirectoryTreeDescriptor config, String identifier, String description,
            String path, DirectoryService directoryService) {
        this.level = level;
        this.config = config;
        this.identifier = identifier;
        this.description = description;
        this.path = path;
        this.directoryService = directoryService;
    }

    protected List<String> processSelectedValuesOnMultiSelect(String value, List<String> values) {
        if (values.contains(value)) {
            values.remove(value);
        } else {
            // unselect all previous selection that are either more
            // generic or more specific
            List<String> valuesToRemove = new ArrayList<String>();
            String valueSlash = value + "/";
            for (String existingSelection : values) {
                String existingSelectionSlash = existingSelection + "/";
                if (existingSelectionSlash.startsWith(valueSlash) || valueSlash.startsWith(existingSelectionSlash)) {
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
    public String selectNode() {
        if (config.hasContentViewSupport()) {
            DocumentModel searchDoc = getContentViewSearchDocumentModel();
            if (searchDoc != null) {
                String fieldName = config.getFieldName();
                String schemaName = config.getSchemaName();
                if (config.isMultiselect()) {
                    List<String> values = (List<String>) searchDoc.getProperty(schemaName, fieldName);
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
            log.error(String.format("Cannot select node on tree '%s': no content view available", identifier));
        }
        // raise this event in order to reset the documents lists from
        // 'conversationDocumentsListsManager'
        Events.instance().raiseEvent(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED,
                DocumentModelFactory.createDocumentModel("Folder"));
        pathProcessing();
        return config.getOutcome();
    }

    @SuppressWarnings("unchecked")
    public boolean isSelected() {
        if (config.hasContentViewSupport()) {
            DocumentModel searchDoc = getContentViewSearchDocumentModel();
            if (searchDoc != null) {
                String fieldName = config.getFieldName();
                String schemaName = config.getSchemaName();
                if (config.isMultiselect()) {
                    List<Object> values = (List<Object>) searchDoc.getProperty(schemaName, fieldName);
                    return values.contains(path);
                } else {
                    return path.equals(searchDoc.getProperty(schemaName, fieldName));
                }
            } else {
                log.error("Cannot check if node is selected: " + "search document model is null");
            }
        } else {
            log.error(String.format("Cannot check if node is selected on tree '%s': no " + "content view available",
                    identifier));
        }
        return false;
    }

    public int getChildCount() {
        if (isLastLevel()) {
            return 0;
        }
        return getChildrenEntries().size();
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
        String schema = getDirectorySchema();
        DocumentModelList results = getChildrenEntries();
        FacesContext context = FacesContext.getCurrentInstance();
        for (DocumentModel result : results) {
            String childIdendifier = result.getId();
            String childDescription = translate(context, (String) result.getProperty(schema, LABEL_FIELD_ID));
            String childPath;
            if ("".equals(path)) {
                childPath = childIdendifier;
            } else {
                childPath = path + '/' + childIdendifier;
            }
            children.add(new DirectoryTreeNode(level + 1, config, childIdendifier, childDescription, childPath,
                    getDirectoryService()));
        }

        // sort children
        Comparator<? super DirectoryTreeNode> cmp = new FieldComparator();
        Collections.sort(children, cmp);

        return children;
    }

    private class FieldComparator implements Comparator<DirectoryTreeNode> {

        @Override
        public int compare(DirectoryTreeNode o1, DirectoryTreeNode o2) {
            return ObjectUtils.compare(o1.getDescription(), o2.getDescription());
        }
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

    protected DocumentModelList getChildrenEntries() {
        if (childrenEntries != null) {
            // memorized directory lookup since directory content is not
            // suppose to change
            // XXX: use the cache manager instead of field caching strategy
            return childrenEntries;
        }
        try (Session session = getDirectorySession()) {
            if (level == 0) {
                String schemaName = getDirectorySchema();
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                Schema schema = schemaManager.getSchema(schemaName);
                if (schema.hasField(PARENT_FIELD_ID)) {
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
        }
    }

    public String getDescription() {
        if (level == 0) {
            return translate(FacesContext.getCurrentInstance(), description);
        }
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

    protected String getDirectoryName() {
        String name = config.getDirectories()[level];
        if (name == null) {
            throw new NuxeoException("could not find directory name for level=" + level);
        }
        return name;
    }

    protected String getDirectorySchema() {
        return getDirectoryService().getDirectorySchema(getDirectoryName());
    }

    protected Session getDirectorySession() {
        return getDirectoryService().open(getDirectoryName());
    }

    protected void lookupContentView() {
        if (contentView != null) {
            return;
        }
        SeamContextHelper seamContextHelper = new SeamContextHelper();
        ContentViewActions cva = (ContentViewActions) seamContextHelper.get("contentViewActions");
        contentView = cva.getContentView(config.getContentView());
        if (contentView == null) {
            throw new NuxeoException("no content view registered as " + config.getContentView());
        }
    }

    protected DocumentModel getContentViewSearchDocumentModel() {
        lookupContentView();
        if (contentView != null) {
            return contentView.getSearchDocumentModel();
        }
        return null;
    }

    protected boolean isLastLevel() {
        return config.getDirectories().length == level;
    }

    public void pathProcessing() {
        if (config.isMultiselect()) {
            // no breadcrumbs management with multiselect
            return;
        }
        String aPath = null;
        if (config.hasContentViewSupport()) {
            DocumentModel searchDoc = getContentViewSearchDocumentModel();
            if (searchDoc != null) {
                aPath = (String) searchDoc.getProperty(config.getSchemaName(), config.getFieldName());
            } else {
                log.error("Cannot perform path preprocessing: " + "search document model is null");
            }
        }
        if (StringUtils.isNotEmpty(aPath)) {
            String[] bitsOfPath = aPath.split("/");
            String myPath = "";
            String property = "";
            for (int b = 0; b < bitsOfPath.length; b++) {
                String dirName = config.getDirectories()[b];
                if (dirName == null) {
                    throw new DirectoryException("Could not find directory name for key=" + b);
                }
                try (Session session = getDirectoryService().open(dirName)) {
                    DocumentModel docMod = session.getEntry(bitsOfPath[b]);
                    try {
                        // take first schema: directory entries only have one
                        final String schemaName = docMod.getSchemas()[0];
                        property = (String) docMod.getProperty(schemaName, LABEL_FIELD_ID);
                    } catch (PropertyException e) {
                        throw new DirectoryException(e);
                    }
                    myPath = myPath + property + '/';
                }
            }
            Events.instance().raiseEvent("PATH_PROCESSED", myPath);
        } else {
            Events.instance().raiseEvent("PATH_PROCESSED", "");
        }
    }

    /**
     * @deprecated since 6.0, use {@link #isOpen()} instead
     */
    @Deprecated
    public boolean isOpened() {
        return isOpen();
    }

    public boolean isOpen() {
        if (open == null) {
            final TreeActions treeActionBean = (TreeActionsBean) Component.getInstance("treeActions");
            if (!treeActionBean.isNodeExpandEvent()) {
                if (!config.isMultiselect() && config.hasContentViewSupport()) {
                    DocumentModel searchDoc = getContentViewSearchDocumentModel();
                    if (searchDoc != null) {
                        String fieldName = config.getFieldName();
                        String schemaName = config.getSchemaName();
                        Object value = searchDoc.getProperty(schemaName, fieldName);
                        if (value instanceof String) {
                            open = Boolean.valueOf(((String) value).startsWith(path));
                        }
                    } else {
                        log.error("Cannot check if node is opened: " + "search document model is null");
                    }
                } else {
                    log.error(String.format("Cannot check if node is opened on tree '%s': no "
                            + "content view available", identifier));
                }
            }
        }
        return Boolean.TRUE.equals(open);
    }

    public void setOpen(boolean open) {
        this.open = Boolean.valueOf(open);
    }

}
