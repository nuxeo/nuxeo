/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
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
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.diff.test")
@Deploy("org.nuxeo.diff.test:OSGI-INF/test-diff-constraint-types-contrib.xml")
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
