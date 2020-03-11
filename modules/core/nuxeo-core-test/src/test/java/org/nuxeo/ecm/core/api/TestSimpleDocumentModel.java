/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestSimpleDocumentModel {

    @Test
    public void testPropertyNotModifiedAreNotDirty() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        assertFalse(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty1() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.setPropertyValue("dc:title", "toto");
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty2() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.getProperty("dc:title").setValue("toto");
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty3() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        Map<String, Object> values = new HashMap<>();
        values.put("title", "toto");
        doc.setProperties("dublincore", values);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty1() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.setPropertyValue("dc:title", doc.getPropertyValue("dc:title"));
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty2() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.setPropertyValue("dc:title", doc.getProperty("dc:title").getValue());
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty3() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.setPropertyValue("dc:title", null);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty4() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.getProperty("dc:title").setValue(doc.getPropertyValue("dc:title"));
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty5() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.getProperty("dc:title").setValue(doc.getProperty("dc:title").getValue());
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty6() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        doc.getProperty("dc:title").setValue(null);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty7() throws Exception {
        SimpleDocumentModel doc = SimpleDocumentModel.empty();
        Map<String, Object> values = new HashMap<>();
        values.put("title", null);
        doc.setProperties("dublincore", values);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

}
