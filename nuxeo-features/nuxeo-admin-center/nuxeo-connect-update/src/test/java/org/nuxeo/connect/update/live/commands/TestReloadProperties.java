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
package org.nuxeo.connect.update.live.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.runner.RunWith;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.live.commands.ReloadProperties;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.connect.update.standalone.commands.AbstractCommandTest;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.connect.client", "org.nuxeo.connect.client.wrapper",
        "org.nuxeo.connect.update", "org.nuxeo.runtime.reload" })
public class TestReloadProperties extends AbstractCommandTest {

    @Inject
    PackageUpdateService injectedService;

    @Override
    protected void setupService() throws IOException, PackageException {
        service = injectedService;
    }

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
        super.installDone(task, error);
        assertEquals("myvalue", Framework.getProperty("myprop"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertNull(Framework.getProperty("myprop"));
    }

}
