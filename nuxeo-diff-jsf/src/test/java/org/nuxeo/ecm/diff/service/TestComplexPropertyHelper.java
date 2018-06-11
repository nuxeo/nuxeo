/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the ComplexPropertyHelper class.
 * <p>
 * TODO: add tests
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.directory.api", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.diff.test", //
        "org.nuxeo.diff.test:OSGI-INF/test-diff-constraint-types-contrib.xml" //
})
public class TestComplexPropertyHelper {

    @Inject
    protected CoreSession session;

    @Test
    public void testGetField() {

        Field field = ComplexPropertyHelper.getField("simpletypes", "string");
        assertNotNull(field);
        assertEquals("string", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());

        field = ComplexPropertyHelper.getField("simpletypes", "textarea");
        assertNotNull(field);
        assertEquals("textarea", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
    }

    /**
     * @since 10.2
     */
    @Test
    public void testGetFieldType() {
        // primitive
        Field field = getField("simpletypes:string");
        assertEquals("string", ComplexPropertyHelper.getFieldType(field));
        field = getField("simpletypes:integer");
        assertEquals("long", ComplexPropertyHelper.getFieldType(field));

        // content
        field = getField("file:content");
        assertEquals(PropertyType.CONTENT, ComplexPropertyHelper.getFieldType(field));

        // complex
        field = getField("complextypes:complex");
        assertEquals(PropertyType.COMPLEX, ComplexPropertyHelper.getFieldType(field));

        // scalar list
        field = getField("simpletypes:multivalued");
        assertEquals(PropertyType.SCALAR_LIST, ComplexPropertyHelper.getFieldType(field));

        // complex list
        field = getField("complextypes:complexList");
        assertEquals(PropertyType.COMPLEX_LIST, ComplexPropertyHelper.getFieldType(field));

        // constrained string
        field = getField("constraints:string");
        assertEquals("string", ComplexPropertyHelper.getFieldType(field));

        // multivalued constrained string
        field = getField("constraints:multivaluedString");
        assertEquals(PropertyType.SCALAR_LIST, ComplexPropertyHelper.getFieldType(field));
        assertEquals("string", ComplexPropertyHelper.getFieldType(getListSubField(field)));

        // multivalued constrained string with directory resolver
        field = getField("constraints:multivaluedDirectory");
        assertEquals(PropertyType.SCALAR_LIST, ComplexPropertyHelper.getFieldType(field));
        assertEquals("string", ComplexPropertyHelper.getFieldType(getListSubField(field)));
    }

    protected Field getField(String xpath) {
        return Framework.getService(SchemaManager.class).getField(xpath);
    }

    protected Field getListSubField(Field listField) {
        return ((ListType) listField.getType()).getField();
    }
}
