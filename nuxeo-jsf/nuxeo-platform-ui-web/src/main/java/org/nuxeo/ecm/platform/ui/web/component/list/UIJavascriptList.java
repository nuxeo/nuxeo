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
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

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

}
