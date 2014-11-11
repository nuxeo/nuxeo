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

import java.util.regex.Pattern;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RegexValidator implements FieldValidator {

    protected String regex;
    protected Pattern pattern;

    public RegexValidator(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    public void validate(String value, Object decoded) throws ValidationException {
        if (!pattern.matcher(value).matches()) {
            throw new ValidationException();
        }
    }

}
