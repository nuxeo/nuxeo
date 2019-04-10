/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveSetVersioningOptions;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * Tests the {@link NuxeoDriveSetVersioningOptions} operation.
 *
 * @author Antoine Taillefer
 * @deprecated since 9.1, see {@link NuxeoDriveSetVersioningOptions}
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@ServletContainer(port = 18080)
public class TestSetVersioningOptions {

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected Session clientSession;

    protected VersioningFileSystemItemFactory defaultFileSystemItemFactory;

    @Before
    public void init() throws Exception {

        defaultFileSystemItemFactory = (VersioningFileSystemItemFactory) ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "defaultFileSystemItemFactory");
        assertNotNull(defaultFileSystemItemFactory);
    }

    @Test
    public void testSetVersioningOptions() throws Exception {

        // Default values
        assertEquals(3600.0, defaultFileSystemItemFactory.getVersioningDelay(), .01);
        assertEquals(VersioningOption.MINOR, defaultFileSystemItemFactory.getVersioningOption());

        // Set delay to 2 seconds
        clientSession.newRequest(NuxeoDriveSetVersioningOptions.ID).set("delay", "2").execute();
        assertEquals(2.0, defaultFileSystemItemFactory.getVersioningDelay(), .01);
        assertEquals(VersioningOption.MINOR, defaultFileSystemItemFactory.getVersioningOption());

        // Set option to MAJOR
        clientSession.newRequest(NuxeoDriveSetVersioningOptions.ID).set("option", "MAJOR").execute();
        assertEquals(2.0, defaultFileSystemItemFactory.getVersioningDelay(), .01);
        assertEquals(VersioningOption.MAJOR, defaultFileSystemItemFactory.getVersioningOption());

    }

}
