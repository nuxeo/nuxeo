/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 *
 */

package org.nuxeo.ecm.core.version.test;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Constants;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

public class TestVersioningSaveOptions extends SQLRepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testTypeSaveOptions() throws Exception {
        VersioningService vService = Framework.getLocalService(VersioningService.class);
        assertNotNull(vService);
        DocumentModel fileDoc = new DocumentModelImpl("File");
        fileDoc = session.createDocument(fileDoc);
        String versionLabel = fileDoc.getVersionLabel();
        assertEquals("0.0", versionLabel);
        List<VersioningOption> opts = vService.getSaveOptions(fileDoc);
        assertEquals(3, opts.size());
        assertEquals(VersioningOption.NONE, opts.get(0));

        deployContrib(Constants.CORE_TEST_TESTS_BUNDLE,
                "test-versioning-contrib.xml");
        fileDoc = new DocumentModelImpl("File");
        fileDoc = session.createDocument(fileDoc);
        versionLabel = fileDoc.getVersionLabel();
        assertEquals("1.1+", versionLabel);
        opts = vService.getSaveOptions(fileDoc);
        assertEquals(2, opts.size());
        assertEquals(VersioningOption.MINOR, opts.get(0));
        session.followTransition(fileDoc.getRef(), "approve");
        opts = vService.getSaveOptions(fileDoc);
        assertEquals(0, opts.size());
        session.followTransition(fileDoc.getRef(), "backToProject");
        session.followTransition(fileDoc.getRef(), "obsolete");
        opts = vService.getSaveOptions(fileDoc);
        assertEquals(3, opts.size());

        deployContrib(Constants.CORE_TEST_TESTS_BUNDLE,
                "test-versioning-override-contrib.xml");
        fileDoc = new DocumentModelImpl("File");
        fileDoc = session.createDocument(fileDoc);
        versionLabel = fileDoc.getVersionLabel();
        assertEquals("2.2+", versionLabel);
    }

}
