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

import org.junit.runner.RunWith;
import org.nuxeo.dam.core.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.junit.Test;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy({
        "org.nuxeo.ecm.core.api",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.audio.core",
        "org.nuxeo.dam.core"
})
public class TestInitPropertiesListener {

    @Inject
    protected CoreSession session;

    @Test
    public void testListener() throws Exception {
        // Import set document
        DocumentModel importSet = session.createDocumentModel("/",
                "importSetTest", Constants.IMPORT_SET_TYPE);
        importSet.setPropertyValue("damc:author", "testCreator");
        Calendar cal = GregorianCalendar.getInstance();
        importSet.setPropertyValue("damc:authoringDate", cal);
        importSet.setPropertyValue("dc:description", "testDescription");
        importSet.setPropertyValue("dc:coverage", "testCoverage");
        importSet.setPropertyValue("dc:expired", cal);
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
        assertEquals("testCreator", file.getPropertyValue("damc:author"));
        assertEquals(cal, file.getPropertyValue("damc:authoringDate"));
        assertEquals("testDescription", file.getPropertyValue("dc:description"));
        assertEquals("testCoverage", file.getPropertyValue("dc:coverage"));
        assertEquals(cal, file.getPropertyValue("dc:expired"));

        // Picture document
        DocumentModel picture = session.createDocumentModel(
                importSet.getPathAsString(), "pictureTest", "Picture");
        picture = session.createDocument(picture);
        assertNotNull(picture);
        session.saveDocument(picture);
        session.save();
        assertTrue(picture.hasSchema("dam_common"));
        assertTrue(picture.hasSchema("dublincore"));
        assertEquals("testCreator", picture.getPropertyValue("damc:author"));
        assertEquals(cal, picture.getPropertyValue("damc:authoringDate"));
        assertEquals("testDescription", picture.getPropertyValue("dc:description"));
        assertEquals("testCoverage", picture.getPropertyValue("dc:coverage"));
        assertEquals(cal, picture.getPropertyValue("dc:expired"));
    }

}
