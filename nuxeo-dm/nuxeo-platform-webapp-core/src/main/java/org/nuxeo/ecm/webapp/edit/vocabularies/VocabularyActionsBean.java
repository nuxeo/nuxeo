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
 * $Id$
 */

package org.nuxeo.ecm.webapp.edit.vocabularies;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.platform.ui.web.directory.VocabularyEntry;
import org.nuxeo.ecm.webapp.directory.DirectoryUIActionsBean;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This class is the implementation of an action listener that provides methods
 * for the management of vocabularies.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 * @deprecated use {@link DirectoryUIActionsBean}
 */
@Name("vocabularyActions")
@Scope(CONVERSATION)
@Deprecated
public class VocabularyActionsBean implements VocabularyActions {

    private static final long serialVersionUID = -257772806500630093L;

    private static final Log log = LogFactory.getLog(VocabularyActionsBean.class);

    public static final String NULL_MARKER = "__NULL__";

    private boolean showAddEntryForm = false;

    private boolean editable = false;

    private String style;

    private String commandName1;

    private String commandName2;

    private VocabularyEntry searchCriteria;

    private String title;

    private transient DirectoryService dirService;

    private List<SelectItem> parentIds;

    @DataModel
    private List<String> vocabularyNames;

    @DataModelSelection(value = "vocabularyNames")
    private String selectedVocabularyName;

    // serialization of VocabularyEntry isn't a problem
    @DataModel
    private List<VocabularyEntry> selectedVocabularyEntries;

    @DataModelSelection(value = "selectedVocabularyEntries")
    protected VocabularyEntry selectedVocabularyEntry;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @Begin(join = true)
    @Create
    public void initialize() {
        initDirService();
        // searchCriteria = getEmptyVocabularyEntry();
    }

    private void initDirService() {
        if (dirService == null) {
            dirService = DirectoryHelper.getDirectoryService();
            if (dirService == null) { // can't throw
                log.error("Failed to lookup directory service");
            }
        }
    }

    @Factory("selectedVocabularyEntries")
    protected void selectedVocabularyEntriesFactory() throws ClientException {
        editVocabulary();
    }

    public String editVocabulary() throws ClientException {
        showAddEntryForm = false;
        searchCriteria = getEmptyVocabularyEntry();
        return searchVocabularyEntries();
    }

