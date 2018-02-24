/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.inject.Inject;

import org.jboss.el.ExpressionFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests for the EL expression resolver for documemts.
 *
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestDocumentModelResolver {

    private final ExpressionTester et = new ExpressionTester(new ExpressionFactoryImpl());

    private class ExpressionTester extends ExpressionEvaluator {
        public ExpressionTester(ExpressionFactory factory) {
            super(factory);
        }

        public void setValue(ELContext context, String stringExpression, Object value) {
            ValueExpression ve = expressionFactory.createValueExpression(context, stringExpression, Object.class);
            ve.setValue(context, value);
        }
    }

    private final ExpressionContext context = new ExpressionContext();

    @Inject
    protected CoreSession session;

    DocumentModel doc;

    @Before
    public void setUp() throws Exception {
        doc = session.createDocumentModel("TestDocument2");
        doc.setPathInfo("/", "doc");
        doc.setPropertyValue("dc:title", "sample title");
        doc.setPropertyValue("tp:removedPropertyFallback", "sample prop");
        doc = session.createDocument(doc);

        et.bindValue(context, "doc", doc);
    }

    protected String checkStringValue(Object value) {
        assertNotNull(value);
        assertTrue(value instanceof String);
        String stringValue = (String) value;
        return stringValue;
    }

    @Test
    public void testStringPropertyPrefix() throws Exception {
        Object value = et.evaluateExpression(context, "${doc.dc.title}", String.class);
        String stringValue = checkStringValue(value);
        assertEquals("sample title", stringValue);

        assertEquals("sample title", doc.getPropertyValue("dc:title"));
        et.setValue(context, "${doc.dc.title}", "sample title updated");
        assertEquals("sample title updated", doc.getPropertyValue("dc:title"));
    }

    @Test
    public void testStringProperty() throws Exception {
        Object value = et.evaluateExpression(context, "${doc.dublincore.title}", String.class);
        String stringValue = checkStringValue(value);
        assertEquals("sample title", stringValue);

        assertEquals("sample title", doc.getPropertyValue("dublincore:title"));
        et.setValue(context, "${doc.dublincore.title}", "sample title updated");
        assertEquals("sample title updated", doc.getPropertyValue("dublincore:title"));
    }

    @Test
    public void testBeanResolver() throws Exception {
        Object value = et.evaluateExpression(context, "${doc.name}", String.class);
        String stringValue = checkStringValue(value);
        assertEquals("doc", stringValue);
        try {
            et.setValue(context, "${doc.name}", "newname");
            fail("should not be able to set name");
        } catch (PropertyNotWritableException e) {
            // ok
        }
    }

    @Test
    public void testRemovedPropertyCompatResolver() throws Exception {
        Object value = et.evaluateExpression(context, "${doc.tp.removedProperty}", String.class);
        String stringValue = checkStringValue(value);
        assertEquals("sample prop", stringValue);
        value = et.evaluateExpression(context, "${doc.tp.removedPropertyFallback}", String.class);
        stringValue = checkStringValue(value);
        assertEquals("sample prop", stringValue);

        assertEquals("sample prop", doc.getPropertyValue("tp:removedProperty"));
        assertEquals("sample prop", doc.getPropertyValue("tp:removedPropertyFallback"));
        et.setValue(context, "${doc.tp.removedProperty}", "sample prop updated");
        assertEquals("sample prop updated", doc.getPropertyValue("tp:removedProperty"));
        assertEquals("sample prop updated", doc.getPropertyValue("tp:removedPropertyFallback"));
    }

    protected void initDocWithBlob() {
        doc.addFacet("WithFile");
        StringBlob blob = new StringBlob("sample content");
        blob.setFilename("testcontent.txt");
        doc.setPropertyValue("file:content", blob);
    }

    @Test
    public void testBlobFilenameResolver() throws Exception {
        initDocWithBlob();

        Object blobValue = et.evaluateExpression(context, "${doc.file.content}", Blob.class);
        assertTrue(blobValue instanceof Blob);
        assertEquals("testcontent.txt", ((Blob) blobValue).getFilename());

        Object value = et.evaluateExpression(context, "${doc.file.content.filename}", String.class);
        String filename = checkStringValue(value);
        assertEquals("testcontent.txt", filename);

        // check filename resolver, as non-regression test for NXP-24479
        value = et.evaluateExpression(context, "${doc.file.content.name}", String.class);
        filename = checkStringValue(value);
        assertEquals("testcontent.txt", filename);

        assertEquals("testcontent.txt", ((Blob) doc.getPropertyValue("file:content")).getFilename());
        et.setValue(context, "${doc.file.content.filename}", "foo");
        assertEquals("foo", ((Blob) doc.getPropertyValue("file:content")).getFilename());
        et.setValue(context, "${doc.file.content.name}", "bar");
        assertEquals("bar", ((Blob) doc.getPropertyValue("file:content")).getFilename());
    }

    @Test
    public void testBlobMimetypeResolver() throws Exception {
        initDocWithBlob();

        Object value = et.evaluateExpression(context, "${doc.file.content.mimeType}", String.class);
        String mt = checkStringValue(value);
        assertEquals(AbstractBlob.TEXT_PLAIN, mt);

        value = et.evaluateExpression(context, "${doc.file.content['mime-type']}", String.class);
        mt = checkStringValue(value);
        assertEquals(AbstractBlob.TEXT_PLAIN, mt);

        assertEquals(AbstractBlob.TEXT_PLAIN, ((Blob) doc.getPropertyValue("file:content")).getMimeType());
        et.setValue(context, "${doc.file.content.mimeType}", "foo");
        assertEquals("foo", ((Blob) doc.getPropertyValue("file:content")).getMimeType());
        et.setValue(context, "${doc.file.content['mime-type']}", "bar");
        assertEquals("bar", ((Blob) doc.getPropertyValue("file:content")).getMimeType());
    }

    @Test
    public void testBlobEncodingResolver() throws Exception {
        initDocWithBlob();

        Object value = et.evaluateExpression(context, "${doc.file.content.encoding}", String.class);
        String e = checkStringValue(value);
        assertEquals(AbstractBlob.UTF_8, e);

        assertEquals(AbstractBlob.UTF_8, ((Blob) doc.getPropertyValue("file:content")).getEncoding());
        et.setValue(context, "${doc.file.content.encoding}", "foo");
        assertEquals("foo", ((Blob) doc.getPropertyValue("file:content")).getEncoding());
    }

    @Test
    public void testBlobDigestResolver() throws Exception {
        initDocWithBlob();

        Object value = et.evaluateExpression(context, "${doc.file.content.digest}", String.class);
        String e = checkStringValue(value);
        assertEquals("", e);

        assertEquals(null, ((Blob) doc.getPropertyValue("file:content")).getDigest());
        et.setValue(context, "${doc.file.content.digest}", "foo");
        assertEquals("foo", ((Blob) doc.getPropertyValue("file:content")).getDigest());
    }

}
