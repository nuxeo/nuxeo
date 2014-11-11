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

package org.nuxeo.ecm.webengine.forms.validation.constraints;

import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.validation.Constraint;
import org.nuxeo.ecm.webengine.forms.validation.Field;
import org.nuxeo.ecm.webengine.forms.validation.Status;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Not extends ContainerConstraint {

    @Override
    public void add(Constraint constraint) {
        if (children.isEmpty()) {
            super.add(constraint);
        } else {
            throw new IllegalStateException("[Not] constraint is accepting only one child constraint");
        }
    }

    @Override
    public Status validate(FormInstance form, Field field, String rawValue, Object value) {
        assert !children.isEmpty();
        Status status = children.get(0).validate(form, field, rawValue, value);
        return status.negate();
    }

    @Override
    public String toString() {
        Constraint c = children.get(0);
        StringBuilder buf = new StringBuilder();
        if (c.isContainer()) {
            buf.append("NOT (").append(c).append(")");
        } else {
            buf.append("NOT ").append(c);
        }
        return buf.toString();
    }
}
