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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.diff.model.DiffDisplayField;

/**
 * Implementation of PropertyDiff for a simple property.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class SimpleDiffDisplayField implements DiffDisplayField {

    private static final long serialVersionUID = 4024413081969146478L;

    protected Serializable leftValue;

    protected Serializable rightValue;

    /**
     * Instantiates a new simple property diff.
     * 
     * @param propertyType the property type
     */
    public SimpleDiffDisplayField(Serializable leftValue,
            Serializable rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public Serializable getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(Serializable leftValue) {
        this.leftValue = leftValue;
    }

    public Serializable getRightValue() {
        return rightValue;
    }

    public void setRightValue(Serializable rightValue) {
        this.rightValue = rightValue;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleDiffDisplayField)) {
            return false;
        }

        Serializable otherLeftValue = ((SimpleDiffDisplayField) other).getLeftValue();
        Serializable otherRightValue = ((SimpleDiffDisplayField) other).getRightValue();

        return (leftValue == null && otherLeftValue == null
                && rightValue == null && otherRightValue == null)
                || (leftValue == null && otherLeftValue == null
                        && rightValue != null && rightValue.equals(otherRightValue))
                || (rightValue == null && otherRightValue == null
                        && leftValue != null && leftValue.equals(otherLeftValue))
                || (leftValue != null && rightValue != null
                        && leftValue.equals(otherLeftValue) && rightValue.equals(otherRightValue));
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(leftValue);
        sb.append(" --> ");
        sb.append(rightValue);

        return sb.toString();
    }
}