    public String clearVocabulary() throws ClientException {

        Session vocabulary = null;
        String message = null;
        try {
            vocabulary = getCurrentVocabulary();
            if (vocabulary != null) {
                for (DocumentModel entry : vocabulary.getEntries()) {
                    vocabulary.deleteEntry(entry.getId());
                }
                vocabulary.commit();
                message = "vocabulary.cleared";
            }
        } finally {
            if (vocabulary != null) {
                vocabulary.close();
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        showAddEntryForm = false;
        return "view_vocabularies";
    }

    public String addVocabularyEntry() throws ClientException {
        Session vocabulary = null;
        String message = "vocabulary.entry.added";
        try {
            vocabulary = getCurrentVocabulary();
            if (vocabulary != null) {
                if (VocabularyConstants.COMMAND_EDIT.equals(commandName1)) {
                    // for an edit, delete the previous entry
                    if (isHierarchical()) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put(VocabularyConstants.VOCABULARY_PARENT,
                                selectedVocabularyEntry.getParent());
                        vocabulary.deleteEntry(selectedVocabularyEntry.getId(),
                                map);
                    } else {
                        vocabulary.deleteEntry(selectedVocabularyEntry.getId());
                    }
                    vocabulary.commit();
                    message = "vocabulary.entry.edited";
                }

                // check that the entry does not already exist
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put(VocabularyConstants.VOCABULARY_ID,
                        selectedVocabularyEntry.getId());
                if (isHierarchical()) {
                    filter.put(VocabularyConstants.VOCABULARY_PARENT,
                            selectedVocabularyEntry.getParent());
                }
                if (!vocabulary.query(filter).isEmpty()) {
                    facesMessages.addToControl(
                            "id",
                            FacesMessage.SEVERITY_INFO,
                            resourcesAccessor.getMessages().get(
                                    "vocabulary.entry.identifier.already.exists"));
                    return "view_vocabulary";
                }

                Map<String, Object> values = new HashMap<String, Object>();
                values.put(VocabularyConstants.VOCABULARY_ID,
                        selectedVocabularyEntry.getId());
                values.put(VocabularyConstants.VOCABULARY_LABEL,
                        selectedVocabularyEntry.getLabel());
                values.put(
                        VocabularyConstants.VOCABULARY_OBSOLETE,
                        Boolean.TRUE.equals(selectedVocabularyEntry.getObsolete()) ? 1L
                                : VocabularyConstants.DEFAULT_OBSOLETE);
                if (isHierarchical()) {
                    String parent = selectedVocabularyEntry.getParent();
                    if ("".equals(parent)) {
                        parent = null;
                    }
                    values.put(VocabularyConstants.VOCABULARY_PARENT, parent);
                }
                values.put(
                        VocabularyConstants.VOCABULARY_ORDERING,
                        selectedVocabularyEntry.getOrdering() != null ? selectedVocabularyEntry.getOrdering()
                                : VocabularyConstants.DEFAULT_VOCABULARY_ORDER);
                vocabulary.createEntry(values);
                vocabulary.commit();
            } else {
                message = "";
            }

        } catch (UnsupportedOperationException e) {
            message = "vocabulary.entry.not.added";

        } finally {
            if (vocabulary != null) {
                vocabulary.close();
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        showAddEntryForm = false;
        return editVocabulary();
    }

    public String editVocabularyEntry() {
        editable = true;
        showAddEntryForm = true;
        commandName1 = VocabularyConstants.COMMAND_EDIT;
        commandName2 = VocabularyConstants.COMMAND_CANCEL;
        style = "none";
        return "view_vocabulary";
    }

    public String viewVocabularyEntry() {
        editable = false;
        showAddEntryForm = true;
        commandName2 = "command.ok";
        style = "notEditable";
        return "view_vocabulary";
    }

    public String deleteVocabularyEntry() throws ClientException {
        Session vocabulary = null;
        String message = null;
        try {
            vocabulary = getCurrentVocabulary();
            if (vocabulary != null) {
                if (isHierarchical()) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(VocabularyConstants.VOCABULARY_PARENT,
                            selectedVocabularyEntry.getParent());
                    vocabulary.deleteEntry(selectedVocabularyEntry.getId(), map);
                } else {
                    vocabulary.deleteEntry(selectedVocabularyEntry.getId());
                }
                vocabulary.commit();
                message = "vocabulary.entry.deleted";
            }
        } catch (UnsupportedOperationException e) {
            message = "vocabulary.entry.not.deleted";

        } finally {
            if (vocabulary != null) {
                vocabulary.close();
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        showAddEntryForm = false;
        return editVocabulary();
    }

    @Factory("vocabularyNames")
    protected void vocabularyNamesFactory() throws ClientException {
        viewAllVocabularies();
    }

    public String viewAllVocabularies() throws ClientException {

        vocabularyNames = new ArrayList<String>();
        for (String dirName : dirService.getDirectoryNames()) {
            String schema = dirService.getDirectorySchema(dirName);
            if (VocabularyConstants.VOCABULARY_TYPE_SIMPLE.equals(schema)
                    || VocabularyConstants.VOCABULARY_TYPE_HIER.equals(schema)) {
                vocabularyNames.add(dirName);
            }
        }
        Collections.sort(vocabularyNames, VocabularyComparator.INSTANCE);
        return "view_vocabularies";
    }

    private static class VocabularyComparator implements Comparator<String>,
            Serializable {

        private static final long serialVersionUID = -3178630590907764894L;

        // use locale?
        static final Collator collator = Collator.getInstance();

        static {
            collator.setStrength(Collator.PRIMARY); // case+accent independent
        }

        static final VocabularyComparator INSTANCE = new VocabularyComparator();

        public int compare(String d1, String d2) {
            return collator.compare(d1, d2);
        }
    }

    public String getSelectedVocabularyName() {
        return selectedVocabularyName;
    }

    public void setSelectedVocabularyName(String selectedVocabularyName) {
        this.selectedVocabularyName = selectedVocabularyName;
    }

    public VocabularyEntry getSelectedVocabularyEntry() {
        return selectedVocabularyEntry;
    }

    public void setSelectedVocabularyEntry(VocabularyEntry entry) {
        selectedVocabularyEntry = entry;
    }

    public void toggleAddEntryForm(ActionEvent event) {
        if (showAddEntryForm) {
            showAddEntryForm = false;
        } else {
            try {
                selectedVocabularyEntry = getEmptyVocabularyEntry();
            } catch (ClientException e) {
                log.debug(
                        "there was an error while instantiating a new vocabulary entry ",
                        e);
            }
            showAddEntryForm = true;
            editable = true;
            commandName1 = VocabularyConstants.COMMAND_ADD;
            commandName2 = VocabularyConstants.COMMAND_CANCEL;
            style = "none";
        }
    }

    public boolean getShowAddEntryForm() {
        return showAddEntryForm;
    }

    /**
     * @param showAddEntryForm The showAddEntryForm to set.
     */
    public void setShowAddEntryForm(boolean showAddEntryForm) {
        this.showAddEntryForm = showAddEntryForm;
    }

    public boolean getEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getCommandName1() {
        return commandName1;
    }

    public void setCommandName1(String commandName) {
        commandName1 = commandName;
    }

    public String getCommandName2() {
        return commandName2;
    }

    public void setCommandName2(String commandName) {
        commandName2 = commandName;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String cancel() {
        log.debug("cancel adding/editing vocabulary entry ...");
        selectedVocabularyEntry = null;
        showAddEntryForm = false;
        return "view_vocabulary";
    }

    private VocabularyEntry getVocabularyEntry(DocumentModel vocabularyEntry)
            throws DirectoryException {
        String schemaName = dirService.getDirectorySchema(selectedVocabularyName);
        VocabularyEntry result;
        try {
            result = new VocabularyEntry((String) vocabularyEntry.getProperty(
                    schemaName, VocabularyConstants.VOCABULARY_ID),
                    (String) vocabularyEntry.getProperty(schemaName,
                            VocabularyConstants.VOCABULARY_LABEL));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        try {
            result.setObsolete(((Long) vocabularyEntry.getProperty(schemaName,
                    VocabularyConstants.VOCABULARY_OBSOLETE)).intValue() == 0 ? Boolean.FALSE
                    : Boolean.TRUE);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        // TODO VocabularyEntry ordering should be changed to use a Long
        Integer ordering;
        try {
            ordering = ((Long) vocabularyEntry.getProperty(schemaName,
                    VocabularyConstants.VOCABULARY_ORDERING)).intValue();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        result.setOrdering(ordering.equals(VocabularyConstants.DEFAULT_VOCABULARY_ORDER) ? null
                : ordering);

        if (isHierarchical()) {
            try {
                result.setParent((String) vocabularyEntry.getProperty(
                        schemaName, VocabularyConstants.VOCABULARY_PARENT));
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        }
        return result;
    }

    private Session getCurrentVocabulary() throws ClientException {
        return dirService.open(selectedVocabularyName);
    }

    public VocabularyEntry getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(VocabularyEntry searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public String clearSearchCriteria() throws ClientException {
        searchCriteria = getEmptyVocabularyEntry();
        return searchVocabularyEntries();
    }

    public String searchVocabularyEntries() throws ClientException {
        Session vocabulary = null;
        String title = null;
        Set<String> parents = new TreeSet<String>(); // sorted
        try {
            vocabulary = getCurrentVocabulary();
            if (vocabulary != null) {
                selectedVocabularyEntries = new ArrayList<VocabularyEntry>();
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                Map<String, String> orderBy = new LinkedHashMap<String, String>();
                Set<String> fulltext = new HashSet<String>();
                if (!"".equals(searchCriteria.getId())) {
                    filter.put(VocabularyConstants.ID_COLUMN_SEARCH,
                            searchCriteria.getId());
                    fulltext.add(VocabularyConstants.ID_COLUMN_SEARCH);
                }
                if (!"".equals(searchCriteria.getLabel())) {
                    filter.put(VocabularyConstants.LABEL_COLUMN_SEARCH,
                            searchCriteria.getLabel());
                    fulltext.add(VocabularyConstants.LABEL_COLUMN_SEARCH);
                }
                String parent = searchCriteria.getParent();
                if (isHierarchical() && parent != null && !parent.equals("")) {
                    if (parent.equals(NULL_MARKER)) {
                        // allow search on null parent
                        parent = null;
                    }
                    filter.put(VocabularyConstants.PARENT_COLUMN_SEARCH, parent);
                    fulltext.add(VocabularyConstants.PARENT_COLUMN_SEARCH);
                }
                orderBy.put(VocabularyConstants.VOCABULARY_ORDERING,
                        VocabularyConstants.ORDER_ASC);
                orderBy.put(VocabularyConstants.VOCABULARY_ID,
                        VocabularyConstants.ORDER_ASC);
                DocumentModelList vocabularyEntries = vocabulary.query(filter,
                        fulltext, orderBy);
                for (DocumentModel entry : vocabularyEntries) {
                    VocabularyEntry vocabularyEntry = getVocabularyEntry(entry);
                    if (vocabularyEntry.getParent() != null) {
                        parents.add(vocabularyEntry.getParent());
                    }
                    selectedVocabularyEntries.add(vocabularyEntry);
                }
                title = selectedVocabularyEntries.isEmpty() ? "title.vocabulary.entries.empty"
                        : "title.vocabulary.entries";
            }
        } catch (SizeLimitExceededException e) {
            title = "".equals(searchCriteria) ? "title.vocabulary.entries.error"
                    : "title.vocabulary.entries.search.error";
            log.debug(
                    "The number of the entries of a vocabulary is 0 or greater that the accepted limit...",
                    e);
        } finally {
            if (vocabulary != null) {
                vocabulary.close();
            }
        }
        if (isHierarchical()) {
            parentIds = computeParentIds(parents);
        } else {
            parentIds = null;
        }
        showAddEntryForm = false;
        this.title = title;
        searchCriteria = getEmptyVocabularyEntry();
        return "view_vocabulary";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<SelectItem> getParentIds() {
        return parentIds;
    }

    protected List<SelectItem> computeParentIds(Set<String> parents)
            throws ClientException {
        Map<String, String> messages = resourcesAccessor.getMessages();
        List<SelectItem> parentIds = new ArrayList<SelectItem>(
                parents.size() + 2);
        // add empty choice to search for anything
        parentIds.add(new SelectItem("",
                messages.get("vocabulary.entry.any.selectitem")));
        // null parent
        if (isNullParentAllowed()) {
            parentIds.add(new SelectItem(NULL_MARKER,
                    messages.get("vocabulary.entry.root.selectitem")));
        }
        // normal parents (sorted set)
        for (String parentId : parents) {
            parentIds.add(new SelectItem(parentId, parentId));
        }
        return parentIds;
    }

    protected String getVocabularyName(Directory voc) {
        try {
            return voc.getName();
        } catch (DirectoryException e) { // quite surprising
            log.error("Could not retrieve vocabulary name !", e);
            return null;
        }
    }

    protected Session getVocabularyByName(String name) {
        try {
            return dirService.open(name);
        } catch (DirectoryException e) {
            log.error("Failed to lookup directory '" + name + "'", e);
            return null;
        }
    }

    @PrePassivate
    public void saveState() {
        log.debug("Saving state before passivation");
    }

    @PostActivate
    public void restoreState() {
        log.debug("Restoring state upon activation");
        initDirService();
        if (dirService == null) {
            log.error("Could not lookup directory service");
            return; // avoid NPEs ...for now
        }
    }

    public boolean isHierarchical() throws DirectoryException {
        return VocabularyConstants.VOCABULARY_TYPE_HIER.equals(dirService.getDirectorySchema(selectedVocabularyName));
    }

    public boolean isNullParentAllowed() throws DirectoryException {
        // only allow a null parent if we have a self-referential hierarchical
        // vocabulary
        if (selectedVocabularyName == null) {
            return false;
        }
        return selectedVocabularyName.equals(dirService.getParentDirectoryName(selectedVocabularyName));
    }

    private VocabularyEntry getEmptyVocabularyEntry() throws ClientException {
        String parent;
        if (isHierarchical()) {
            parent = "";
        } else {
            parent = null;
        }
        return new VocabularyEntry("", "", parent);
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing SEAM component: vocabularyActions");
    }

}
