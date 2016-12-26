/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 */
package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.IOUtils;

public class TestCopyDir extends AbstractCommandTest {

    private final static String deprecatedFilename = "nuxeo-superfeature-5.4.2.jar";

    private final static String snapshotFilename = "nuxeo-otherfeature-5.5-SNAPSHOT.jar";

    private final static String newFilename = "nuxeo-superfeature-5.5.jar";

    private final static String notToDeployFilename = "nuxeo-newfeature-5.5.jar";

    private final static String testConfigFilename = "test-config.xml";

    private File deprecatedFile;

    private File snapshotFile;

    private File newFile;

    private File bundles;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        bundles = new File(Environment.getDefault().getHome(), "bundles");
        deprecatedFile = new File(bundles, deprecatedFilename);
        org.apache.commons.io.FileUtils.touch(deprecatedFile);
        newFile = new File(bundles, newFilename);
        snapshotFile = new File(bundles, snapshotFilename);
        FileUtils.writeStringToFile(snapshotFile, "old SNAPSHOT content");
    }

    @Override
    protected File createPackage() throws IOException, URISyntaxException {
        return getTestPackageZip("test-copy-dir");
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        LocalPackage pkg = task.getPackage();
        File src = pkg.getData().getEntry("bundles/" + newFilename);
        assertTrue(newFilename + " was not installed", newFile.isFile());
        assertEquals(IOUtils.createMd5(src), IOUtils.createMd5(newFile));
        assertFalse(deprecatedFilename + " was not replaced", deprecatedFile.exists());
        BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
        try {
            String line = reader.readLine();
            assertEquals("new SNAPSHOT content", line);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(reader);
        }
        assertFalse("New feature was copied whereas 'upgradeOnly=true'",
                new File(bundles, notToDeployFilename).exists());

        File templates = new File(Environment.getDefault().getHome(), "templates");
        File configFile = new File(templates, "collaboration/config/" + testConfigFilename);
        assertTrue(configFile.exists());
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertFalse(newFilename + " was not removed", newFile.exists());
        assertTrue(deprecatedFilename + " was not copy back", deprecatedFile.exists());
        BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
        try {
            String line = reader.readLine();
            assertEquals("old SNAPSHOT content", line);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(reader);
        }

        File templates = new File(Environment.getDefault().getHome(), "templates");
        File configFile = new File(templates, "collaboration/config/" + testConfigFilename);
        assertFalse(configFile.exists());
    }

}
