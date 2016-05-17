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

package org.nuxeo.apidoc.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.apidoc.listener.AttributesExtractorFlagListener.ATTRIBUTES_PROPERTY;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.apidoc.core" })
public class TestExtractor {

    protected List<String> checks = Arrays.asList("usersocialworkspaces", "socialworkspaceactivitystream",
            "publicarticles");

    protected FileBlob getBlob() {
        return new FileBlob(FileUtils.getResourceFileFromContext("sample-fragment-contrib.xml"));
    }

    @Inject
    protected CoreSession session;

    @Inject
    protected WorkManager workManager;

    /**
     * Only useful with debug.
     */
    @Test
    public void testExtractorFlag() {
        DocumentModel myDoc = session.createDocumentModel("/", "mydoc", ExtensionInfo.TYPE_NAME);
        myDoc.setPropertyValue(ATTRIBUTES_PROPERTY, "toto");

        // Should not be triggered
        myDoc = session.createDocument(myDoc);
        session.save();
        Assert.assertNull(myDoc.getPropertyValue(ATTRIBUTES_PROPERTY));

        // Should be triggered
        myDoc.setPropertyValue("file:content", getBlob());
        myDoc = session.saveDocument(myDoc);
        session.save();

        // Should not be triggered
        myDoc.setPropertyValue("file:content", null);
        session.saveDocument(myDoc);
        session.save();
    }

    @Test
    public void testAttributesWorker() throws IOException, SAXException, ParserConfigurationException {
        ExtractXmlAttributesWorker worker = new ExtractXmlAttributesWorker();
        assertNull(worker.extractAttributes(null));

        String attributes = worker.extractAttributes(getBlob());
        assertNotNull(attributes);

        assertTrue(checks.stream().allMatch(attributes::contains));
    }

    @Test
    public void testAttributesExtractedAtCreation() throws InterruptedException {
        DocumentModel myDoc = session.createDocumentModel("/", "mydoc", ExtensionInfo.TYPE_NAME);
        myDoc.setPropertyValue("file:content", getBlob());
        myDoc = session.createDocument(myDoc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        workManager.awaitCompletion(5, TimeUnit.SECONDS);
        myDoc = session.getDocument(myDoc.getRef());

        String attributes = (String) myDoc.getPropertyValue(ATTRIBUTES_PROPERTY);
        assertNotNull(attributes);
        assertTrue(checks.stream().allMatch(attributes::contains));
    }
}
