/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
        HashMap<String, String> parameters = new HashMap<>();
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
