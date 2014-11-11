/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */
package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

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
        FileUtils.writeFile(snapshotFile, "old SNAPSHOT content");
    }

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        File jarFile = File.createTempFile("test-commands-", ".jar");
        jarFile.deleteOnExit();
        FileUtils.writeFile(jarFile, "anything");
        builder.addEntry("bundles/" + newFilename, new FileInputStream(jarFile));
        FileUtils.writeFile(jarFile, "new SNAPSHOT content");
        builder.addEntry("bundles/" + snapshotFilename, new FileInputStream(
                jarFile));
        builder.addEntry("bundles/" + notToDeployFilename, new FileInputStream(
                jarFile));
        File xmlFile = File.createTempFile("test-config", ".xml");
        xmlFile.deleteOnExit();
        FileUtils.writeFile(xmlFile, "anything");
        builder.addEntry(
                "templates/collaboration/config/" + testConfigFilename,
                new FileInputStream(xmlFile));
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("copy");
        writer.attr("dir", "${package.root}/bundles");
        writer.attr("todir", "${env.bundles}");
        writer.attr("overwriteIfNewerVersion", "true");
        writer.attr("upgradeOnly", "true");
        writer.end();

        writer.start("copy");
        writer.attr("dir", "${package.root}/templates");
        writer.attr("todir", "${env.templates}");
        writer.attr("overwrite", "true");
        writer.end();
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        LocalPackage pkg = task.getPackage();
        File src = pkg.getData().getEntry("bundles/" + newFilename);
        assertTrue(newFilename + " was not installed", newFile.isFile());
        assertEquals(IOUtils.createMd5(src), IOUtils.createMd5(newFile));
        assertFalse(deprecatedFilename + " was not replaced",
                deprecatedFile.exists());
        BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
        String line = reader.readLine();
        assertEquals("new SNAPSHOT content", line);
        assertFalse("New feature was copied whereas 'upgradeOnly=true'",
                new File(bundles, notToDeployFilename).exists());

        File templates = new File(Environment.getDefault().getHome(),
                "templates");
        File configFile = new File(templates, "collaboration/config/"
                + testConfigFilename);
        assertTrue(configFile.exists());
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertFalse(newFilename + " was not removed", newFile.exists());
        assertTrue(deprecatedFilename + " was not copy back",
                deprecatedFile.exists());
        BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
        String line = reader.readLine();
        assertEquals("old SNAPSHOT content", line);

        File templates = new File(Environment.getDefault().getHome(),
                "templates");
        File configFile = new File(templates, "collaboration/config/"
                + testConfigFilename);
        assertFalse(configFile.exists());
    }

}
