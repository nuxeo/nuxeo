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

import org.nuxeo.ecm.webengine.validation.Field;
import org.nuxeo.ecm.webengine.validation.ValidationStatus;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Lt extends AbstractConstraint {

    protected Comparable value;

    @Override
    public void init(Field field, String value) {
        Object c = field.decode(value);
        if (!(c instanceof Comparable)) {
            throw new IllegalArgumentException("Only Comparable objects are supported: "+this.value.getClass());
        }
        this.value = (Comparable)c;
    }


    @Override
    @SuppressWarnings("unchecked")
    public ValidationStatus validate(Field field, String rawValue, Object value) {
        assert this.value != null;
        return this.value.compareTo((Comparable)value) > 0 ? ValidationStatus.OK
                : new ValidationStatus(false, field.getId());
    }
    @Override
    public String toString() {
        return "<"+value;
    }

}
