/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import org.nuxeo.ecm.core.schema.types.Constraint;

/**
 * String length constraint.
 * <p>
 * The length constraint is not strict (i.e. >= and <=).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringLengthConstraint implements Constraint {

    /**
     *
     */
    private static final long serialVersionUID = 3630463971175189087L;
    private final int min;
    private final int max;

    public StringLengthConstraint(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean validate(Object object) {
        int len = object.toString().length();
        return len <= max && len >= min;
    }

}
