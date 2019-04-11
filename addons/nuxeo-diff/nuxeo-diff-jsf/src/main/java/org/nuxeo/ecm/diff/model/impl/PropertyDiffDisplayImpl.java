/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.diff.model.DifferenceType;
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

    protected DifferenceType differenceType;

    protected String styleClass = DEFAULT_STYLE_CLASS;

    public PropertyDiffDisplayImpl(Serializable value) {
        this(value, DifferenceType.different);
    }

    public PropertyDiffDisplayImpl(Serializable value, DifferenceType differenceType) {
        this(value, differenceType, null);
    }

    public PropertyDiffDisplayImpl(Serializable value, String styleClass) {
        this(value, DifferenceType.different, styleClass);
    }

    public PropertyDiffDisplayImpl(Serializable value, DifferenceType differenceType, String styleClass) {
        this.value = value;
        this.differenceType = differenceType;
        this.styleClass = styleClass;
    }

    @Override
    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    @Override
    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    @Override
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
        DifferenceType otherDifferenceType = ((PropertyDiffDisplay) other).getDifferenceType();
        if (value == null && otherValue == null && styleClass == null && otherStyleClass == null
                && differenceType.equals(otherDifferenceType)) {
            return true;
        }
        return differenceType.equals(otherDifferenceType)
                && (value == null && otherValue == null && styleClass != null && styleClass.equals(otherStyleClass)
                        || styleClass == null && otherStyleClass == null && value != null && value.equals(otherValue) || value != null
                        && value.equals(otherValue) && styleClass != null && styleClass.equals(otherStyleClass));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        sb.append(" / ");
        sb.append(differenceType.name());
        sb.append(" / ");
        sb.append(styleClass);
        return sb.toString();
    }
}
