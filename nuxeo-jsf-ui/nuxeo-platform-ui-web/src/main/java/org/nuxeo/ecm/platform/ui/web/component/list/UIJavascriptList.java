/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.ui.web.component.list;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.EditableModelImpl;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Editable list component, relying on client side javascript code to handle adding/removing element from the target
 * list.
 *
 * @since 7.2
 */
public class UIJavascriptList extends UIEditableList {

    public static final String COMPONENT_TYPE = UIJavascriptList.class.getName();

    public static final String COMPONENT_FAMILY = UIJavascriptList.class.getName();

    private static final Log log = LogFactory.getLog(UIJavascriptList.class);

    protected static final String TEMPLATE_INDEX_MARKER = "TEMPLATE_INDEX_MARKER";

    protected static final String ROW_INDEXES_PARAM = "rowIndex[]";

    protected static final String IS_LIST_TEMPLATE_VAR = "isListTemplate";

    protected enum PropertyKeys {
        rowIndexes;
    }

    public void setRowIndexes(int[] rowIndexes) {
        getStateHelper().put(PropertyKeys.rowIndexes, rowIndexes);
    }

    public int[] getRowIndexes() {
        return (int[]) getStateHelper().eval(PropertyKeys.rowIndexes);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * Override container client id resolution to handle recursion.
     */
    @Override
    @SuppressWarnings("deprecation")
    public String getContainerClientId(FacesContext context) {
        String id = super.getClientId(context);
        int index = getRowIndex();
        if (index == -2) {
            id += SEPARATOR_CHAR + TEMPLATE_INDEX_MARKER;
        } else if (index != -1) {
            id += SEPARATOR_CHAR + String.valueOf(index);
        }
        return id;
    }

    /**
     * Renders an element using rowIndex -2 and client side marker {@link #TEMPLATE_INDEX_MARKER}.
     * <p>
     * This element will be used on client side by js code to handle addition of a new element.
     */
    @Override
    protected void encodeTemplate(FacesContext context) throws IOException {
        int oldIndex = getRowIndex();
        Object requestMapValue = saveRequestMapModelValue();
        Map<String, Object> requestMap = getFacesContext().getExternalContext().getRequestMap();
        boolean hasVar = false;
        if (requestMap.containsKey(IS_LIST_TEMPLATE_VAR)) {
            hasVar = true;
        }
        Object oldIsTemplateBoolean = requestMap.remove(IS_LIST_TEMPLATE_VAR);

        try {
            setRowIndex(-2);

            // expose a boolean that can be used on client side to hide this element without disturbing the DOM
            requestMap.put(IS_LIST_TEMPLATE_VAR, Boolean.TRUE);

            EditableModel model = new EditableModelImpl(Collections.singletonList(getTemplate()), null);
            model.setRowIndex(0);
            requestMap.put("model", model);

            // render the template as escaped html
            @SuppressWarnings("resource")
            ResponseWriter oldResponseWriter = context.getResponseWriter();
            StringWriter cacheingWriter = new StringWriter();

            @SuppressWarnings("resource")
            ResponseWriter newResponseWriter = oldResponseWriter.cloneWithWriter(cacheingWriter);

            context.setResponseWriter(newResponseWriter);

            if (getChildCount() > 0) {
                for (UIComponent kid : getChildren()) {
                    if (!kid.isRendered()) {
                        continue;
                    }
                    try {
                        ComponentSupport.encodeRecursive(context, kid);
                    } catch (IOException err) {
                        log.error("Error while rendering component " + kid);
                    }
                }
            }

            cacheingWriter.flush();
            cacheingWriter.close();

            context.setResponseWriter(oldResponseWriter);

            String html = Functions.htmlEscape(cacheingWriter.toString());
            @SuppressWarnings("resource")
            ResponseWriter writer = context.getResponseWriter();
            writer.write("<script type=\"text/x-html-template\">");
            writer.write(html);
            writer.write("</script>");

        } finally {
            setRowIndex(oldIndex);

            // restore
            if (hasVar) {
                requestMap.put(IS_LIST_TEMPLATE_VAR, oldIsTemplateBoolean);
            } else {
                requestMap.remove(IS_LIST_TEMPLATE_VAR);
            }
            restoreRequestMapModelValue(requestMapValue);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void decode(FacesContext context) {
        super.decode(context);
        Map<String, String[]> requestMap = context.getExternalContext().getRequestParameterValuesMap();

        String clientId = getClientId() + NamingContainer.SEPARATOR_CHAR + ROW_INDEXES_PARAM;
        String[] v = requestMap.get(clientId);
        if (v == null) {
            // no info => no elements to decode
            setRowIndexes(null);
            return;
        }

        try {
            int[] indexes = new int[v.length];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = Integer.valueOf(v[i]);
            }
            setRowIndexes(indexes);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid value '%s' for row indexes at '%s'", StringUtils.join(v, ","), clientId));
        }
    }

    @Override
    protected void processFacetsAndChildren(final FacesContext context, final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        EditableModel model = getEditableModel();
        int oldIndex = getRowIndex();
        int[] rowIndexes = getRowIndexes();
        Object requestMapValue = saveRequestMapModelValue();

        try {

            if (phaseId == PhaseId.APPLY_REQUEST_VALUES && rowIndexes != null) {
                for (int i = 0; i < rowIndexes.length; i++) {
                    int idx = rowIndexes[i];
                    setRowIndex(idx);
                    if (!isRowAvailable()) {
                        // new value => insert it, initialized with template
                        model.insertValue(idx, getEditableModel().getUnreferencedTemplate());
                    }
                }
            }

            List<Integer> deletedIndexes = new ArrayList<>();
            if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
                // check deleted indexes, to avoid performing validation on them
                // A map with the new index for each row key
                Map<Integer, Integer> keyIndexMap = new HashMap<>();
                if (rowIndexes != null) {
                    for (int i = 0; i < rowIndexes.length; i++) {
                        int idx = rowIndexes[i];
                        keyIndexMap.put(idx, i);
                    }
                }
                for (int i = 0; i < getRowCount(); i++) {
                    // This row has been deleted
                    if (!keyIndexMap.containsKey(i)) {
                        deletedIndexes.add(i);
                    }
                }
            }

            int end = getRowCount();
            for (int idx = 0; idx < end; idx++) {
                if (deletedIndexes.contains(idx)) {
                    continue;
                }
                setRowIndex(idx);
                if (isRowAvailable()) {
                    for (UIComponent stamp : stamps) {
                        processComponent(context, stamp, phaseId);
                    }
                    if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                        // detect changes during process update phase and fill
                        // the EditableModel list diff.
                        if (isRowModified()) {
                            recordValueModified(idx, getRowData());
                        }
                    }
                } else {
                    break;
                }
            }

            if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                // A map with the new index for each row key
                Map<Integer, Integer> keyIndexMap = new HashMap<>();
                if (rowIndexes != null) {
                    for (int i = 0; i < rowIndexes.length; i++) {
                        int idx = rowIndexes[i];
                        keyIndexMap.put(idx, i);
                    }
                }

                // rows to delete
                List<Integer> toDelete = new ArrayList<>();
                // client id
                String cid = super.getClientId(context);

                // move rows
                for (int i = 0; i < getRowCount(); i++) {
                    setRowKey(i);
                    int curIdx = getRowIndex();

                    // This row has been deleted
                    if (!keyIndexMap.containsKey(i)) {
                        toDelete.add(i);
                    } else { // This row has been moved
                        int newIdx = keyIndexMap.get(i);
                        if (curIdx != newIdx) {
                            model.moveValue(curIdx, newIdx);
                            // also move any messages in the context attached to the old index
                            String prefix = cid + SEPARATOR_CHAR + curIdx + SEPARATOR_CHAR;
                            String replacement = cid + SEPARATOR_CHAR + newIdx + SEPARATOR_CHAR;
                            Iterator<String> it = context.getClientIdsWithMessages();
                            while (it.hasNext()) {
                                String id = it.next();
                                if (id != null && id.startsWith(prefix)) {
                                    Iterator<FacesMessage> mit = context.getMessages(id);
                                    while (mit.hasNext()) {
                                        context.addMessage(id.replaceFirst(prefix, replacement), mit.next());
                                    }
                                }
                            }
                        }
                    }
                }

                // delete rows
                for (int i : toDelete) {
                    setRowKey(i);
                    model.removeValue(getRowIndex());
                }
            }

        } finally {
            setRowIndex(oldIndex);
            restoreRequestMapModelValue(requestMapValue);
        }
    }

}
