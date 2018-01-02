/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: SuggestionActionsBean.java 59340 2008-12-12 14:07:40Z cbaican $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;

/**
 * Suggestion actions helpers
 *
 * @author Anahide Tchertchian
 * @since 5.2M4
 */
@Name("suggestionActions")
@Scope(EVENT)
public class SuggestionActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SuggestionActionsBean.class);

    /**
     * Id of the input selector
     * <p>
     * Component must be an instance of {@link ValueHolder}
     */
    @RequestParameter
    protected String suggestionInputSelectorId;

    /**
     * Id of the editable list component where selection ids are put.
     * <p>
     * Component must be an instance of {@link UIEditableList}
     */
    @RequestParameter
    protected String suggestionSelectionListId;

    /**
     * Id of the output component where single selection is displayed
     * <p>
     * Component must be an instance of {@link ValueHolder}
     */
    @RequestParameter
    protected String suggestionSelectionOutputId;

    /**
     * Id if the hidden component where single selection id is put
     * <p>
     * Component must be an instance of {@link EditableValueHolder}
     */
    @RequestParameter
    protected String suggestionSelectionHiddenId;

    /**
     * Id of the delete component displayed next to single selection
     * <p>
     * Component must be an instance of {@link UIComponent}
     */
    @RequestParameter
    protected String suggestionSelectionDeleteId;

    protected String selectedValue;

    public String getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    /**
     * Gets the base naming container from anchor.
     * <p>
     * Gets out of suggestion box as it's a naming container and we can't get components out of it with a relative path
     * => take above first found container.
     *
     * @deprecated: use {@link ComponentUtils#getBase(UIComponent)} instead
     */
    @Deprecated
    protected UIComponent getBase(UIComponent anchor) {
        return ComponentUtils.getBase(anchor);
    }

    /**
     * Adds selection from selector as a list element
     * <p>
     * Must pass request parameter "suggestionSelectionListId" holding the binding to model. Selection will be retrieved
     * using the {@link #getSelectedValue()} method.
     */
    public void addBoundSelectionToList(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        UIEditableList list = ComponentUtils.getComponent(base, suggestionSelectionListId, UIEditableList.class);

        if (list != null) {
            // add selected value to the list
            String selectedValue = getSelectedValue();
            if (!StringUtils.isBlank(selectedValue)) {
                list.addValue(selectedValue);
            }
        }
    }

    /**
     * Adds selection from selector as a list element
     * <p>
     * Must pass request parameters "suggestionInputSelectorId" holding the value to pass to the binding component,
     * "suggestionSelectionListId" holding the binding to model.
     *
     * @deprecated use {@link #addBoundSelectionToList(ActionEvent)} which retrieves selected value from bound method
     *             instead of retrieving suggestion input.
     */
    @Deprecated
    public void addSelectionToList(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        ValueHolder selector = ComponentUtils.getComponent(base, suggestionInputSelectorId, ValueHolder.class);
        UIEditableList list = ComponentUtils.getComponent(base, suggestionSelectionListId, UIEditableList.class);

        if (selector != null && list != null) {
            // add selected value to the list
            list.addValue(selector.getValue());
        }
    }

    /**
     * Adds selection from selector as single element
     * <p>
     * Must pass request parameters "suggestionSelectionOutputId" holding the value to show, and
     * "suggestionSelectionHiddenId" holding the binding to model. Selection will be retrieved using the
     * {@link #getSelectedValue()} method. Since 5.5, only one of these two parameters is required.
     * <p>
     * Additional optional request parameter "suggestionSelectionDeleteId" can be used to show an area where the "clear"
     * button is shown.
     */
    public void addSingleBoundSelection(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        EditableValueHolder hiddenSelector = null;
        if (suggestionSelectionHiddenId != null) {
            hiddenSelector = ComponentUtils.getComponent(base, suggestionSelectionHiddenId, EditableValueHolder.class);
        }
        ValueHolder output = null;
        if (suggestionSelectionOutputId != null) {
            output = ComponentUtils.getComponent(base, suggestionSelectionOutputId, ValueHolder.class);
        }

        if (output != null || hiddenSelector != null) {
            String selectedValue = getSelectedValue();
            if (output != null) {
                output.setValue(selectedValue);
            }
            if (hiddenSelector != null) {
                hiddenSelector.setSubmittedValue(selectedValue);
            }

            // display delete component if needed
            if (suggestionSelectionDeleteId != null) {
                UIComponent deleteComponent = ComponentUtils.getComponent(base, suggestionSelectionDeleteId,
                        UIComponent.class);
                if (deleteComponent != null) {
                    deleteComponent.setRendered(true);
                }
            }

        }
    }

    /**
     * Adds selection from selector as single element
     * <p>
     * Must pass request parameters "suggestionInputSelectorId" holding the value to pass to the binding component,
     * "suggestionSelectionOutputId" holding the value to show, and "suggestionSelectionHiddenId" holding the binding to
     * model.
     * <p>
     * Additional optional request parameter "suggestionSelectionDeleteId" can be used to show an area where the "clear"
     * button is shown. *
     *
     * @deprecated use {@link #addBoundSelectionToList(ActionEvent)} which retrieves selected value from bound method
     *             instead of retrieving suggestion input.
     */
    @Deprecated
    public void addSingleSelection(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = ComponentUtils.getBase(component);
        ValueHolder selector = ComponentUtils.getComponent(base, suggestionInputSelectorId, ValueHolder.class);
        EditableValueHolder hiddenSelector = ComponentUtils.getComponent(base, suggestionSelectionHiddenId,
                EditableValueHolder.class);
        ValueHolder output = ComponentUtils.getComponent(base, suggestionSelectionOutputId, ValueHolder.class);

        if (selector != null && hiddenSelector != null && output != null) {
            String selection = (String) selector.getValue();
            output.setValue(selection);
            hiddenSelector.setSubmittedValue(selection);

            // display delete component if needed
            if (suggestionSelectionDeleteId != null) {
                UIComponent deleteComponent = ComponentUtils.getComponent(base, suggestionSelectionDeleteId,
                        UIComponent.class);
                if (deleteComponent != null) {
                    deleteComponent.setRendered(true);
                }
            }

        }
    }

    /**
     * Clears single selection.
     * <p>
     * Must pass request parameters "suggestionSelectionOutputId" holding the value to show, and
     * "suggestionSelectionHiddenId" holding the binding to model. Since 5.5, only one of these two parameters is
     * required.
     * <p>
     * Additional optional request parameter "suggestionSelectionDeleteId" can be used to hide an area where the "clear"
     * button is shown.
     */
    public void clearSingleSelection(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = component;
        EditableValueHolder hiddenSelector = null;
        if (suggestionSelectionHiddenId != null) {
            hiddenSelector = ComponentUtils.getComponent(base, suggestionSelectionHiddenId, EditableValueHolder.class);
        }
        ValueHolder output = null;
        if (suggestionSelectionOutputId != null) {
            output = ComponentUtils.getComponent(base, suggestionSelectionOutputId, ValueHolder.class);
        }

        if (output != null || hiddenSelector != null) {
            if (output != null) {
                output.setValue("");
            }
            if (hiddenSelector != null) {
                hiddenSelector.setSubmittedValue("");
            }

            // hide delete component if needed
            if (suggestionSelectionDeleteId != null) {
                UIComponent deleteComponent = ComponentUtils.getComponent(base, suggestionSelectionDeleteId,
                        UIComponent.class);
                if (deleteComponent != null) {
                    deleteComponent.setRendered(false);
                }
            }

        }
    }

}
