/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;

/**
 * Default implementation of {@link PropertyDiffDisplay}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class PropertyDiffDisplayImpl implements PropertyDiffDisplay {

    private static final long serialVersionUID = 875764424409126845L;

    protected Serializable value;

    protected String styleClass = DEFAULT_STYLE_CLASS;

    public PropertyDiffDisplayImpl(Serializable value) {
        this.value = value;
    }

    public PropertyDiffDisplayImpl(Serializable value, String styleClass) {
        this.value = value;
        this.styleClass = styleClass;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof PropertyDiffDisplay)) {
            return false;
        }
        Serializable otherValue = ((PropertyDiffDisplay) other).getValue();
        Serializable otherStyleClass = ((PropertyDiffDisplay) other).getStyleClass();
        if (value == null && otherValue == null && styleClass == null
                && otherStyleClass == null) {
            return true;
        }
        return value == null && otherValue == null && styleClass != null
                && styleClass.equals(otherStyleClass) || styleClass == null
                && otherStyleClass == null && value != null
                && value.equals(otherValue) || value != null
                && value.equals(otherValue) && styleClass != null
                && styleClass.equals(otherStyleClass);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        sb.append(" (");
        sb.append(styleClass);
        sb.append(")");
        return sb.toString();
    }
}
