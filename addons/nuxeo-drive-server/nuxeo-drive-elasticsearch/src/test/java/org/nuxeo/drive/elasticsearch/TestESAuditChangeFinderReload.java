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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.test.ESAuditFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(ESAuditFeature.class)
@Deploy("org.nuxeo.drive.core")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-sync-root-cache-contrib.xml")
public class TestESAuditChangeFinderReload {

    @Inject
    protected NuxeoDriveManager driveManager;

    @Inject
    private HotDeployer deployer;

    @Test
    public void testReloadDoesntBreakFinder() throws Exception {
        FileSystemChangeFinder finder = driveManager.getChangeFinder();
        assertTrue(finder instanceof ESAuditChangeFinder);

        // run a request
        long bound = finder.getUpperBound();
        assertNotEquals(0, bound);

        // reload Nuxeo context
        deployer.reload();

        // run a request again
        bound = finder.getUpperBound();
        assertNotEquals(0, bound);
    }

}
