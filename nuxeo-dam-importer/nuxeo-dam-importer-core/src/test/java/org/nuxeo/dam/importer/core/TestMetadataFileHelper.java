/*
 * (C) Copyright 2009-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.importer.core;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.dam.Constants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;
import org.nuxeo.ecm.platform.importer.properties.MetadataFile;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy({ "org.nuxeo.ecm.core.api", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.audio.core", "org.nuxeo.dam.core" })
public class TestMetadataFileHelper {

    @Inject
    protected CoreSession session;

    protected Calendar calendar = GregorianCalendar.getInstance();

    @Test
    public void generateMetadataFile() throws Exception {
        DocumentModel testFile = createTestFile();

        MetadataFile mdFile = MetadataFileHelper.createFrom(testFile);

        File file = File.createTempFile("mdf", null);
        mdFile.writeTo(file);

        MetadataCollector collector = new MetadataCollector();
        collector.addPropertyFile(file);

        String contextPath = file.getAbsoluteFile().getParent();
        Map<String, Serializable> properties = collector.getProperties(contextPath);
        assertEquals(5, properties.size());
        assertEquals("testAuthor",
                properties.get(Constants.DAM_COMMON_AUTHOR_PROPERTY));
        Date date = ((Calendar) new DateType().decode((String) properties.get(Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY))).getTime();
        DateFormat dateFormat = new SimpleDateFormat(
                MetadataCollector.DATE_FORMAT);
        assertEquals(dateFormat.format(calendar.getTime()),
                dateFormat.format(date));

        assertEquals(dateFormat.format(calendar.getTime()),
                dateFormat.format(date));

        assertEquals("testDescription",
                properties.get(Constants.DUBLINCORE_DESCRIPTION_PROPERTY));
        assertEquals("testDescription",
                properties.get(Constants.DUBLINCORE_DESCRIPTION_PROPERTY));
        assertEquals("testCoverage",
                properties.get(Constants.DUBLINCORE_COVERAGE_PROPERTY));
        date = ((Calendar) new DateType().decode((String) properties.get(Constants.DUBLINCORE_EXPIRED_PROPERTY))).getTime();
        assertEquals(dateFormat.format(calendar.getTime()),
                dateFormat.format(date));

        assertFalse(properties.containsKey(Constants.DUBLINCORE_TITLE_PROPERTY));
        assertFalse(properties.containsKey("common:icon"));
    }

    protected DocumentModel createTestFile() throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "testFile",
                "File");
        file.setPropertyValue(Constants.DAM_COMMON_AUTHOR_PROPERTY,
                "testAuthor");
        file.setPropertyValue(Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY,
                calendar);
        file.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "testTitle");
        file.setPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY,
                "testDescription");
        file.setPropertyValue(Constants.DUBLINCORE_COVERAGE_PROPERTY,
                "testCoverage");
        file.setPropertyValue(Constants.DUBLINCORE_EXPIRED_PROPERTY, calendar);
        file.setPropertyValue("common:icon", "testIcon");
        file = session.createDocument(file);
        assertNotNull(file);
        file = session.saveDocument(file);
        session.save();

        return file;
    }

}
