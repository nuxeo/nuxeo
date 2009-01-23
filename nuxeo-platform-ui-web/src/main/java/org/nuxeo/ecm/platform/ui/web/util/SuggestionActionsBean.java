/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: PleiadeSuggestionActionsBean.java 59340 2008-12-12 14:07:40Z cbaican $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import static org.jboss.seam.ScopeType.STATELESS;

import java.io.Serializable;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.event.ActionEvent;

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
@Scope(STATELESS)
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
     * Gets the base naming container from anchor.
     * <p>
     * Gets out of suggestion box as it's a naming container and we can't get
     * components out of it with a relative path => take above first found
     * container.
     */
    protected UIComponent getBase(UIComponent anchor) {
        UIComponent base = anchor;
        boolean firstFound = false;
        while (base.getParent() != null) {
            if (base instanceof NamingContainer) {
                if (firstFound) {
                    break;
                } else {
                    firstFound = true;
                }
            }
            base = base.getParent();
        }
        return base;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getComponent(UIComponent base, String componentId,
            Class<T> expectedComponentClass) {
        if (componentId == null) {
            log.error("Cannot retrieve component with a null id");
            return null;
        }
        try {
            UIComponent component = base.findComponent(componentId);
            if (component == null) {
                log.error("Could not find component with id: " + componentId);
            } else {
                try {
                    return (T) component;
                } catch (ClassCastException e) {
                    log.error(String.format(
                            "Invalid component with id %s: %s, expected a "
                                    + "component with interface %s",
                            componentId, component, expectedComponentClass));
                }
            }
        } catch (Exception e) {
            log.error("Error when trying to retrieve component with id "
                    + componentId, e);
        }
        return null;
    }

    /**
     * Adds selection from selector as a list element
     */
    public void addSelectionToList(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = getBase(component);
        ValueHolder selector = getComponent(base, suggestionInputSelectorId,
                ValueHolder.class);
        UIEditableList list = getComponent(base, suggestionSelectionListId,
                UIEditableList.class);

        if (selector != null && list != null) {
            // add selected value to the list
            list.addValue(selector.getValue());
        }
    }

    /**
     * Adds selection from selector as single element
     */
    public void addSingleSelection(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = getBase(component);
        ValueHolder selector = getComponent(base, suggestionInputSelectorId,
                ValueHolder.class);
        EditableValueHolder mailbox = getComponent(base,
                suggestionSelectionHiddenId, EditableValueHolder.class);
        ValueHolder mailboxOutput = getComponent(base,
                suggestionSelectionOutputId, ValueHolder.class);

        if (selector != null && mailbox != null && mailboxOutput != null) {
            String mailboxId = (String) selector.getValue();

            mailboxOutput.setValue(mailboxId);
            mailbox.setSubmittedValue(mailboxId);
        }
    }

    /**
     * Clears single selection
     */
    public void clearSingleSelection(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component == null) {
            return;
        }
        UIComponent base = component;
        ValueHolder selector = getComponent(base, suggestionInputSelectorId,
                ValueHolder.class);

        EditableValueHolder mailbox = getComponent(base,
                suggestionSelectionHiddenId, EditableValueHolder.class);
        ValueHolder mailboxOutput = getComponent(base,
                suggestionSelectionOutputId, ValueHolder.class);

        if (selector != null && mailbox != null && mailboxOutput != null) {
            mailboxOutput.setValue("");
            mailbox.setSubmittedValue("");
        }
    }

}
