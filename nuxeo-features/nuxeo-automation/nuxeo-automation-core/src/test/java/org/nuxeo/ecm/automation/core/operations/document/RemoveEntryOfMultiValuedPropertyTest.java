/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:OSGI-INF/my-automation-doc-type-contrib.xml")
public class RemoveEntryOfMultiValuedPropertyTest {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    private RemoveEntryOfMultiValuedProperty operation;

    @Before
    public void setup() throws Exception {
        doc = session.createDocumentModel("/", "test", "MyDocument");
        List<String> values = new ArrayList<String>();
        values.add("Test");
        values.add("Test2");
        values.add("Test2");
        doc.setPropertyValue("lists:string", (Serializable) values);
        session.createDocument(doc);
        session.save();

        operation = new RemoveEntryOfMultiValuedProperty();
        operation.save = true;
        operation.session = session;
        operation.value = "Test";
        operation.xpath = "lists:string";
    }

    @Test
    public void shouldRemoveAllEntryIntoStringList() throws Exception {
        String[] value = (String[]) doc.getPropertyValue("lists:string");
        assertEquals(3, value.length);

        operation.run(doc);
        value = (String[]) doc.getPropertyValue("lists:string");
        assertEquals(2, value.length);
        assertEquals("Test2", value[0]);
        assertEquals("Test2", value[1]);

        operation.value = "Test2";
        operation.run(doc);
        value = (String[]) doc.getPropertyValue("lists:string");
        assertEquals(0, value.length);
    }

    @Test
    public void shouldOnlyOneEntryIntoStringList() throws Exception {
        String[] value = (String[]) doc.getPropertyValue("lists:string");
        assertEquals(3, value.length);

        operation.value = "Test2";
        operation.isRemoveAll = false;
        operation.run(doc);
        value = (String[]) doc.getPropertyValue("lists:string");
        assertEquals(2, value.length);
    }

    @Test
    public void shouldFailedForComplextType() throws Exception {
        doc = session.createDocumentModel("/", "test", "File");
        session.createDocument(doc);
        session.save();

        operation.xpath = "files:files";

        try {
            operation.run(doc);
            fail();
        } catch (UnsupportedOperationException e) {
            assertEquals("Manage only lists of scalar items", e.getMessage());
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void shouldFailedIfToTypeNotMatch() throws Exception {
        operation.value = Boolean.FALSE;

        try {
            operation.run(doc);
            fail();
        } catch (UnsupportedOperationException e) {
            assertEquals("Given type \"false\" value is not a string type",
                    e.getMessage());
        } catch (Exception e) {
            fail();
        }

    }

}
