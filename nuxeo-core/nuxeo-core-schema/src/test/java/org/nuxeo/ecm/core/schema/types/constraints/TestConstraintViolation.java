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

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.XSDLoader;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintViolation.PathNode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestConstraintViolation extends NXRuntimeTestCase {

    private Schema schema;

    private Field field;

    private PatternConstraint constraint;

    private static final String INVALID = "   ";

    private ConstraintViolation violation;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        SchemaManager typeMgr = Framework.getLocalService(SchemaManager.class);
        XSDLoader reader = new XSDLoader((SchemaManagerImpl) typeMgr);
        URL url = getResource("schema/testrestriction.xsd");
        schema = reader.loadSchema("testrestriction", "", url);
        field = schema.getField("firstname");
        constraint = ConstraintUtils.getConstraint(field.getConstraints(), PatternConstraint.class);
        violation = new ConstraintViolation(schema, Arrays.asList(new PathNode(field)), constraint, INVALID);
    }

    @Test
    public void testGenericMessage() throws Exception {
        String expected = "generic";
        String message = violation.getMessage(Locale.ENGLISH);
        assertEquals(expected, message);
    }

    @Test
    public void testConstraintSpecificMessage() throws Exception {
        String expected = "constraint";
        String message = violation.getMessage(Locale.ITALIAN);
        assertEquals(expected, message);
    }

    @Test
    public void testFieldSpecificMessage() throws Exception {
        String expected = "field";
        String message = violation.getMessage(Locale.FRENCH);
        assertEquals(expected, message);
    }

    @Test
    public void testNoTranslationForLanguageRepliesToDefault() throws Exception {
        String expected = "generic";
        String message = violation.getMessage(Locale.GERMAN);
        assertEquals(expected, message);
    }

}
