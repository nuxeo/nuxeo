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

import java.io.ByteArrayInputStream;

import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.impl.task.commands.ReloadProperties;
import org.nuxeo.connect.update.impl.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestReloadProperties extends AbstractCommandTest {

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        String content = "myprop=myvalue\n";
        builder.addEntry("test.properties",
                new ByteArrayInputStream(content.getBytes()));
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("copy");
        writer.attr("file", "${package.root}/test.properties");
        writer.attr("todir", "${env.config}");
        writer.attr("overwrite", "false");
        writer.end();
        writer.start(ReloadProperties.ID);
        writer.end();
    }

    @Override
    public boolean install(LocalPackage pkg) throws Exception {
        assertNull(Framework.getProperty("myprop"));
        return super.install(pkg);
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        assertEquals("myvalue", Framework.getProperty("myprop"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        assertNull(Framework.getProperty("myprop"));
    }

}
