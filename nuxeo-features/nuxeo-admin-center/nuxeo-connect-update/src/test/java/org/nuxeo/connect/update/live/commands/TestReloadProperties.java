/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.live.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.connect.update.ConnectUpdateFeature;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.standalone.commands.AbstractCommandTest;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.live.commands.ReloadProperties;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(ConnectUpdateFeature.class)
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
        builder.addEntry("test.properties", new ByteArrayInputStream(content.getBytes()));
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
