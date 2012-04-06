/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestCopy extends AbstractCommandTest {

    private File goldStandardFile;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        goldStandardFile = new File(Environment.getDefault().getConfig(),
                "goldstandard.properties");
        FileUtils.writeFile(goldStandardFile, "param1=value1");
    }

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        File props = File.createTempFile("test-commands-", ".properties");
        props.deleteOnExit();
        FileUtils.writeFile(props, "test=my value");
        builder.addEntry("test.properties", new FileInputStream(props));
        props = File.createTempFile("test-commands-", ".properties");
        props.deleteOnExit();
        FileUtils.writeFile(props, "param2=value2");
        builder.addEntry("append.properties", new FileInputStream(props));
        props = File.createTempFile("test-commands-", ".properties");
        props.deleteOnExit();
        FileUtils.writeFile(props, "param3=value3");
        builder.addEntry("append2.properties", new FileInputStream(props));
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("copy");
        writer.attr("file", "${package.root}/test.properties");
        writer.attr("todir", "${env.config}");
        writer.end();
        writer.start("copy");
        writer.attr("file", "${package.root}/append.properties");
        writer.attr("tofile", "${env.config}/goldstandard.properties");
        writer.attr("append", "true");
        writer.end();
        writer.start("append");
        writer.attr("file", "${package.root}/append2.properties");
        writer.attr("tofile", "${env.config}/goldstandard.properties");
        writer.end();
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        LocalPackage pkg = task.getPackage();
        File src = pkg.getData().getEntry("test.properties");
        File dst = getTargetFile();
        assertTrue(dst.isFile());
        assertEquals(IOUtils.createMd5(src), IOUtils.createMd5(dst));
        Properties goldstandard = new Properties();
        goldstandard.load(new FileInputStream(goldStandardFile));
        assertEquals("Original property is missing", "value1",
                goldstandard.getProperty("param1"));
        assertEquals("Appended property is missing", "value2",
                goldstandard.getProperty("param2"));
        assertEquals("Appended property is missing", "value3",
                goldstandard.getProperty("param3"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertFalse(getTargetFile().exists());
        Properties goldstandard = new Properties();
        goldstandard.load(new FileInputStream(goldStandardFile));
        assertEquals("Original property is missing", "value1",
                goldstandard.getProperty("param1"));
        assertNull("Appended property must be removed",
                goldstandard.getProperty("param2"));
        assertNull("Appended property must be removed",
                goldstandard.getProperty("param3"));
    }

    protected File getTargetFile() {
        return new File(Environment.getDefault().getConfig(), "test.properties");
    }

}
