/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This constraint ensure some object is not null.
 * <p>
 * This class is a singleton. Use {@link #get()} to get the singleton.
 * </p>
 *
 * @since 7.1
 * @author <a href="mailto:nc@nuxeo.com">Nicolas Chapurlat</a>
 */
public class NotNullConstraint implements Constraint {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "NotNullConstraint";

    // Lazy ang thread safe singleton instance.
    private static class Holder {
        private static final NotNullConstraint INSTANCE = new NotNullConstraint();
    }

    private NotNullConstraint() {
    }

    public static NotNullConstraint get() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean validate(Object object) {
        return object != null;
    }

    /**
     * Here, value is : <br>
     * name = {@value #NAME}. <br>
     * parameters is empty
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        return new Description(NotNullConstraint.NAME, params);
    }

    @Override
    public int hashCode() {
        return NotNullConstraint.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof NotNullConstraint;
    }
}
