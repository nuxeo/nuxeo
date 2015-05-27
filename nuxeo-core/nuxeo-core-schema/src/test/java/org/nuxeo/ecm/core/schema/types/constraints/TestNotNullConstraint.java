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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestNotNullConstraint {

    @Test
    public void testNotNullConstraintSupportedType() {
        assertTrue(DoubleType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(IntegerType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(LongType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(StringType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(DateType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(BinaryType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(BooleanType.INSTANCE.support(NotNullConstraint.class));
    }

    @Test
    public void testNotNullConstraintNominals() {
        assertTrue(NotNullConstraint.get().validate(123));
        assertTrue(NotNullConstraint.get().validate("123"));
        assertTrue(NotNullConstraint.get().validate(NotNullConstraint.get()));
        assertFalse(NotNullConstraint.get().validate(null));
    }

    @Test
    public void testNotNullConstraintDescription() {
        Constraint.Description description = NotNullConstraint.get().getDescription();
        assertEquals("NotNullConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(0, params.size());
    }

}
