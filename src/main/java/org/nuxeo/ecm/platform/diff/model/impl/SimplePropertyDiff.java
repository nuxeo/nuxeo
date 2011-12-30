/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.diff.model.impl;

import org.nuxeo.ecm.platform.diff.model.PropertyDiff;

/**
 * Implementation of PropertyDiff for a simple property.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class SimplePropertyDiff implements PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    private String leftValue;

    private String rightValue;

    /**
     * Instantiates a new simple property diff.
     * 
     * @param leftValue the left value
     * @param rightValue the right value
     */
    public SimplePropertyDiff(String leftValue, String rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
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

    @Override
    public boolean equals(Object other) {

        boolean res = false;

        if (other != null && other instanceof SimplePropertyDiff) {

            String objLeftValue = ((SimplePropertyDiff) other).getLeftValue();
            String objRightValue = ((SimplePropertyDiff) other).getRightValue();

            res = (leftValue == null && objLeftValue == null
                    && rightValue == null && objRightValue == null)
                    || (leftValue == null && objLeftValue == null
                            && rightValue != null && rightValue.equals(objRightValue))
                    || (rightValue == null && objRightValue == null
                            && leftValue != null && leftValue.equals(objLeftValue))
                    || (leftValue != null && rightValue != null
                            && leftValue.equals(objLeftValue) && rightValue.equals(objRightValue));
        }

        return res;
    }
}
