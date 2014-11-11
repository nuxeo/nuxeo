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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.forms.validation;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RangeValidator implements FieldValidator {

    protected boolean negate;
    protected double min = Double.MIN_VALUE;
    protected double max = Double.MAX_VALUE;

    public RangeValidator(double min, double max, boolean negate) {
        this.min = min;
        this.max = max;
        this.negate = negate;
    }

    public boolean validateNumber(Number value) {
        double d = value.doubleValue();
        boolean result = d > min && d < max;
        return negate ? !result : result;
    }

    public void validate(String value, Object decoded) throws ValidationException {
        if (!validateNumber((Number)decoded)) {
            throw new ValidationException();
        }
    }

}
