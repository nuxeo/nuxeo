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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EnumerationValidator implements FieldValidator {

    protected Set<String> values;

    public EnumerationValidator(String expr) {
        values = new HashSet<>();
        String[] vals = StringUtils.split(expr, ',', true);
        for (String v : vals) {
            values.add(v);
        }
    }

    public EnumerationValidator(String[] values) {
        this.values = new HashSet<>();
        for (String v : values) {
            this.values.add(v);
        }
    }

    @Override
    public void validate(String value, Object decoded) throws ValidationException {
        if (!values.contains(value)) {
            throw new ValidationException();
        }
    }

}
