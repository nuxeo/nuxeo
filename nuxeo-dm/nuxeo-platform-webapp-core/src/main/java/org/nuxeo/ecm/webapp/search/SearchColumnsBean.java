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

package org.nuxeo.ecm.webapp.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.FieldDescriptor;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.types.TypeService;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;
import org.nuxeo.ecm.platform.ui.web.directory.VocabularyEntry;
import org.nuxeo.ecm.platform.ui.web.directory.VocabularyEntryList;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@Name("searchColumns")
@Scope(ScopeType.CONVERSATION)
public class SearchColumnsBean extends InputController implements SearchColumns, Serializable {

    private static final long serialVersionUID = -2462881843957407025L;

    private static final Log log = LogFactory.getLog(SearchColumnsBean.class);

    private static final String RESULT_FIELD_GROUPS = "resultFieldGroups";

    private static final String SEARCH_RESULTS_TYPE = "search_results";

    private static final String SEARCH_RESULT_COLUMNS = "resultColumns";

    private String newField;

    private Map<String, FieldWidget> fieldMap;

    private List<FieldWidget> resultColumns;

    private VocabularyEntryList fieldGroupEntries;

    private VocabularyEntryList fieldEntries;

    @RequestParameter("removeField")
    private String removeFieldName;

    private String fieldRef1;

    private String fieldRef2;

    private List<FieldWidget> defaultResultColumns;

    private String sortColumn;

    private boolean sortAscending;


    @Create
    public void init() {
        log.debug("Initializing...");
        fieldMap = buildFieldMap();
        buildVocabularyEntries();
    }

