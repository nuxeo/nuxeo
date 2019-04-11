/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public void validate(String value, Object decoded) throws ValidationException {
        if (!validateNumber((Number) decoded)) {
            throw new ValidationException();
        }
    }

}
