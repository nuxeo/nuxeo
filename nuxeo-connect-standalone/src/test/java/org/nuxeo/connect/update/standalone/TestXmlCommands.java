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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestXmlCommands extends PackageTestCase {

    protected File createPackage() throws Exception {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
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
        File file = File.createTempFile("nxinstall-file-", ".tmp");
        file.deleteOnExit();
        File tofile = File.createTempFile("nxinstall-tofile-", ".tmp");
        tofile.deleteOnExit();
        builder.addInstallScript("<install>\n  <copy file=\""
                + file.getAbsolutePath() + "\" tofile=\""
                + tofile.getAbsolutePath()
                + "\" overwrite=\"true\"/>\n</install>\n");
        // System.out.println(builder.buildManifest());
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
        // check that uninstall file was generated.
        assertTrue(pkg.getUninstallFile().isFile());
        // System.out.println(FileUtils.readFile(pkg.getUninstallFile()));
    }

}
