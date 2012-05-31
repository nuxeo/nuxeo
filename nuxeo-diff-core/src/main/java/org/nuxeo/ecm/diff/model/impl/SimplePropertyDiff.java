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

import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.PropertyDiff;

/**
 * Implementation of {@code PropertyDiff} for a simple property.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class SimplePropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    protected String leftValue;

    protected String rightValue;

    /**
     * Instantiates a new simple property diff with a property type.
     *
     * @param propertyType the property type
     */
    public SimplePropertyDiff(String propertyType) {
        this.propertyType = propertyType;
    }

    /**
     * Instantiates a new simple property diff with a property type, the
     * {@link DifferenceType#different} difference type, a left value and right
     * value.
     *
     * @param propertyType the property type
     * @param leftValue the left value
     * @param rightValue the right value
     */
    public SimplePropertyDiff(String propertyType, String leftValue,
            String rightValue) {

        this(propertyType, DifferenceType.different, leftValue, rightValue);
    }

    /**
     * Instantiates a new simple property diff with a property type, difference
     * type, left value and right value.
     *
     * @param propertyType the property type
     * @param differenceType the difference type
     * @param leftValue the left value
     * @param rightValue the right value
     */
    public SimplePropertyDiff(String propertyType,
            DifferenceType differenceType, String leftValue, String rightValue) {

        this.propertyType = propertyType;
        this.differenceType = differenceType;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    @Override
    public boolean equals(Object other) {

        if (!super.equals(other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimplePropertyDiff)) {
            return false;
        }

        String otherLeftValue = ((SimplePropertyDiff) other).getLeftValue();
        String otherRightValue = ((SimplePropertyDiff) other).getRightValue();

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
        sb.append(super.toString());

        return sb.toString();
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }
}
