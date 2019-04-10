/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.core.listener;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.dam.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy( { "org.nuxeo.ecm.core.api",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.audio.core", "org.nuxeo.dam.core" })
public class TestInitPropertiesListener {

    @Inject
    protected CoreSession session;

    @Test
    public void testListener() throws Exception {
        // Import set document
        DocumentModel importSet = session.createDocumentModel("/",
                "importSetTest", Constants.IMPORT_SET_TYPE);
        importSet.setPropertyValue(Constants.DAM_COMMON_AUTHOR_PROPERTY,
                "testCreator");
        Calendar cal = GregorianCalendar.getInstance();
        importSet.setPropertyValue(
                Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY, cal);
        importSet.setPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY,
                "testDescription");
        importSet.setPropertyValue(Constants.DUBLINCORE_COVERAGE_PROPERTY,
                "testCoverage");
        importSet.setPropertyValue(Constants.DUBLINCORE_EXPIRED_PROPERTY, cal);
        importSet = session.createDocument(importSet);
        assertNotNull(importSet);
        session.saveDocument(importSet);
        session.save();

        // File document
        DocumentModel file = session.createDocumentModel(
                importSet.getPathAsString(), "fileTest", "File");
        file = session.createDocument(file);
        assertNotNull(file);

        session.saveDocument(file);
        session.save();
        assertTrue(file.hasSchema("dam_common"));
        assertTrue(file.hasSchema("dublincore"));
        assertEquals("testCreator",
                file.getPropertyValue(Constants.DAM_COMMON_AUTHOR_PROPERTY));
        assertEquals(
                cal,
                file.getPropertyValue(Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY));
        assertEquals(
                "testDescription",
                file.getPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY));
        assertEquals("testCoverage",
                file.getPropertyValue(Constants.DUBLINCORE_COVERAGE_PROPERTY));
        assertEquals(cal,
                file.getPropertyValue(Constants.DUBLINCORE_EXPIRED_PROPERTY));

        // Picture document
        DocumentModel picture = session.createDocumentModel(
                importSet.getPathAsString(), "pictureTest", "Picture");
        picture = session.createDocument(picture);
        assertNotNull(picture);

        session.saveDocument(picture);
        session.save();
        assertTrue(picture.hasSchema("dam_common"));
        assertTrue(picture.hasSchema("dublincore"));
        assertEquals("testCreator",
                picture.getPropertyValue(Constants.DAM_COMMON_AUTHOR_PROPERTY));
        assertEquals(
                cal,
                picture.getPropertyValue(Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY));
        assertEquals(
                "testDescription",
                picture.getPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY));
        assertEquals(
                "testCoverage",
                picture.getPropertyValue(Constants.DUBLINCORE_COVERAGE_PROPERTY));
        assertEquals(cal,
                picture.getPropertyValue(Constants.DUBLINCORE_EXPIRED_PROPERTY));
    }

}
