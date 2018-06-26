/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.apidoc.listener.AttributesExtractorStater.ATTRIBUTES_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.apidoc.listener.AttributesExtractorStater;
import org.nuxeo.apidoc.worker.ExtractXmlAttributesWorker;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestExtractor {

    protected List<String> checks = Arrays.asList("usersocialworkspaces", "socialworkspaceactivitystream",
            "publicarticles", "gadget", "org.nuxeo.opensocial.gadgets.service");

    protected FileBlob getBlob() {
        return new FileBlob(
                org.nuxeo.common.utils.FileUtils.getResourceFileFromContext("apidoc-sample-fragment-contrib.xml"));
    }

    @Inject
    protected CoreSession session;

    @Inject
    protected WorkManager workManager;

    @Inject TransactionalFeature txFeature;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Test
    public void testParser() throws Exception {
        File xmlFile = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext(
                "apidoc-sample-fragment-contrib.xml");
        String xml = FileUtils.readFileToString(xmlFile, StandardCharsets.UTF_8);
        String html = XMLContributionParser.prettyfy(xml);
    }

    @Test
    public void testExtractorFlag() {
        DocumentModel myDoc = session.createDocumentModel("/", "mydoc", ExtensionInfo.TYPE_NAME);
        myDoc.setPropertyValue(ATTRIBUTES_PROPERTY, "toto");

        long completed = workManager.getMetrics(ExtractXmlAttributesWorker.CATEGORY).completed.longValue();
        // Should not be triggered
        myDoc = session.createDocument(myDoc);
        Assert.assertNull(myDoc.getPropertyValue(ATTRIBUTES_PROPERTY));
        txFeature.nextTransaction();
        Assert.assertEquals(completed, workManager.getMetrics(ExtractXmlAttributesWorker.CATEGORY).completed);

        // Should be triggered
        myDoc.setPropertyValue("file:content", getBlob());
        myDoc = session.saveDocument(myDoc);
        txFeature.nextTransaction();
        Assert.assertEquals(++completed, workManager.getMetrics(ExtractXmlAttributesWorker.CATEGORY).completed);

        // Should not be triggered
        myDoc.setPropertyValue("file:content", null);
        session.saveDocument(myDoc);
        txFeature.nextTransaction();
        Assert.assertEquals(completed, workManager.getMetrics(ExtractXmlAttributesWorker.CATEGORY).completed);
    }

    @Test
    public void testAttributesWorker() throws IOException, SAXException, ParserConfigurationException {
        ExtractXmlAttributesWorker worker = new ExtractXmlAttributesWorker("test", "test", "test");
        assertNull(worker.extractAttributes(null));

        String attributes = worker.extractAttributes(getBlob());
        assertNotNull(attributes);

        assertTrue(checks.stream().allMatch(attributes::contains));
    }

    @Test
    public void testAttributesExtractedAtCreation() {
        DocumentModel myDoc = session.createDocumentModel("/", "mydoc", ExtensionInfo.TYPE_NAME);
        myDoc.setPropertyValue("file:content", getBlob());
        myDoc = session.createDocument(myDoc);
        txFeature.nextTransaction();

        myDoc = session.getDocument(myDoc.getRef());
        String attributes = (String) myDoc.getPropertyValue(ATTRIBUTES_PROPERTY);
        assertNotNull(attributes);
        assertTrue(checks.stream().allMatch(attributes::contains));
    }

    @Test
    public void testOnExistingDocument() {
        // Disable listener to simulate exisiting document before new attributes field
        eventServiceAdmin.setListenerEnabledFlag(AttributesExtractorStater.class.getSimpleName(), false);

        DocumentModel myDoc = session.createDocumentModel("/", "mydoc", ExtensionInfo.TYPE_NAME);
        myDoc.setPropertyValue("file:content", getBlob());
        myDoc = session.createDocument(myDoc);
        txFeature.nextTransaction();

        myDoc = session.getDocument(myDoc.getRef());
        // Expect that nothing is present in the attribute field
        String attributes = (String) myDoc.getPropertyValue(ATTRIBUTES_PROPERTY);
        assertNull(attributes);

        // Enable it after document is created
        eventServiceAdmin.setListenerEnabledFlag(AttributesExtractorStater.class.getSimpleName(), true);

        // Change anything other than the blob
        myDoc.setPropertyValue("dc:title", "modification");
        myDoc = session.saveDocument(myDoc);
        txFeature.nextTransaction();

        myDoc = session.getDocument(myDoc.getRef());
        attributes = (String) myDoc.getPropertyValue(ATTRIBUTES_PROPERTY);
        assertNotNull(attributes);
        assertTrue(checks.stream().allMatch(attributes::contains));
    }
}
