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
 *     ${user}
 *
 * $$Id$$
 */
package org.nuxeo.ecm.webapp.bulkupdate;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.nuxeo.common.utils.ArrayUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioning;

/**
 * Action bean for bulk editing of DocumentModels. The selected documents are
 * checked for a list of common fields that could be edited and a fictive
 * document model is created with the list of common schema fields from the
 * selected docs. This FictiveDocumentModel is used as a data backing bean for
 * the bulk editing JSF pages.
 *
 * @author <a href="mailto:dm@nuxeo.ro">Dragos Mihalache</a>
 */
@Name("massEditActions")
@Scope(ScopeType.CONVERSATION)
public class MassEditActionsBean extends InputController implements
        MassEditAction, Serializable {

    public static final String NAVIGATION_PREVIEW = "mass_edit_confirm";

    public static final String NAVIGATION_MASS_EDIT = "mass_edit";

    private static final long serialVersionUID = 76543986285636L;

    private static final Log log = LogFactory.getLog(MassEditActionsBean.class);

    private static final String SELECT_ITEM_ID_EMPTY = "SELECT_ITEM_ID_EMPTY";

    private static final String SELECT_ITEM_ID_ALL = "SELECT_ITEM_ID_ALL";

    /** resource bundle keys for translations of 'EMPTY' and 'ALL'. */
    private static final String RB_KEY_TEXT_EMPTY = "label.bulkedit.cbvalue.EMPTY";

    private static final String RB_KEY_TEXT_ALL = "label.bulkedit.cbvalue.ALL";

    protected Map<String, Boolean> chainSelectMap;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient ClipboardActions clipboardActions;

    @In(create = true)
    protected DocumentVersioning documentVersioning;

    @Out(required = false)
    FictiveDocumentModel fictiveDocumentModel;

    @Out(required = false)
    DocumentModel changeCheckboxes;

    @Out(required = false)
    DocumentModel currentFieldValues;

    @Out(required = false)
    DocumentModel docModelExistingSelect;

    @Out(required = false)
    DocumentModel docModelExistingSelectVerbose;

    @DataModelSelection
    private DocumentModel selectedDM;

    private List<DocumentModel> docsList;

    private Map<String, Set<String>> changingFields;

    private List<DocumentModel> changingDocuments;

    private List<DocumentModel> unchangingDocuments;

    /**
     * This will be set from the preview page if the user wants to remove saved
     * documents from the selection list after succesfully saving them.
     */
    private boolean removeFromList = true;

    @Begin(nested = true)
    public String putSelectionInWorkList() throws ClientException {
        // Redirect.instance().captureCurrentRequest();
        final String logPrefix = "<putSelectionInWorkList> ";

        if (documentsListsManager.isWorkingListEmpty(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            log.debug(logPrefix
                    + "No selectable Documents in context for mass edition.");
            return null;
        }
        final List<DocumentModel> selectedDocs = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        setDocumentsList(selectedDocs);

        log.debug(logPrefix + "add to worklist processed...");

        return NAVIGATION_MASS_EDIT;
    }

    /**
     * Called by JSF component.
     */
    @DataModel
    public List<DocumentModel> getDocumentsList() {
        return docsList;
    }

    public void setDocumentsList(List<DocumentModel> docsList)
            throws ClientException {
        final String logPrefix = "<setDocumentsList> ";

        log.debug(logPrefix + "setting " + docsList.size() + " documents.");

        //
        // re-retrieve docsList because the given list might be composed
        // of other implementations of DocumentModel which could have
        // extra schemas/properties (this is the case when selecting from
        // a search result)
        //
        this.docsList = new ArrayList<DocumentModel>();
        for (DocumentModel documentModel : docsList) {
            DocumentRef docRef = documentModel.getRef();
            if (docRef == null) {
                log.error("null DocumentRef for: " + documentModel);
            }
            final DocumentModel realDocModel = documentManager.getDocument(docRef);
            this.docsList.add(realDocModel);
        }

        fictiveDocumentModel = new FictiveDocumentModel();

        final String[] schemas = getCommonSchemas(docsList);

        // very important
        // add schemas to FictiveDocumentModel object
        fictiveDocumentModel.registerSchemas(schemas);

        changeCheckboxes = new FictiveDocumentModel();
        currentFieldValues = new FictiveDocumentModel();
        docModelExistingSelect = new FictiveDocumentModel();
        docModelExistingSelectVerbose = new FictiveDocumentModel();

        changingDocuments = new ArrayList<DocumentModel>();
        unchangingDocuments = new ArrayList<DocumentModel>();
    }

    public FieldWidget[] getCommonLayout() throws ClientException {
        final String logPrefix = "<getCommonLayout> ";
        // algorithm:
        // add field widgets from the first document
        // and exclude the ones not found in the other documents

        if (docsList == null) {
            log.debug(logPrefix + "docsList is null");
            return new FieldWidget[0];
        }

        return getCommonWidgets(docsList);
    }

    public FieldWidget[] getChangeLayout() throws ClientException {
        FieldWidget[] commonWidgets = getCommonLayout();
        if (commonWidgets.length == 0) {
            return commonWidgets;
        }

        Map<String, List<String>> checkboxes = new HashMap<String, List<String>>();

        final List<FieldWidget> filteredWidgetsList = new ArrayList<FieldWidget>();
        for (FieldWidget widget : commonWidgets) {
            final String jsfComp = widget.getJsfComponent();
            if (jsfComp.equals("t:inputFileUpload")) {
                // filter out
                continue;
            }

            // add auxiliary widgets along with this one
            String schemaName = widget.getSchemaName();
            String fieldName = widget.getFieldName();
            /*
             * List<String> schemaFields = checkboxes.get(schemaName); if
             * (schemaFields==null) { schemaFields = new ArrayList<String>();
             * checkboxes.put(schemaName, schemaFields); }
             * schemaFields.add(fieldName);
             */
            // changeCheckboxes.
            addExistingValues(schemaName, fieldName);

            filteredWidgetsList.add(widget);
        }

        // create

        FieldWidget[] filteredWidgets = new FieldWidget[filteredWidgetsList.size()];
        filteredWidgets = filteredWidgetsList.toArray(filteredWidgets);

        return filteredWidgets;
    }

    public boolean getMapFlag(String key) {
        return chainSelectMap.get(key);
    }

    /**
     *
     * @return
     * @throws ClientException
     */
    public FieldWidget[] getPreviewLayout() throws ClientException {
        FieldWidget[] commonWidgets = getCommonLayout();
        final List<FieldWidget> filteredWidgetsList = new ArrayList<FieldWidget>();
        for (FieldWidget widget : commonWidgets) {
            String schemaName = widget.getSchemaName();
            String fieldName = widget.getFieldName();

            Set<String> props = changingFields.get(schemaName);
            if (props != null) {
                if (props.contains(fieldName)) {
                    filteredWidgetsList.add(widget);
                }
            }
        }

        FieldWidget[] filteredWidgets = new FieldWidget[filteredWidgetsList.size()];
        filteredWidgets = filteredWidgetsList.toArray(filteredWidgets);

        return filteredWidgets;
    }

    public List<DocumentModel> getChangingDocuments() {
        return changingDocuments;
    }

    public List<DocumentModel> getUnchangingDocuments() {
        return unchangingDocuments;
    }

    /**
     * Adds existing values from the selected documents to a list from which the
     * user will select a value that he/she wants to be changed only.
     *
     * @param schemaName
     * @param fieldName
     */
    private void addExistingValues(String schemaName, String fieldName) {
        final List<SelectItem> existingValues = new ArrayList<SelectItem>();

        // for any value
        final String text_ALL = resourcesAccessor.getMessages().get(
                RB_KEY_TEXT_ALL);
        SelectItem itemAll = new SelectItem(SELECT_ITEM_ID_ALL, text_ALL);
        existingValues.add(itemAll);

        // for fields which have empty values
        final String text_EMPTY = resourcesAccessor.getMessages().get(
                RB_KEY_TEXT_EMPTY);
        SelectItem itemEmpty = new SelectItem(SELECT_ITEM_ID_EMPTY, text_EMPTY);
        existingValues.add(itemEmpty);

        final Set addedItemIds = new HashSet();

        for (DocumentModel doc : docsList) {

            Object exValue = doc.getProperty(schemaName, fieldName);
            // existingValues.add(exValue);

            if (exValue != null) {
                Object id = exValue;
                String label;
                // display label differently based on property value type
                if (exValue instanceof String) {
                    label = (String) exValue;
                } else if (exValue instanceof Calendar) {
                    Calendar date = (Calendar) exValue;
                    // String format = ???
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    label = df.format(date.getTime());
                    // timestamp
                    id = date.getTimeInMillis();
                } else if (exValue instanceof String[]) {
                    String[] currentValueList = (String[]) exValue;
                    label = Arrays.asList(currentValueList).toString();
                    id = label;
                } else {
                    // TODO handle the other types
                    log.warn("unknown type for property (" + schemaName + ':'
                            + fieldName + "= " + exValue);
                    label = exValue.toString();
                }

                if (!addedItemIds.contains(id)) {

                    // truncate label if too long
                    if (label.length() > 30) {
                        label = label.substring(0, 27) + "...";
                    }

                    SelectItem item = new SelectItem(id, label);
                    existingValues.add(item);
                    addedItemIds.add(id);
                }
            }
        }
        currentFieldValues.setProperty(schemaName, fieldName, existingValues);
    }

    /**
     * Returns an intersection of schemas declared for given documents.
     *
     * @param docsList
     * @return
     */
    private static String[] getCommonSchemas(List<DocumentModel> docsList) {
        // TODO optimize this: cache the schemas list

        final String[][] docsSchemas = new String[docsList.size()][];
        int i = 0;
        for (DocumentModel docModel : docsList) {
            docsSchemas[i] = docModel.getDeclaredSchemas();
            i++;
        }

        return ArrayUtils.intersect(docsSchemas);
    }

    private FieldWidget[] getCommonWidgets(List<DocumentModel> docsList)
            throws ClientException {

        assert null != docsList;

        final FieldWidget[][] fieldWidgets = new FieldWidget[docsList.size()][];
        int i = 0;
        for (DocumentModel docModel : docsList) {
            Type t = getDocumentType(docModel);

            fieldWidgets[i] = t.getLayout();
            log.debug("Doc type: " + t.getId() + ", fieldWidgets[" + i
                    + "]= " + Arrays.asList(fieldWidgets[i]));
            i++;
        }

        final FieldWidget[] commonFieldWidgets = ArrayUtils.intersect(fieldWidgets);

        log.debug("common fieldWidgets: " + Arrays.asList(commonFieldWidgets));
        // make fields non-required
        for (FieldWidget widget : commonFieldWidgets) {
            widget.setRequired(false);
        }

        return commonFieldWidgets;
    }

    private Type getDocumentType(DocumentModel docModel) throws ClientException {
        Type platformType = typeManager.getType(docModel.getType());
        if (null == platformType) {
            // TODO maybe define a ConfigurationException
            throw new ClientException(
                    "Platform type not defined for document type: "
                            + docModel.getType());
        }
        return platformType;
    }

    /**
     * Action method called from JSF.
     *
     * @return
     */
    public String previewChanges() throws ClientException {
        // impacted docs...

        if (docsList == null || docsList.isEmpty()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "message.bulkedit.emptyList"));
            // log error as this situation is not normal
            log.error("empty list for bulk edit preview");
            return null;
        }

        final int docsCount = docsList.size();

        final String[] schemas = getCommonSchemas(docsList);

        changingDocuments.clear();
        unchangingDocuments.clear();

        changingFields = new HashMap<String, Set<String>>();
        for (DocumentModel docModel : docsList) {
            // copy selected fields only
            copyDocData(fictiveDocumentModel, docModel, schemas, true);
        }

        log.debug("will change " + changingDocuments.size()
                + " documents from " + docsCount);

        return NAVIGATION_PREVIEW;
    }

    /**
     * For now it just goes back.
     *
     * @return
     */
    public String cancelChanges() {
        return NAVIGATION_MASS_EDIT;
    }

    /**
     * Action method called from JSF.
     *
     * @see MassEditAction#updateDocuments()
     */
    @End
    public String updateDocuments() throws ClientException {
        final String logPrefix = "<updateDocuments> ";

        final int docsCount = changingDocuments.size();

        log.debug(logPrefix + "updating " + docsCount + " documents");

        final String[] schemas = getCommonSchemas(docsList);

        for (DocumentModel docModel : changingDocuments) {
            // copy selected fields only

            boolean canWrite = documentManager.hasPermission(docModel.getRef(),
                    SecurityConstants.WRITE)
                    && !docModel.hasFacet("Immutable");
            if (!canWrite) {
                String titleOrId = docModel.getTitle();
                if (titleOrId == null) {
                    titleOrId = docModel.getId();
                }
                facesMessages.add(
                        FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "feedback.documents_update_error.writePermDenied"),
                        titleOrId);
                return null;
            }

            copyDocData(fictiveDocumentModel, docModel, schemas, false);

            // NXP-1236 : apply default incrementation option for each doc
            VersioningActions optionId = VersioningActions.ACTION_INCREMENT_DEFAULT;
            documentVersioning.setVersioningOptionInstanceId(docModel, optionId);
        }

        DocumentModel[] documents = new DocumentModel[docsCount];
        documents = changingDocuments.toArray(documents);

        documentManager.saveDocuments(documents);
        documentManager.save();

        for (DocumentModel docModel : docsList) {
            eventManager.raiseEventsOnDocumentChange(docModel);
        }

        log.debug(logPrefix + "documents updated ");

        // data saved successfully, we can remove docs from the list now
        if (removeFromList) {
            for (DocumentModel docModel : changingDocuments) {
                // copy selected fields only
                documentsListsManager.removeFromWorkingList(
                        DocumentsListsManager.CURRENT_DOCUMENT_SELECTION,
                        docModel);
            }
        }

        // display message
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(
                        "feedback.documents_updated"));
        // return navigationContext.navigateToDocument(changeableDocument,
        // "after-create");

        // Conversation.instance().endAndRedirect();

        return null;
    }

    private boolean isFieldSelected(String schemaName, String propName) {
        return (Boolean) changeCheckboxes.getProperty(schemaName, propName);
    }

    /**
     * Copies data from srcDocModel to destDocModel for the given schemas.
     *
     * @param srcDocModel
     * @param destDocModel
     * @param schemas
     */
    private void copyDocData(DocumentModel srcDocModel,
            DocumentModel destDocModel, String[] schemas, boolean previewOnly) {
        final String logPrefix = "<copyDocData> ";

        boolean docToBeUpdated = false;

        // copy only non-null values
        for (String schemaName : schemas) {
            final Map<String, Object> data = srcDocModel.getProperties(schemaName);

            if (null == data) {
                // the fictive document should not have empty dataModels...

                log.warn(logPrefix + "null data for schema name: " + schemaName);
                continue;
            }

            if (data.isEmpty()) {
                log.debug(logPrefix
                        + "empty data for schema name: "
                        + schemaName
                        + ". Check declared schema on document layout for type: "
                        + destDocModel.getType());
                continue;
            }

            for (String propName : data.keySet()) {
                Object value = data.get(propName);
                if (value != null) {
                    if (value instanceof String) {
                        // if empty do not set
                        // XXX: is this logic ok?
                        // NXGED-809
                        // if (((String) value).trim().length() == 0) {
                        // log.debug(logPrefix + "skip empty val prop " +
                        // schemaName
                        // + ':' + propName);
                        // continue;
                        // }
                    }

                    // skip if not selected
                    if (!isFieldSelected(schemaName, propName)) {
                        log.debug(logPrefix + "skip not selected prop "
                                + schemaName + ':' + propName);
                        continue;
                    }

                    if (!isSelectedValueMatching(destDocModel, schemaName,
                            propName)) {
                        log.debug(logPrefix + "skip value not matching for "
                                + srcDocModel.getTitle() + '.' + schemaName
                                + ':' + propName);
                        continue;
                    }

                    if (previewOnly) {
                        log.debug(logPrefix + "will set prop '" + schemaName
                                + ':' + propName + " = " + value);

                        // document will be updated
                        docToBeUpdated = true;

                        Set<String> props = changingFields.get(schemaName);
                        if (props == null) {
                            props = new HashSet<String>();
                            changingFields.put(schemaName, props);
                        }

                        // prepare current property value for displaying
                        Object selectedValue = docModelExistingSelect.getProperty(
                                schemaName, propName);
                        if (selectedValue != null) {
                            String verboseValue = selectedValue.toString();
                            if (verboseValue.equals(SELECT_ITEM_ID_ALL)) {
                                // TODO internationalize
                                final String text_ALL = resourcesAccessor.getMessages().get(
                                        RB_KEY_TEXT_ALL);
                                verboseValue = text_ALL;
                            } else if (verboseValue.equals(SELECT_ITEM_ID_EMPTY)) {
                                final String text_EMPTY = resourcesAccessor.getMessages().get(
                                        RB_KEY_TEXT_EMPTY);
                                verboseValue = text_EMPTY;
                            }

                            // set different object if required

                            // ok, we have to convert it back so it could be
                            // well displayed
                            // check by type
                            Object currentValue = destDocModel.getProperty(
                                    schemaName, propName);
                            if (currentValue instanceof Calendar) {
                                docModelExistingSelectVerbose.setProperty(
                                        schemaName, propName, currentValue);
                            } else {
                                // no special data
                                docModelExistingSelectVerbose.setProperty(
                                        schemaName, propName, verboseValue);
                            }
                        }

                        props.add(propName);
                    } else {
                        log.debug(logPrefix + "set prop '" + schemaName + ':'
                                + propName + " = " + value);
                        destDocModel.setProperty(schemaName, propName, value);
                    }
                }
            }
        }

        if (previewOnly) {
            if (docToBeUpdated) {
                changingDocuments.add(destDocModel);
            } else {
                unchangingDocuments.add(destDocModel);
            }
        }
    }

    /**
     * Checks if the given document has the specified property value matching
     * the replacement criteria. (i.e. the property value is matching the
     * selected value or the selected item specifies to replace any value).
     *
     * @param destDocModel
     * @param schemaName
     * @param propName
     * @return
     */
    private boolean isSelectedValueMatching(DocumentModel destDocModel,
            String schemaName, String propName) {
        Object currentValue = destDocModel.getProperty(schemaName, propName);
        Object selectedValue = docModelExistingSelect.getProperty(schemaName,
                propName);
        if (selectedValue == null) {
            log.warn("selectedValue=null");
            return false;
        }
        if (currentValue != null) {
            if (selectedValue.equals(SELECT_ITEM_ID_ALL)) {
                return true;
            }

            if (selectedValue.equals(SELECT_ITEM_ID_EMPTY)) {
                if (currentValue instanceof String
                        && ((String) currentValue).length() == 0) {
                    return true;
                }
            }

            // check by type
            if (currentValue instanceof Calendar) {
                // try to convert the selected value
                Calendar currentCalendarDate = (Calendar) currentValue;
                long currentDate = currentCalendarDate.getTimeInMillis();
                return selectedValue.equals(Long.toString(currentDate));

            } else if (currentValue instanceof String[]) {
                String[] currentValueArray = (String[]) currentValue;
                String selectedValueStr = (String) selectedValue;
                // eliminate starting '[' and ending ']'
                if (selectedValueStr.length() < 2) {
                    log.warn("selected item has invalid value: "
                            + selectedValueStr);
                    return false;
                }
                selectedValueStr = selectedValueStr.substring(1,
                        selectedValueStr.length() - 1);
                String[] selectedValueArray = selectedValueStr.split(", ");

                boolean eq = Arrays.equals(currentValueArray,
                        selectedValueArray);
                log.debug("comparing currentValue: "
                        + Arrays.asList(currentValueArray)
                        + " with selectedValue: "
                        + Arrays.asList(selectedValueArray) + " = " + eq);

                return eq;
            } else if (currentValue.equals(selectedValue)) {
                return true;
            }
        } else {
            // current value is null
            if (selectedValue.equals(SELECT_ITEM_ID_EMPTY)
                    || selectedValue.equals(SELECT_ITEM_ID_ALL)) {
                return true;
            }
        }
        return false;
    }

    public String viewDocument() throws ClientException {
        final String logPrefix = "<viewDocument> ";
        log.debug(logPrefix + "selected doc: " + selectedDM);
        return navigationContext.navigateToDocument(selectedDM);
    }

    public String massEditWorkList() throws ClientException {

        final String logPrefix = "<massEditWorkList> ";

        // List<DocumentModel> clipboardSelection =
        // clipboardActions.getClipboardSelectionList();
        List<DocumentModel> clipboardSelection = clipboardActions.getCurrentSelectedList();

        if (clipboardSelection.isEmpty()) {
            log.warn(logPrefix + "No Documents in worklist for mass edition.");
            return null;
        }
        setDocumentsList(clipboardSelection);

        return NAVIGATION_MASS_EDIT;
    }

    public boolean getRemoveFromList() {
        return removeFromList;
    }

    public void setRemoveFromList(boolean remove) {
        removeFromList = remove;
    }

    @Destroy
    public void destroy() {
        log.debug("Removing SEAM component: lockActions");
    }
}
