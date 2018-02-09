/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static junit.framework.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:OSGI-INF/my-automation-doc-type-contrib.xml")
public class RemoveEntryOfMultiValuedPropertyTest {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    private RemoveEntryOfMultiValuedProperty operation;

    private Calendar now = Calendar.getInstance();

    @Before
    public void setup() throws Exception {
        doc = session.createDocumentModel("/", "test", "MyDocument");
        List<String> values = new ArrayList<String>();
        values.add("Test");
        values.add("Test2");
        values.add("Test2");
        doc.setPropertyValue("lists:string", (Serializable) values);

        List<Integer> integerValues = new ArrayList<>();
        integerValues.add(1);
        integerValues.add(2);
        doc.setPropertyValue("lists:integer", (Serializable) integerValues);

        List<Calendar> calendarValues = new ArrayList<>();
        calendarValues.add(now);
        calendarValues.add(now);
        doc.setPropertyValue("lists:date", (Serializable) calendarValues);

        List<Double> doubleValues = new ArrayList<>();
        doubleValues.add(1.0);
        doubleValues.add(2.0);
        doc.setPropertyValue("lists:double", (Serializable) doubleValues);

        List<Boolean> booleanValues = new ArrayList<>();
        booleanValues.add(Boolean.TRUE);
        booleanValues.add(Boolean.FALSE);
        doc.setPropertyValue("lists:boolean", (Serializable) booleanValues);

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
    public void shouldRemoveDifferentEntryIntoList() throws Exception {
        operation.xpath = "lists:double";
        operation.value = 2.0;
        operation.run(doc);
        Double[] doubleValues = (Double[]) doc.getPropertyValue("lists:double");
        assertEquals(1, doubleValues.length);

        operation.xpath = "lists:boolean";
        operation.value = false;
        operation.run(doc);
        Boolean[] booleanValues = (Boolean[]) doc.getPropertyValue("lists:boolean");
        assertEquals(1, booleanValues.length);

        operation.xpath = "lists:integer";
        operation.value = 1L;
        operation.run(doc);
        Long[] longValues = (Long[]) doc.getPropertyValue("lists:integer");
        assertEquals(1, longValues.length);

        operation.xpath = "lists:date";
        operation.value = now;
        operation.run(doc);
        Calendar[] dateValues = (Calendar[]) doc.getPropertyValue("lists:date");
        assertEquals(0, dateValues.length);
    }

}
