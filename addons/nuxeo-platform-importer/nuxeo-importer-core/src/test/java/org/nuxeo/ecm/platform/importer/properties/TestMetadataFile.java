/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.api" })
public class TestMetadataFile {

    @Inject
    protected CoreSession session;

    protected Calendar calendar = GregorianCalendar.getInstance();

    @Test
    public void generateSimpleMetadataFile() throws Exception {
        DocumentModel testFile = createTestFile();

        MetadataFile mdFile = MetadataFile.createFromDocument(testFile);

        File file = Framework.createTempFile("mdf", null);
        mdFile.writeTo(file);

        MetadataCollector collector = new MetadataCollector();
        collector.addPropertyFile(file);

        String contextPath = file.getParent();
        Map<String, Serializable> properties = collector.getProperties(contextPath);
        assertEquals(7, properties.size());
        assertEquals("testTitle", properties.get("dc:title"));
        assertEquals("testDescription", properties.get("dc:description"));
        assertEquals("testCoverage", properties.get("dc:coverage"));
        Date date = formatDate((String) properties.get("dc:expired")).getTime();
        DateFormat dateFormat = new SimpleDateFormat(MetadataCollector.DATE_FORMAT);
        assertEquals(dateFormat.format(calendar.getTime()), dateFormat.format(date));
        assertEquals("testIcon", properties.get("common:icon"));
        assertEquals("0", properties.get("uid:major_version"));
        assertEquals("0", properties.get("uid:minor_version"));
    }

    @Test
    public void generateOneSchemaMetadataFile() throws Exception {
        DocumentModel testFile = createTestFile();

        MetadataFile mdFile = MetadataFile.createFromSchemas(testFile, Arrays.asList(new String[] { "dublincore" }));

        File file = Framework.createTempFile("mdf", null);
        mdFile.writeTo(file);

        MetadataCollector collector = new MetadataCollector();
        collector.addPropertyFile(file);

        String contextPath = file.getParent();
        Map<String, Serializable> properties = collector.getProperties(contextPath);
        assertEquals(4, properties.size());
        assertEquals("testTitle", properties.get("dc:title"));
        assertEquals("testDescription", properties.get("dc:description"));
        assertEquals("testCoverage", properties.get("dc:coverage"));
        Date date = formatDate((String) properties.get("dc:expired")).getTime();
        DateFormat dateFormat = new SimpleDateFormat(MetadataCollector.DATE_FORMAT);
        assertEquals(dateFormat.format(calendar.getTime()), dateFormat.format(date));
    }

    @Test
    public void generateOneSchemaWithPropertiesMetadataFile() throws Exception {
        DocumentModel testFile = createTestFile();

        MetadataFile mdFile = MetadataFile.createFromSchemasAndProperties(testFile,
                Arrays.asList(new String[] { "common" }), Arrays.asList(new String[] { "dc:title" }));

        File file = Framework.createTempFile("mdf", null);
        mdFile.writeTo(file);

        MetadataCollector collector = new MetadataCollector();
        collector.addPropertyFile(file);

        String contextPath = file.getParent();
        Map<String, Serializable> properties = collector.getProperties(contextPath);
        assertEquals(2, properties.size());
        assertEquals("testTitle", properties.get("dc:title"));
        assertEquals("testIcon", properties.get("common:icon"));
    }

    @Test
    public void generatePropertiesMetadataFile() throws Exception {
        DocumentModel testFile = createTestFile();

        MetadataFile mdFile = MetadataFile.createFromProperties(testFile,
                Arrays.asList(new String[] { "dc:title", "common:icon", "dc:description" }));

        File file = Framework.createTempFile("mdf", null);
        mdFile.writeTo(file);

        MetadataCollector collector = new MetadataCollector();
        collector.addPropertyFile(file);

        String contextPath = file.getParent();
        Map<String, Serializable> properties = collector.getProperties(contextPath);
        assertEquals(3, properties.size());
        assertEquals("testTitle", properties.get("dc:title"));
        assertEquals("testIcon", properties.get("common:icon"));
        assertEquals("testDescription", properties.get("dc:description"));
    }

    protected DocumentModel createTestFile() {
        DocumentModel file = session.createDocumentModel("/", "testfile", "File");
        file.setPropertyValue("dc:title", "testTitle");
        file.setPropertyValue("dc:description", "testDescription");
        file.setPropertyValue("dc:coverage", "testCoverage");
        file.setPropertyValue("dc:expired", calendar);
        file.setPropertyValue("common:icon", "testIcon");
        file = session.createDocument(file);
        assertNotNull(file);
        file = session.saveDocument(file);
        session.save();

        return file;
    }

    protected Calendar formatDate(String value) {
        return (Calendar) new DateType().decode(value);
    }

}
