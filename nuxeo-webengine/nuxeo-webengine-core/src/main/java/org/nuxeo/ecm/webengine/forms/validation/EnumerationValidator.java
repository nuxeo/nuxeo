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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EnumerationValidator implements FieldValidator {

    protected Set<String> values;

    public EnumerationValidator(String expr) {
        values = new HashSet<String>();
        String[] vals = StringUtils.split(expr, ',', true);
        for (String v : vals) {
            values.add(v);
        }
    }

    public EnumerationValidator(String[] values) {
        this.values = new HashSet<String>();
        for (String v : values) {
            this.values.add(v);
        }
    }

    public void validate(String value, Object decoded) throws ValidationException {
        if (!values.contains(value)) {
            throw new ValidationException();
        }
    }

}
