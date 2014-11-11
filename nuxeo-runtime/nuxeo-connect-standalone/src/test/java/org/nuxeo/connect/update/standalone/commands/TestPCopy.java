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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestPCopy extends AbstractCommandTest {

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        String content = "test=my ${v}";
        builder.addEntry("test.properties",
                new ByteArrayInputStream(content.getBytes()));
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("pcopy");
        writer.attr("file", "${package.root}/test.properties");
        writer.attr("tofile", "${env.config}");
        writer.end();
    }

    @Override
    protected Map<String, String> getUserProperties() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("v", "value");
        return map;
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        File dst = getTargetFile();
        assertTrue(dst.isFile());
        assertEquals("test=my value", FileUtils.readFile(dst));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertFalse(getTargetFile().isFile());
    }

    protected File getTargetFile() {
        return new File(Environment.getDefault().getConfig(), "test.properties");
    }

}
