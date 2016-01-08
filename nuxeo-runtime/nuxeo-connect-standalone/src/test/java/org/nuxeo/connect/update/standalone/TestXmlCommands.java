/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.task.standalone.CommandsTask;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestXmlCommands extends PackageTestCase {

    protected File createPackage() throws Exception {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(PackageType.ADDON);
        builder.platform("dm-5.3.2");
        builder.platform("dam-5.3.2");
        builder.dependency("nuxeo-core:5.3.1:5.3.2");
        builder.dependency("nuxeo-runtime:5.3.1");
        builder.title("Nuxeo Automation");
        builder.description("A service that enables building complex business logic on top of Nuxeo services using scriptable operation chains");
        builder.classifier("Open Source");
        builder.vendor("Nuxeo");
        builder.installer(InstallTask.class.getName(), true);
        builder.uninstaller(UninstallTask.class.getName(), true);
        builder.addLicense("My test license. All rights reserved.");
        File file = Framework.createTempFile("nxinstall-file-", ".tmp");
        Framework.trackFile(file, this);
        File tofile = Framework.createTempFile("nxinstall-tofile-", ".tmp");
        Framework.trackFile(tofile, this);
        builder.addInstallScript("<install>\n  <copy file=\"" + file.getAbsolutePath() + "\" tofile=\""
                + tofile.getAbsolutePath() + "\" overwrite=\"true\"/>\n</install>\n");
        return builder.build();
    }

    @Test
    public void testReadCommands() throws Exception {
        File zip = createPackage();
        // register the package
        service.addPackage(zip);
        List<LocalPackage> pkgs = service.getPackages();
        assertEquals(1, pkgs.size());

        LocalPackage pkg = pkgs.get(0);
        assertEquals("nuxeo-automation-5.3.2", pkg.getId());
        assertEquals("nuxeo-automation", pkg.getName());
        assertEquals("5.3.2", pkg.getVersion().toString());

        CommandsTask task = (CommandsTask) pkg.getInstallTask();
        assertTrue(task.isRestartRequired());

        task.validate();
        task.run(null);
        assertEquals(1, task.getCommands().size());
        assertEquals(1, task.getCommandLog().size());
        assertTrue("uninstall file was not generated", pkg.getUninstallFile().isFile());
    }

}
