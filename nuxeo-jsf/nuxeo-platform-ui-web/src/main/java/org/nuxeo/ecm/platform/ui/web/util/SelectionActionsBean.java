/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *     <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 */
package org.nuxeo.ecm.platform.ui.web.util;

import java.io.Serializable;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectMany;
import javax.faces.component.ValueHolder;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.FacesEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;

/**
 * Helper for selection actions, useful when performing ajax calls on a "liste shuttle" widget for instance, or to
 * retrieve the selected value on a JSF component and set it on another.
 */
@Name("selectionActions")
@Scope(ScopeType.EVENT)
public class SelectionActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SelectionActionsBean.class);

    public enum ShiftType {
        FIRST, UP, DOWN, LAST
    }

    @RequestParameter
    protected String leftSelect;

    @RequestParameter
    protected String leftItems;

    @RequestParameter
    protected String rightSelect;

    @RequestParameter
    protected String rightItems;

    @RequestParameter
    protected String submittedList;

    /**
     * Id of the input selector
     * <p>
     * Component must be an instance of {@link ValueHolder}
     */
    @RequestParameter
    protected String selectorId;

    /**
     * Id of the value holder that will receive the selected value.
     * <p>
     * Component must be an instance of {@link ValueHolder}
     */
    @RequestParameter
    protected String valueHolderId;

    /**
     * Lookup level request parameter (defaults to 1, means search is done in r first parent naming container)
     *
     * @since 5.6
     */
    @RequestParameter
    protected Integer lookupLevel;

    /**
     * Lookup level field (defaults to 1, means search is done in first parent naming container)
     * <p>
     * Useful as fallback when posting a button where request parameter {@link #lookupLevel} cannot be set.
     *
     * @since 5.6
     */
    protected String lookupLevelValue;

    /**
     * @since 5.6
     */
    public String getLookupLevelValue() {
        return lookupLevelValue;
    }

    /**
     * @since 5.6
     */
    public void setLookupLevelValue(String lookupLevelValue) {
        this.lookupLevelValue = lookupLevelValue;
    }

    /**
     * @since 5.6
     */
    protected int computeLookupLevel() {
        if (lookupLevel != null) {
            return lookupLevel.intValue();
        }
        String setValue = getLookupLevelValue();
        if (setValue != null) {
            return Integer.valueOf(setValue).intValue();
        }
        return 1;
    }

    /**
     * Value held temporarily by this bean to be set on JSF components.
     *
     * @since 5.5
     */
    @RequestParameter
    protected String selectedValue;

    public String getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    /**
     * Value component id held temporarily by this bean to be retrieved from the JSF component tree.
     * <p>
     * this is an alternative to {@link #valueHolderId} request parameter usage, to make it possible to set this value
     * easily from command buttons (as only command links do take request parameters into account).
     *
     * @since 5.6
     */
    protected String selectedValueHolder;

    public String getSelectedValueHolder() {
        return selectedValueHolder;
    }

    public void setSelectedValueHolder(String selectedValueHolder) {
        this.selectedValueHolder = selectedValueHolder;
    }

    public SelectItem[] getEmptySelection() {
        return new SelectItem[0];
    }

    protected boolean checkRightComponents() {
        String logPrefix = "Check right components: ";
        if (rightSelect == null) {
            log.error(logPrefix + "No select component name");
            return false;
        }
        if (rightItems == null) {
            log.error(logPrefix + "No items component name");
            return false;
        }
        return true;
    }

    protected boolean checkLeftComponents() {
        String logPrefix = "Check left components: ";
        if (leftSelect == null) {
            log.error(logPrefix + "No select component name");
            return false;
        }
        if (leftItems == null) {
            log.error(logPrefix + "No items component name");
            return false;
        }
        return true;
    }

    protected boolean checkSubmittedList() {
        String logPrefix = "Check submitted list: ";
        if (submittedList == null) {
            log.error(logPrefix + "No component name");
            return false;
        }
        return true;
    }

    public void shiftSelected(ShiftType stype, ActionEvent event) {
        if (!checkRightComponents() || !checkSubmittedList()) {
            return;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent rightItemsComp = eventComp.findComponent(rightItems);
        UIComponent rightSelectComp = eventComp.findComponent(rightSelect);
        UIComponent hiddenTargetListComp = eventComp.findComponent(submittedList);
        if (rightSelectComp instanceof UISelectMany && rightItemsComp instanceof UISelectItems
                && hiddenTargetListComp instanceof UIEditableList) {
            UISelectItems targetItems = (UISelectItems) rightItemsComp;
            UISelectMany targetComp = (UISelectMany) rightSelectComp;
            UIEditableList hiddenTargetList = (UIEditableList) hiddenTargetListComp;
            switch (stype) {
            case UP:
                ComponentUtils.shiftItemsUp(targetComp, targetItems, hiddenTargetList);
                break;
            case DOWN:
                ComponentUtils.shiftItemsDown(targetComp, targetItems, hiddenTargetList);
                break;
            case FIRST:
                ComponentUtils.shiftItemsFirst(targetComp, targetItems, hiddenTargetList);
                break;
            case LAST:
                ComponentUtils.shiftItemsLast(targetComp, targetItems, hiddenTargetList);
                break;
            }
        }
    }

    public void shiftSelectedUp(ActionEvent event) {
        shiftSelected(ShiftType.UP, event);
    }

    public void shiftSelectedDown(ActionEvent event) {
        shiftSelected(ShiftType.DOWN, event);
    }

    public void shiftSelectedFirst(ActionEvent event) {
        shiftSelected(ShiftType.FIRST, event);
    }

    public void shiftSelectedLast(ActionEvent event) {
        shiftSelected(ShiftType.LAST, event);
    }

    public void addToSelection(ActionEvent event) {
        if (!checkLeftComponents() || !checkRightComponents() || !checkSubmittedList()) {
            return;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftSelectComp = eventComp.findComponent(leftSelect);
        UIComponent leftItemsComp = eventComp.findComponent(leftItems);
        UIComponent rightItemsComp = eventComp.findComponent(rightItems);
        UIComponent hiddenTargetListComp = eventComp.findComponent(submittedList);
        if (leftSelectComp instanceof UISelectMany && leftItemsComp instanceof UISelectItems
                && rightItemsComp instanceof UISelectItems && hiddenTargetListComp instanceof UIEditableList) {
            UISelectMany sourceSelect = (UISelectMany) leftSelectComp;
            UISelectItems sourceItems = (UISelectItems) leftItemsComp;
            UISelectItems targetItems = (UISelectItems) rightItemsComp;
            UIEditableList hiddenTargetList = (UIEditableList) hiddenTargetListComp;
            ComponentUtils.moveItems(sourceSelect, sourceItems, targetItems, hiddenTargetList, true);
        }
    }

    public void removeFromSelection(ActionEvent event) {
        if (!checkLeftComponents() || !checkRightComponents() || !checkSubmittedList()) {
            return;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftItemsComp = eventComp.findComponent(leftItems);
        UIComponent rightSelectComp = eventComp.findComponent(rightSelect);
        UIComponent rightItemsComp = eventComp.findComponent(rightItems);
        UIComponent hiddenTargetListComp = eventComp.findComponent(submittedList);
        if (leftItemsComp instanceof UISelectItems && rightSelectComp instanceof UISelectMany
                && rightItemsComp instanceof UISelectItems && hiddenTargetListComp instanceof UIEditableList) {
            UISelectItems leftItems = (UISelectItems) leftItemsComp;
            UISelectMany rightSelect = (UISelectMany) rightSelectComp;
            UISelectItems rightItems = (UISelectItems) rightItemsComp;
            UIEditableList hiddenTargetList = (UIEditableList) hiddenTargetListComp;
            ComponentUtils.moveItems(rightSelect, rightItems, leftItems, hiddenTargetList, false);
        }
    }

    public void addAllToSelection(ActionEvent event) {
        if (!checkLeftComponents() || !checkRightComponents() || !checkSubmittedList()) {
            return;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftItemsComp = eventComp.findComponent(leftItems);
        UIComponent rightItemsComp = eventComp.findComponent(rightItems);
        UIComponent hiddenTargetListComp = eventComp.findComponent(submittedList);
        if (leftItemsComp instanceof UISelectItems && rightItemsComp instanceof UISelectItems
                && hiddenTargetListComp instanceof UIEditableList) {
            UISelectItems sourceItems = (UISelectItems) leftItemsComp;
            UISelectItems targetItems = (UISelectItems) rightItemsComp;
            UIEditableList hiddenTargetList = (UIEditableList) hiddenTargetListComp;
            ComponentUtils.moveAllItems(sourceItems, targetItems, hiddenTargetList, true);
        }
    }

    public UISelectMany getSourceSelectComponent(ActionEvent event) {
        if (leftSelect == null) {
            log.warn("Unable to find leftSelect component. Param 'leftSelect' not sent in request");
            return null;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftSelectComp = eventComp.findComponent(leftSelect);

        if (leftSelectComp instanceof UISelectMany) {
            return (UISelectMany) leftSelectComp;
        }
        return null;
    }

    public UISelectItems getSourceSelectItems(ActionEvent event) {
        if (leftItems == null) {
            log.warn("Unable to find leftItems component. Param 'leftItems' not sent in request");
            return null;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftItemsComp = eventComp.findComponent(leftItems);

        if (leftItemsComp instanceof UISelectItems) {
            return (UISelectItems) leftItemsComp;
        }
        return null;
    }

    public void removeAllFromSelection(ActionEvent event) {
        if (!checkLeftComponents() || !checkRightComponents() || !checkSubmittedList()) {
            return;
        }
        UIComponent eventComp = event.getComponent();
        UIComponent leftItemsComp = eventComp.findComponent(leftItems);
        UIComponent rightItemsComp = eventComp.findComponent(rightItems);
        UIComponent hiddenTargetListComp = eventComp.findComponent(submittedList);
        if (leftItemsComp instanceof UISelectItems && rightItemsComp instanceof UISelectItems
                && hiddenTargetListComp instanceof UIEditableList) {
            UISelectItems leftItems = (UISelectItems) leftItemsComp;
            UISelectItems rightItems = (UISelectItems) rightItemsComp;
            UIEditableList hiddenTargetList = (UIEditableList) hiddenTargetListComp;
            ComponentUtils.moveAllItems(rightItems, leftItems, hiddenTargetList, false);
        }
    }

    /**
     * Adds selection retrieved from a selector to another component
     * <p>
     * Must pass request parameters "selectorId" holding the id of component holding the value to pass to the other
     * component, and "valueHolderId" holding the other component id.
     *
     * @since 5.5
     * @param event
     * @deprecated since 6.0: use {@link #onSelection(AjaxBehaviorEvent)} instead.
     */
    @Deprecated
    public void onSelection(ActionEvent event) {
        log.warn(String.format("The method #onSelection(ActionEvent) on component "
                + "'selectionActions' at '%s' is deprecated, please " + "use #onSelection(AjaxBehaviorEvent) instead",
                this.getClass().getName()));
        onSelection((FacesEvent) event);
    }

    /**
     * @since 6.0
     */
    public void onSelection(AjaxBehaviorEvent event) {
        onSelection((FacesEvent) event);
    }

    protected void onSelection(FacesEvent event) {
        UIComponent component = event.getComponent();
        Object value = retrieveSourceComponentValue(component, selectorId);
        UIComponent base = ComponentUtils.getBase(component);
        ValueHolder valueHolderComp = ComponentUtils.getComponent(base, valueHolderId, ValueHolder.class);
        setTargetComponentValue(valueHolderComp, value);
    }

    /**
     * Adds value retrieved from {@link #getSelectedValue()} to a component
     * <p>
     * Must pass request parameters "valueHolderId" holding the id of the bound component, and call
     * {@link #setSelectedValue(String)} prior to this call.
     * <p>
     * As an alternative, must call {@link #setSelectedValueHolder(String)} with the id of the bound component, and call
     * {@link #setSelectedValue(String)} prior to this call (this makes it possible to use the same logic in command
     * buttons that do not make it possible to pass request parameters).
     *
     * @deprecated since 6.0: use {@link #onClick(AjaxBehaviorEvent)} instead.
     * @since 5.5
     * @param event
     */
    @Deprecated
    public void onClick(ActionEvent event) {
        log.warn(String.format("The method #onClick(ActionEvent) on component "
                + "'selectionActions' at '%s' is deprecated, please " + "use #onClick(AjaxBehaviorEvent) instead",
                this.getClass().getName()));
        onClick((FacesEvent) event);
    }

    public void onClick(AjaxBehaviorEvent event) {
        onClick((FacesEvent) event);
    }

    protected void onClick(FacesEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        EditableValueHolder hiddenSelector = null;
        UIComponent base = retrieveBase(component, computeLookupLevel());
        if (valueHolderId != null) {
            hiddenSelector = ComponentUtils.getComponent(base, valueHolderId, EditableValueHolder.class);
        }
        if (hiddenSelector == null) {
            String selectedValueHolder = getSelectedValueHolder();
            if (selectedValueHolder != null) {
                hiddenSelector = ComponentUtils.getComponent(base, selectedValueHolder, EditableValueHolder.class);
            }
        }
        if (hiddenSelector != null) {
            String selectedValue = getSelectedValue();
            setTargetComponentValue(hiddenSelector, selectedValue);
        }
    }

    protected UIComponent retrieveBase(UIComponent anchor, int lookupLevel) {
        UIComponent base = ComponentUtils.getBase(anchor);
        if (lookupLevel > 1) {
            for (int i = 0; i < (lookupLevel - 1); i++) {
                base = ComponentUtils.getBase(base);
            }
        }
        return base;
    }

    /**
     * Retrieves a value from another component and sets it on the target component.
     * <p>
     * Source component id must be passed in the event component attributes with id "sourceComponentId".
     * <p>
     * Target component id must be passed in the event component attributes with id "targetComponentId". If target
     * component is an {@link EditableValueHolder}, its submitted value is set. Otherwise, its local value is set.
     *
     * @since 6.0
     * @param event
     */
    public void setValueFromComponent(AjaxBehaviorEvent event) {
        setValueFromComponent((FacesEvent) event);
    }

    /**
     * @see #setValueFromComponent(ActionEvent)
     * @since 6.0
     */
    public void setValueFromComponent(ActionEvent event) {
        setValueFromComponent((FacesEvent) event);
    }

    protected void setValueFromComponent(FacesEvent event) {
        UIComponent anchor = event.getComponent();
        String sourceCompId = getStringAttribute(anchor, "sourceComponentId", true);
        Object value = retrieveSourceComponentValue(anchor, sourceCompId);
        String targetCompId = getStringAttribute(anchor, "targetComponentId", true);
        ValueHolder targetComp = ComponentUtils.getComponent(anchor, targetCompId, ValueHolder.class);
        setTargetComponentValue(targetComp, value);
    }

    /**
     * Retrieves a value passed as an attribute with id "selectedValue" on the event component attributes and sets it on
     * the target component.
     * <p>
     * Target component id must be passed in the event component attributes with id "targetComponentId". If target
     * component is an {@link EditableValueHolder}, its submitted value is set. Otherwise, its local value is set.
     *
     * @since 6.0
     * @param event
     */
    public void setStaticValue(AjaxBehaviorEvent event) {
        setStaticValue((FacesEvent) event);
    }

    /**
     * @see #setStaticValue(ActionEvent)
     * @since 6.0
     */
    public void setStaticValue(ActionEvent event) {
        setStaticValue((FacesEvent) event);
    }

    protected void setStaticValue(FacesEvent event) {
        UIComponent anchor = event.getComponent();
        Object value = anchor.getAttributes().get("selectedValue");
        String targetCompId = getStringAttribute(anchor, "targetComponentId", true);
        ValueHolder targetComp = ComponentUtils.getComponent(anchor, targetCompId, ValueHolder.class);
        setTargetComponentValue(targetComp, value);
    }

    protected String getStringAttribute(UIComponent component, String name, boolean required) {
        Object value = component.getAttributes().get(name);
        if (required && value == null) {
            throw new IllegalArgumentException("Component attribute with name '" + name + "' cannot be null: " + value);
        }
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw new IllegalArgumentException("Component attribute with name '" + name + "' is not a String: " + value);
    }

    protected Object retrieveSourceComponentValue(UIComponent base, String targetId) {
        ValueHolder selectComp = ComponentUtils.getComponent(base, targetId, ValueHolder.class);
        if (selectComp != null) {
            Object value;
            if (selectComp instanceof EditableValueHolder) {
                value = ((EditableValueHolder) selectComp).getSubmittedValue();
                if (value == null) {
                    value = selectComp.getValue();
                }
            } else {
                value = selectComp.getValue();
            }
            return value;
        }
        return null;
    }

    protected void setTargetComponentValue(ValueHolder target, Object value) {
        if (target != null) {
            if (target instanceof EditableValueHolder) {
                ((EditableValueHolder) target).setSubmittedValue(value);
            } else {
                target.setValue(value);
            }
        }
    }

}
