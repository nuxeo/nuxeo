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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.validation.constraints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.webengine.validation.Field;
import org.nuxeo.ecm.webengine.validation.ValidationStatus;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Like extends AbstractConstraint {

    protected Pattern pattern;

    @Override
    public void init(Field field, String value) {
        pattern = Pattern.compile(value);
    }

    @Override
    public ValidationStatus validate(Field field, String rawValue, Object value) {
        assert pattern != null;
        Matcher m = pattern.matcher(rawValue);
        return m.matches() ? ValidationStatus.OK
                : new ValidationStatus(false, field.getId());
    }

    @Override
    public String toString() {
        return "~"+pattern;
    }

}