    public void destroy() {
        log.debug("Destroy...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    public String changeSearch() {
        return null;
    }

    public String getNewField() {
        return newField;
    }

    public void setNewField(String newField) {
        this.newField = newField;
    }

    public String addField() {
        // the value has this form: "label.schema/schema:field"
        String ref = newField.substring(newField.indexOf(ChainSelect.DEFAULT_KEY_SEPARATOR) + 1);
        ref = ref.replace(ChainSelect.DEFAULT_KEY_SEPARATOR, ":");
        FieldWidget uiField = fieldMap.get(ref);
        if (uiField == null){
            return null;
        }
        if (resultColumns.contains(uiField)) {
            FacesContext context = FacesContext.getCurrentInstance();
            String translatedText = translate(context,
                    "label.search.column_already_added");
            FacesMessage msg = new FacesMessage(translatedText);
            context.addMessage("searchForm:resultNewField", msg);
            return null;
        }
        resultColumns.add(uiField);

        newField = null;
        return null;
    }


    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

    public String removeField() {
        FieldWidget uiField = fieldMap.get(removeFieldName);
        resultColumns.remove(uiField);
        return null;
    }

    private void buildVocabularyEntries() {
        SearchUIConfigService service = SearchUIConfigServiceHelper.getConfigService();
        List<FieldGroupDescriptor> fields = service.getFieldGroups(RESULT_FIELD_GROUPS);

        fieldGroupEntries = new VocabularyEntryList("fieldGroups");
        fieldEntries = new VocabularyEntryList("fieldValues");

        for (FieldGroupDescriptor fieldGroupDescriptor : fields) {
            String groupLabel = fieldGroupDescriptor.getLabel();
            String groupName = fieldGroupDescriptor.getName();
            if (groupLabel == null) {
                groupLabel = "label.schema." + groupName;
                log.debug(String.format("no label for fieldGroup %s, using %s",
                        groupName, groupLabel));
            }
            VocabularyEntry fieldGroupEntry = new VocabularyEntry(groupName,
                    groupLabel);
            fieldGroupEntries.add(fieldGroupEntry);

            for (FieldDescriptor fieldDescriptor : fieldGroupDescriptor.getFields()) {
                String ref = fieldDescriptor.getSchema() + ":"
                        + fieldDescriptor.getName();
                FieldWidget fieldWidget = fieldMap.get(ref);
                if (fieldWidget == null) {
                    log.debug("field not found: " + ref);
                    continue;
                }
                String fieldLabel = fieldWidget.getLabel();
                String fieldRef = fieldWidget.getFullName();
                VocabularyEntry fieldEntry = new VocabularyEntry(fieldRef,
                        fieldLabel, groupName);
                fieldEntries.add(fieldEntry);
            }
        }
    }

    public VocabularyEntryList getFieldGroupEntries() {
        return fieldGroupEntries;
    }

    public VocabularyEntryList getFieldEntries() {
        return fieldEntries;
    }

    public String reset() {
        resultColumns = getDefaultResultColumns();
        return null;
    }

    private void swapResultColumns(String fieldRef1, String fieldRef2)
            throws ClientException {
        if (fieldRef1 == null || fieldRef2 == null) {
            throw new IllegalArgumentException("null arguments not allowed");
        }

        int beginIndex = "fieldRef:".length();
        String ref1 = fieldRef1.substring(beginIndex);
        String ref2 = fieldRef2.substring(beginIndex);
        int index = 0;
        int pos1 = -1;
        int pos2 = -1;
        for (FieldWidget field : resultColumns) {
            if (field.getFullName().equals(ref1)) {
                pos1 = index;
            }
            if (field.getFullName().equals(ref2)) {
                pos2 = index;
            }
            index++;
        }
        if (pos1 == -1 || pos2 == -1) {
            throw new ClientException("field not found");
        }
        Collections.swap(resultColumns, pos1, pos2);
    }

    public String getFieldRef1() {
        return fieldRef1;
    }

    public void setFieldRef1(String fieldRef1) {
        this.fieldRef1 = fieldRef1;
    }

    public String getFieldRef2() {
        return fieldRef2;
    }

    public void setFieldRef2(String fieldRef2) {
        this.fieldRef2 = fieldRef2;
    }

    public String swapColumns() throws ClientException {
        swapResultColumns(fieldRef1, fieldRef2);
        return null;
    }

    @Begin(join = true)
    public List<FieldWidget> getResultColumns() {
        if (resultColumns == null) {
            resultColumns = getDefaultResultColumns();
        }
        return resultColumns;
    }

    @Begin(join = true)
    public void setResultColumns(List<FieldWidget> resultColumns) {
        this.resultColumns = resultColumns;
    }

    public Map<String, FieldWidget> getFieldMap() {
        if (fieldMap == null) {
            fieldMap = buildFieldMap();
        }
        return fieldMap;
    }

    public List<FieldWidget> getDefaultResultColumns() {
        if (defaultResultColumns == null) {
            SearchUIConfigService service = SearchUIConfigServiceHelper.getConfigService();
            defaultResultColumns = new ArrayList<FieldWidget>();
            List<FieldDescriptor> fields = service.getResultColumns(SEARCH_RESULT_COLUMNS);
            for (FieldDescriptor desc: fields) {
                String fullName = desc.getSchema() + ":" + desc.getName();
                defaultResultColumns.add(fieldMap.get(fullName));
            }
        }
        return new ArrayList<FieldWidget>(defaultResultColumns);
    }

    public void setDefaultResultColumnList(String[] resultColumnList) {
        defaultResultColumns = new ArrayList<FieldWidget>();
        for(String fullName: resultColumnList) {
            defaultResultColumns.add(fieldMap.get(fullName));
        }
    }

    private Map<String, FieldWidget> buildFieldMap() {
        Map<String, FieldWidget> fieldMap = new HashMap<String, FieldWidget>();
        TypeService typeService = (TypeService) Framework.getRuntime().getComponent(
                TypeService.ID);
        FieldWidget[] fieldWidgets = typeService.getTypeRegistry().getType(
                SEARCH_RESULTS_TYPE).getLayout();
        for (FieldWidget fieldWidget : fieldWidgets) {
            String fullName = fieldWidget.getFullName();
            String prefixedName = FieldHelper.getPrefixedName(fullName);
            if (prefixedName == null) {
                log.debug("field not found: " + fullName);
                continue;
            }
            fieldWidget.setPrefixedName(prefixedName);
            fieldMap.put(fieldWidget.getFullName(), fieldWidget);
        }
        return fieldMap;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public boolean getSortAscending() {
        return sortAscending;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

}
