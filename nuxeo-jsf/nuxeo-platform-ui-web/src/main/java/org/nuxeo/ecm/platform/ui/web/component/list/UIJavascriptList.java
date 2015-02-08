/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.ui.web.component.list;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    protected void encodeTemplate(FacesContext context) {
        int oldIndex = getRowIndex();
        setRowIndex(-2);

        // expose a boolean that can be used on client side to hide this element without disturbing the DOM
        Map<String, Object> requestMap = getFacesContext().getExternalContext().getRequestMap();
        boolean hasVar = false;
        if (requestMap.containsKey(IS_LIST_TEMPLATE_VAR)) {
            hasVar = true;
        }
        Object oldIsTemplateBoolean = requestMap.remove(IS_LIST_TEMPLATE_VAR);
        requestMap.put(IS_LIST_TEMPLATE_VAR, Boolean.TRUE);

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
        setRowIndex(oldIndex);

        // restore
        if (hasVar) {
            requestMap.put(IS_LIST_TEMPLATE_VAR, oldIsTemplateBoolean);
        } else {
            requestMap.remove(IS_LIST_TEMPLATE_VAR);
        }
    }

    @SuppressWarnings("deprecation")
    protected int[] retrieveRowIndexesFromRequest(FacesContext context) {

        Map<String, String[]> requestMap = context.getExternalContext().getRequestParameterValuesMap();

        String clientId = getClientId() + NamingContainer.SEPARATOR_CHAR + ROW_INDEXES_PARAM;
        String[] v = requestMap.get(clientId);

        try {
            int[] indexes = new int[v.length - 1]; // skip the last value since it comes from the template
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = Integer.valueOf(v[i]);
            }
            return indexes;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid value '%s' for row indexes: '%s'",
                    StringUtils.join(v, ","), clientId));
        }
    }

    protected void processFacetsAndChildren(final FacesContext context, final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        int oldIndex = getRowIndex();
        int end = getRowCount();

        // A map with the new index for each row key
        Map<Integer, Integer> keyIndexMap = new HashMap<>();

        if (phaseId == PhaseId.APPLY_REQUEST_VALUES || phaseId == PhaseId.UPDATE_MODEL_VALUES) {
            // if processing decodes retrieve row indexes from request
            int[] rowIndexes = retrieveRowIndexesFromRequest(context);

            for (int i = 0; i < rowIndexes.length; i++) {
                int idx = rowIndexes[i];
                keyIndexMap.put(idx, i);
                // determine the row count
                if (idx >= end) {
                    end = idx + 1;
                }
            }
        }

        Object requestMapValue = saveRequestMapModelValue();
        try {
            // update values and add new rows if needed
            for (int idx = 0; idx < end; idx++) {
                setRowIndex(idx);

                if (!isRowAvailable()) {
                    // might be a new value
                    // XXX to refine
                    getEditableModel().insertValue(idx, getTemplate());
                }
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

                // rows to delete
                List<Integer> toDelete = new ArrayList<>();

                // move rows
                for (int i = 0; i < end; i++) {
                    setRowKey(i);
                    int curIdx = getRowIndex();

                    // This row has been deleted
                    if (!keyIndexMap.containsKey(i)) {
                        toDelete.add(i);
                    } else { // This row has been moved
                        int newIdx = keyIndexMap.get(i);
                        if (curIdx != newIdx) {
                            getEditableModel().moveValue(curIdx, newIdx);
                        }
                    }
                }

                // delete rows
                for (int i : toDelete) {
                    setRowKey(i);
                    getEditableModel().removeValue(getRowIndex());
                }
            }

        } finally {
            setRowIndex(oldIndex);
            restoreRequestMapModelValue(requestMapValue);
        }
    }

}
