/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.DummyLoginFeature;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, DummyLoginFeature.class })
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-secured-types-contrib.xml")
@WithUser("john")
public class TestDocumentModelWithSecuredProperty {

    protected static final String SECURED_SCHEMA = "secured";

    @Test
    public void testSetSecuredScalarProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "scalar", "test secure");
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: scalar since it is readonly", e.getMessage());
        }
    }

    @Test
    public void testSetSecuredScalarPropertyWithSystem() {
        Framework.doPrivileged(() -> {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "scalar", "test secure");
            assertEquals("test secure", doc.getProperty(SECURED_SCHEMA, "scalar"));
        });
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredScalarPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "scalar", "test secure");
        assertEquals("test secure", doc.getProperty(SECURED_SCHEMA, "scalar"));
    }

    @Test
    public void testSetUnsecuredScalarProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "unsecureScalar", "test unsecure");
        assertEquals("test unsecure", doc.getProperty(SECURED_SCHEMA, "unsecureScalar"));
    }

    @Test
    public void testSetSecuredComplexProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "complex", Map.of("scalar1", "test secure1", "scalar2", "test secure2"));
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: complex since it is readonly", e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredComplexPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "complex", Map.of("scalar1", "test secure1", "scalar2", "test secure2"));
        assertEquals("test secure1", doc.getProperty(SECURED_SCHEMA, "complex/scalar1"));
        assertEquals("test secure2", doc.getProperty(SECURED_SCHEMA, "complex/scalar2"));
    }

    @Test
    public void testSetUnsecuredComplexProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        // unsecureComplex/scalar1 is secured
        doc.setProperty(SECURED_SCHEMA, "unsecureComplex", Map.of("scalar2", "test secure2"));
        assertEquals("test secure2", doc.getProperty(SECURED_SCHEMA, "unsecureComplex/scalar2"));
    }

    @Test
    public void testSetSecuredComplexItemProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "unsecureComplex", Map.of("scalar1", "test secure1"));
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: unsecureComplex/scalar1 since it is readonly",
                    e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredComplexItemPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "unsecureComplex", Map.of("scalar1", "test secure1"));
        assertEquals("test secure1", doc.getProperty(SECURED_SCHEMA, "unsecureComplex/scalar1"));
    }

    @Test
    public void testSetSecuredListProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "list",
                    List.of(Map.of("scalar1", "test secure1"), Map.of("scalar2", "test secure2")));
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: list since it is readonly", e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredListPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "list",
                List.of(Map.of("scalar1", "test secure1"), Map.of("scalar2", "test secure2")));
        assertEquals("test secure1", doc.getProperty(SECURED_SCHEMA, "list/0/scalar1"));
        assertEquals("test secure2", doc.getProperty(SECURED_SCHEMA, "list/1/scalar2"));
    }

    @Test
    public void testSetUnsecuredListProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        // unsecureList/*/scalar1 is secured
        doc.setProperty(SECURED_SCHEMA, "unsecureList", List.of(Map.of("scalar2", "test secure2")));
        assertEquals("test secure2", doc.getProperty(SECURED_SCHEMA, "unsecureList/0/scalar2"));
    }

    @Test
    public void testSetSecuredListItemProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "unsecureList", List.of(Map.of("scalar1", "test secure1")));
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: unsecureList/-1/scalar1 since it is readonly",
                    e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredListItemPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "unsecureList", List.of(Map.of("scalar1", "test secure1")));
        assertEquals("test secure1", doc.getProperty(SECURED_SCHEMA, "unsecureList/0/scalar1"));
    }

    @Test
    public void testSetSecuredArrayProperty() {
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
            doc.setProperty(SECURED_SCHEMA, "array", List.of("test secure1", "test secure2"));
            fail("A ReadOnlyPropertyException should have been thrown");
        } catch (ReadOnlyPropertyException e) {
            assertEquals("Cannot set the value of property: array since it is readonly", e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testSetSecuredArrayPropertyWithAdministrator() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Secured");
        doc.setProperty(SECURED_SCHEMA, "array", List.of("test secure1", "test secure2"));
        assertArrayEquals(new String[] { "test secure1", "test secure2" },
                (String[]) doc.getProperty(SECURED_SCHEMA, "array"));
    }

}
