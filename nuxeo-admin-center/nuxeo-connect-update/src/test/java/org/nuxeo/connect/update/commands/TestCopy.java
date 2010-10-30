/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.commands;

import java.io.File;
import java.io.FileInputStream;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.impl.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.util.PackageBuilder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestCopy extends AbstractCommandTest {

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        File props = File.createTempFile("test-commands-", ".properties");
        props.deleteOnExit();
        FileUtils.writeFile(props, "test=my value");
        FileInputStream in = new FileInputStream(props);
        builder.addEntry("test.properties", in);
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("copy");
        writer.attr("file", "${package.root}/test.properties");
        writer.attr("todir", "${env.config}");
        writer.attr("overwrite", "false");
        writer.end();
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        if (error != null) {
            error.printStackTrace();
            fail("Unexpected Rollback on Install Task");
        }
        LocalPackage pkg = task.getPackage();
        File src = pkg.getData().getEntry("test.properties");
        File dst = getTargetFile();
        assertTrue(dst.isFile());
        assertEquals(IOUtils.createMd5(src), IOUtils.createMd5(dst));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        if (error != null) {
            error.printStackTrace();
            fail("Unexpected Rollback on uninstall Task");
        }
        assertFalse(getTargetFile().isFile());
    }

    protected File getTargetFile() {
        return new File(Environment.getDefault().getConfig(), "test.properties");
    }

}
