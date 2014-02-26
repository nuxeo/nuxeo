/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.component;

import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ui.util.cdk.RendererUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.richfaces.renderkit.util.AjaxRendererUtils;

/**
 * Managed bean, request-scoped, that resets the components value for ajax
 * interactions.
 * <p>
 * Resets are done using the following rule:
 * <ul>
 * <li>if the component implements {@link ResettableComponent} interface, its
 * method {@link ResettableComponent#resetCachedModel()} is called</li>
 * <li>if the component implements {@link EditableValueHolder}, its submitted
 * value is reset, and its local value is reset only if it holds a value
 * binding for the "value" attribute</li>
 * <li>if the component implements {@link ValueHolder}, its its local value is
 * reset only if it holds a value binding for the "value" attribute.</li>
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
     * Returns the base component id, if {@link #setBaseComponentId(String)}
     * was previously called in the same request, or null.
     *
     * @since 5.9.1
     */
    public String getBaseComponentId() {
        return baseComponentId;
    }

    /**
     * Sets the base component id so that
     * {@link #resetComponentsFor(ActionEvent)} can look it up in the hierarchy
     * of components, and reset component states recursively from it.
     *
     * @since 5.9.1
     * @see #resetComponentsFor(ActionEvent)
     */
    public void setBaseComponentId(String baseComponentId) {
        this.baseComponentId = baseComponentId;
    }

    /**
     * Looks up the parent naming container for the component source of the
     * action event, and reset components recursively within this container.
     *
     * @since 5.9.1
     */
    public void resetComponentsFor(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        String baseCompId = getBaseComponentId();
        if (baseCompId != null) {
            FacesContext ctx = FacesContext.getCurrentInstance();
            // TODO migrate to JSF2
            // UIComponent target = component.findComponentFor(
            // component, baseCompId);
            // if (target != null) {
            // baseCompId = AjaxRendererUtils.getAbsoluteId(target);
            // }
            // UIComponent anchor =
            // ctx.getViewRoot().findComponent(baseCompId);
            // resetComponentResursive(anchor);
        } else {
            log.error("No base component id given => cannot reset "
                    + "components state.");
        }
    }

    /**
     * Looks up the parent naming container for the component source of the
     * action event, and reset components recursively within this container.
     */
    public void resetComponents(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        // take first anchor and force flush on every resettable component
        UIComponent anchor = ComponentUtils.getBase(component);
        resetComponentResursive(anchor);
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
