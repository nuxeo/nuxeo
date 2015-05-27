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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.COLOR_MODE;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.MODE;

public class TestObjectResolverConstraint {

    protected static ObjectResolverConstraint constraint;

    @BeforeClass
    public static void setUp() {
        TestingColorResolver resolver = new TestingColorResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(COLOR_MODE, MODE.PRIMARY.name());
        resolver.configure(parameters);
        constraint = new ObjectResolverConstraint(resolver);
    }

    @Test
    public void testSucceedOnGoodValue() throws Exception {
        assertTrue(constraint.validate("RED"));
    }

    @Test
    public void testSucceedOnNull() throws Exception {
        assertTrue(constraint.validate(null));
    }

    @Test
    public void testFailOnInvalidValue1() throws Exception {
        assertFalse(constraint.validate("VIOLET"));
    }

    @Test
    public void testFailOnInvalidValue2() throws Exception {
        assertFalse(constraint.validate("prosper"));
    }

}
