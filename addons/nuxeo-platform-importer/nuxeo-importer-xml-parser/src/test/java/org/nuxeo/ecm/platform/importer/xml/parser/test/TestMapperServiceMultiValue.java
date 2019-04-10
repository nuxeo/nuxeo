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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.importer.xml.parser.test;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Verify Service mapping with multi-value metadata
 *
 * @author <a href="mailto:mvachette@nuxeo.com">Mika</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-importer-xml-parser")
@Deploy("nuxeo-importer-xml-parser:test-ImporterMapping-MultiValue-contrib.xml")
public class TestMapperServiceMultiValue {

    @Inject
    CoreSession session;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("multivalue.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, xml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Document where dc:title='MultiValue'");
        Assert.assertEquals("we should have only one File", 1, docs.size());
        DocumentModel fileDoc = docs.get(0);
        Property property = fileDoc.getProperty("dc:subjects");
        Object[] subjects = (Object[]) property.getValue();
        Assert.assertEquals("The property dc:subjects should contain 2 values", 2, subjects.length);
    }

}
