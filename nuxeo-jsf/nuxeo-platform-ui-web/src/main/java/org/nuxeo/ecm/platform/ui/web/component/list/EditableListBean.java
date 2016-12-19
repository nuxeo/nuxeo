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
 * $Id: EditableListBean.java 25566 2007-10-01 14:01:21Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.component.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.FacesEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.model.EditableModel;

/**
 * Bean used to interact with {@link UIEditableList} component.
 * <p>
 * Used to add/remove items from a list.
 * <p>
 * Optionally used to work around some unwanted behaviour in data tables.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class EditableListBean {

    private static final Log log = LogFactory.getLog(EditableListBean.class);

    public static final String FOR_PARAMETER_NAME = "for";

    public static final String INDEX_PARAMETER_NAME = "index";

    public static final String TYPE_PARAMETER_NAME = "type";

    public static final String NUMBER_PARAMETER_NAME = "number";

    protected UIComponent binding;

    // dont make it static so that jsf can call it
    public UIComponent getBinding() {
        return binding;
    }

    // dont make it static so that jsf can call it
    public void setBinding(UIComponent binding) {
        this.binding = binding;
    }

    // don't make it static so that jsf can call it
    public void performAction(String listComponentId, String index, String type) {
        if (binding == null) {
            log.error("Component binding not set, cannot perform action");
            return;
        }
        Map<String, String> requestMap = new HashMap<String, String>();
        requestMap.put(FOR_PARAMETER_NAME, listComponentId);
        requestMap.put(INDEX_PARAMETER_NAME, index);
        requestMap.put(TYPE_PARAMETER_NAME, type);
        performAction(binding, requestMap);
    }

    public void performAction(ActionEvent event) {
        performAction((FacesEvent) event);
    }

    /**
     * @since 6.0
     */
    public void performAction(AjaxBehaviorEvent event) {
        performAction((FacesEvent) event);
    }

    protected void performAction(FacesEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Map<String, String> requestMap = eContext.getRequestParameterMap();
        performAction(component, requestMap);
    }

    protected static void performAction(UIComponent binding, Map<String, String> requestMap) {
        UIEditableList editableComp = getEditableListComponent(binding, requestMap);
        if (editableComp == null) {
            return;
        }
        EditableListModificationType type = getModificationType(requestMap);
        if (type == null) {
            return;
        }
        Integer index;
        Integer number;

        EditableModel model = editableComp.getEditableModel();
        Object template = editableComp.getTemplate();
        switch (type) {
        case ADD:
            number = getNumber(requestMap);
            if (number == null) {
                // perform add only once
                model.addValue(template);
            } else {
                for (int i = 0; i < number; i++) {
                    // make sure added template is unreferenced
                    model.addTemplateValue();
                }
            }
            break;
        case INSERT:
            index = getIndex(requestMap);
            if (index == null) {
                return;
            }
            number = getNumber(requestMap);
            if (number == null) {
                // perform insert only once
                model.insertValue(index, template);
            } else {
                for (int i = 0; i < number; i++) {
                    model.insertTemplateValue(index);
                }
            }
            break;
        case REMOVE:
            index = getIndex(requestMap);
            if (index == null) {
                return;
            }
            editableComp.removeValue(index);
            break;
        case MOVEUP:
            index = getIndex(requestMap);
            if (index == null) {
                return;
            }
            editableComp.moveValue(index, index - 1);
            break;
        case MOVEDOWN:
            index = getIndex(requestMap);
            if (index == null) {
                return;
            }
            editableComp.moveValue(index, index + 1);
            break;
        }
    }

    protected static String getParameterValue(Map<String, String> requestMap, String parameterName) {
        String string = requestMap.get(parameterName);
        if (string == null || string.length() == 0) {
            return null;
        } else {
            return string;
        }
    }

    protected static UIEditableList getEditableListComponent(UIComponent component, Map<String, String> requestMap) {
        UIEditableList listComponent = null;
        String forString = getParameterValue(requestMap, FOR_PARAMETER_NAME);
        if (forString == null) {
            log.error("Could not find '" + FOR_PARAMETER_NAME + "' parameter in the request map");
        } else {
            UIComponent forComponent = component.findComponent(forString);
            if (forComponent == null) {
                log.error("Could not find component with id: " + forString);
            } else if (!(forComponent instanceof UIEditableList)) {
                log.error("Invalid component with id " + forString + ": " + forComponent
                        + ", expected a component with class " + UIEditableList.class);
            } else {
                listComponent = (UIEditableList) forComponent;
            }
        }
        return listComponent;
    }

    protected static EditableListModificationType getModificationType(Map<String, String> requestMap) {
        EditableListModificationType type = null;
        String typeString = getParameterValue(requestMap, TYPE_PARAMETER_NAME);
        if (typeString == null) {
            log.error("Could not find '" + TYPE_PARAMETER_NAME + "' parameter in the request map");
        } else {
            try {
                type = EditableListModificationType.valueOfString(typeString);
            } catch (IllegalArgumentException err) {
                log.error("Illegal value for '" + TYPE_PARAMETER_NAME + "' attribute: " + typeString
                        + ", should be one of " + EditableListModificationType.values());
            }
        }
        return type;
    }

    protected static Integer getIndex(Map<String, String> requestMap) {
        Integer index = null;
        String indexString = getParameterValue(requestMap, INDEX_PARAMETER_NAME);
        if (indexString == null) {
            log.error("Could not find '" + INDEX_PARAMETER_NAME + "' parameter in the request map");
        } else {
            try {
                index = Integer.valueOf(indexString);
            } catch (NumberFormatException e) {
                log.error("Illegal value for '" + INDEX_PARAMETER_NAME + "' attribute: " + indexString
                        + ", should be integer");
            }
        }
        return index;
    }

    protected static Integer getNumber(Map<String, String> requestMap) {
        Integer number = null;
        String numberString = getParameterValue(requestMap, NUMBER_PARAMETER_NAME);
        if (numberString != null) {
            try {
                number = Integer.valueOf(numberString);
            } catch (NumberFormatException e) {
                log.error("Illegal value for '" + NUMBER_PARAMETER_NAME + "' attribute: " + numberString
                        + ", should be integer");
            }
        }
        return number;
    }

    /**
     * Dummy list of one item, used to wrap a table within another table.
     * <p>
     * A table resets its saved state when decoding, which is a problem when saving a file temporarily: as it will not
     * be submitted again in the request, the new value will be lost. The table is not reset when embedded in another
     * table, so we can use this list as value of the embedding table as a work around.
     *
     * @return dummy list of one item
     */
    // don't make it static so that jsf can call it
    public List<Object> getDummyList() {
        List<Object> dummy = new ArrayList<Object>(1);
        dummy.add("dummy");
        return dummy;
    }

}
