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
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

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

    protected static final String ITEMS_COUNTER_ID = "itemsCounter";

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
    protected int retrieveCountFromRequest(FacesContext context) {

        UIComponent component = findComponent(ITEMS_COUNTER_ID);
        if (!(component instanceof UIInput)) {
            throw new IllegalArgumentException("Invalid sub component with id " + ITEMS_COUNTER_ID);
        }

        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String clientId = getClientId() + NamingContainer.SEPARATOR_CHAR + TEMPLATE_INDEX_MARKER
                + NamingContainer.SEPARATOR_CHAR + ITEMS_COUNTER_ID;
        String v = requestMap.get(clientId);
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid value '%s' for counter component with id '%s'",
                    v, clientId));
        }
    }

    protected void processFacetsAndChildren(final FacesContext context, final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        int oldIndex = getRowIndex();
        int end = getRowCount();
        if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
            // if processing decodes, do not rely on current counter in datamodel, retrieve counter from request
            end = retrieveCountFromRequest(context);
        }
        Object requestMapValue = saveRequestMapModelValue();
        try {
            int first = 0;
            for (int i = first; i < end; i++) {
                setRowIndex(i);
                if (!isRowAvailable()) {
                    // might be a new value
                    // XXX to refine
                    getEditableModel().insertValue(i, getTemplate());
                }
                if (isRowAvailable()) {
                    for (UIComponent stamp : stamps) {
                        processComponent(context, stamp, phaseId);
                    }
                    if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                        // detect changes during process update phase and fill
                        // the EditableModel list diff.
                        if (isRowModified()) {
                            recordValueModified(i, getRowData());
                        }
                    }
                } else {
                    break;
                }
            }
        } finally {
            setRowIndex(oldIndex);
            restoreRequestMapModelValue(requestMapValue);
        }
    }

}
