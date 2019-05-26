/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.component;

import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.FacesEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.util.ComponentRenderUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Managed bean, request-scoped, that resets the components value for ajax interactions.
 * <p>
 * Resets are done using the following rule:
 * <ul>
 * <li>if the component implements {@link ResettableComponent} interface, its method
 * {@link ResettableComponent#resetCachedModel()} is called</li>
 * <li>if the component implements {@link EditableValueHolder}, its submitted value is reset, and its local value is
 * reset only if it holds a value binding for the "value" attribute</li>
 * <li>if the component implements {@link ValueHolder}, its its local value is reset only if it holds a value binding
 * for the "value" attribute.</li>
 * </ul>
 *
 * @since 5.7
 */
@Name("jsfResetActions")
@Scope(ScopeType.EVENT)
public class JSFResetActionsBean {

    private static final Log log = LogFactory.getLog(JSFResetActionsBean.class);

    /**
     * Base component id for reset actions.
     *
     * @since 5.9.1
     */
    protected String baseComponentId;

    /**
     * Returns the base component id, if {@link #setBaseComponentId(String)} was previously called in the same request,
     * or null.
     *
     * @since 5.9.1
     */
    public String getBaseComponentId() {
        return baseComponentId;
    }

    /**
     * Sets the base component id so that {@link #resetComponentsFor(ActionEvent)} can look it up in the hierarchy of
     * components, and reset component states recursively from it.
     *
     * @since 5.9.1
     * @see #resetComponentsFor(ActionEvent)
     */
    public void setBaseComponentId(String baseComponentId) {
        this.baseComponentId = baseComponentId;
    }

    /**
     * Looks up the parent naming container for the component source of the action event, and reset components
     * recursively within this container.
     *
     * @since 5.9.1
     */
    public void resetComponentsFor(ActionEvent event) {
        resetComponentsFor((FacesEvent) event);
    }

    /**
     * Looks up the parent naming container for the corresponding ajax behavior event, and reset components recursively
     * within this container.
     *
     * @since 8.1
     */
    public void resetComponentsFor(AjaxBehaviorEvent event) {
        resetComponentsFor((FacesEvent) event);
    }

    protected void resetComponentsFor(FacesEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        String baseCompId = ComponentUtils.getAttributeValue(component, "target", String.class, null, false);
        if (baseCompId == null) {
            // compat
            baseCompId = getBaseComponentId();
        }
        if (baseCompId != null) {
            String[] split = baseCompId.split("\\s");
            if (split != null) {
                for (String item : split) {
                    if (!StringUtils.isBlank(item)) {
                        UIComponent anchor = ComponentRenderUtils.getComponent(component, item);
                        resetComponentResursive(anchor);
                    }
                }
            }
        } else {
            log.error("No base component id given => cannot reset components state.");
        }
    }

    /**
     * Looks up the parent naming container for the component source of the action event, and reset components
     * recursively within this container.
     */
    public void resetComponents(ActionEvent event) {
        resetComponents((FacesEvent) event);
    }

    /**
     * Looks up the parent naming container for the component source of the action event, and reset components
     * recursively within this container.
     *
     * @since 6.0
     */
    public void resetComponents(AjaxBehaviorEvent event) {
        resetComponents((FacesEvent) event);
    }

    protected void resetComponents(FacesEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        // take first anchor and force flush on every resettable component
        UIComponent anchor = component.getNamingContainer();
        if (anchor == null) {
            resetComponentResursive(component);
        } else {
            resetComponentResursive(anchor);
        }
    }

    /**
     * Resets the given component.
     * <p>
     * Does not reset the component children.
     */
    public void resetComponent(UIComponent component) {
        resetComponent(component, false);
    }

    /**
     * Resets the given component and its children recursively.
     */
    public void resetComponentResursive(UIComponent parent) {
        resetComponent(parent, true);
    }

    protected void resetComponent(UIComponent comp, boolean recursive) {
        if (comp == null) {
            return;
        }
        if (comp instanceof ResettableComponent) {
            ((ResettableComponent) comp).resetCachedModel();
        } else {
            if (comp instanceof EditableValueHolder) {
                // reset submitted value
                ((EditableValueHolder) comp).setSubmittedValue(null);
            }
            if (comp instanceof ValueHolder) {
                // reset local value, only if there's a value expression
                // binding
                ValueExpression ve = comp.getValueExpression("value");
                if (ve != null) {
                    ValueHolder vo = (ValueHolder) comp;
                    vo.setValue(null);
                    if (comp instanceof EditableValueHolder) {
                        ((EditableValueHolder) comp).setLocalValueSet(false);
                    }
                }
            }
        }
        if (recursive) {
            List<UIComponent> children = comp.getChildren();
            if (children != null && !children.isEmpty()) {
                for (UIComponent child : children) {
                    resetComponent(child, recursive);
                }
            }
        }
    }

}
