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
        SimpleDocumentModel doc = new SimpleDocumentModel();
        assertFalse(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty1() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.setPropertyValue("dc:title", "toto");
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty2() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.getProperty("dc:title").setValue("toto");
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedAreDirty3() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("title", "toto");
        doc.setProperties("dublincore", values);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty1() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.setPropertyValue("dc:title", doc.getPropertyValue("dc:title"));
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty2() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.setPropertyValue("dc:title", doc.getProperty("dc:title").getValue());
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty3() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.setPropertyValue("dc:title", null);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty4() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.getProperty("dc:title").setValue(doc.getPropertyValue("dc:title"));
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty5() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.getProperty("dc:title").setValue(doc.getProperty("dc:title").getValue());
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty6() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        doc.getProperty("dc:title").setValue(null);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

    @Test
    public void testPropertyUpdatedWithSameValueAreDirty7() throws Exception {
        SimpleDocumentModel doc = new SimpleDocumentModel();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("title", null);
        doc.setProperties("dublincore", values);
        assertTrue(doc.getProperty("dc:title").isDirty());
    }

}
