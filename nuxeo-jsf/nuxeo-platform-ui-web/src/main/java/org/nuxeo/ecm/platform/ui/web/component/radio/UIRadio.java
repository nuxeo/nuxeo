/*
 * (C) Copyright 2014 JBoss RichFaces and others.
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

package org.nuxeo.ecm.platform.ui.web.component.radio;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import com.sun.faces.renderkit.SelectItemsIterator;

/**
 * Component representing a single radio button, referencing the original select and an index, for original button
 * attributes retrieval.
 *
 * @since 6.0
 */
public class UIRadio extends UIOutput implements ClientBehaviorHolder {

    public static final String COMPONENT_FAMILY = "org.nuxeo.Radio";

    public static final String COMPONENT_TYPE = "org.nuxeo.Radio";

    enum PropertyKeys {
        forValue("for"), index, onChange;

        String toString;

        PropertyKeys(String toString) {
            this.toString = toString;
        }

        PropertyKeys() {
        }

        @Override
        public String toString() {
            return ((toString != null) ? toString : super.toString());
        }
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public static List<SelectItem> getSelectItems(FacesContext context, UIComponent component) {
        if (context == null) {
            throw new IllegalArgumentException("Faces context is null");
        }

        ArrayList<SelectItem> list = new ArrayList<>();
        final SelectItemsIterator<SelectItem> iterator = new SelectItemsIterator<>(context, component);
        while (iterator.hasNext()) {
            final SelectItem next = iterator.next();
            list.add(new SelectItem(next.getValue(), next.getLabel(), next.getDescription(), next.isDisabled(),
                    next.isEscape(), next.isNoSelectionOption()));
        }
        return list;
    }

    public String getFor() {
        return (String) getStateHelper().eval(PropertyKeys.forValue);
    }

    public void setFor(String forValue) {
        getStateHelper().put(PropertyKeys.forValue, forValue);
    }

    public Integer getIndex() {
        return (Integer) getStateHelper().eval(PropertyKeys.index);
    }

    public void setIndex(Integer index) {
        getStateHelper().put(PropertyKeys.index, index);
    }

    @SuppressWarnings("boxing")
    public SelectItem getSelectItem(FacesContext context, UIComponent targetComponent) {
        final List<SelectItem> list = getSelectItems(context, targetComponent);
        try {
            return list.get(getIndex());
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Component ''" + getId() + "'' has wrong value of index attribute ("
                    + getIndex() + "). Target component ''" + targetComponent.getId() + "'' has only " + list.size()
                    + " items.");
        }
    }

}
