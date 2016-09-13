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
import org.nuxeo.connect.update.task.standalone.CommandsTask;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestXmlCommands extends PackageTestCase {

    @Test
    public void testReadCommands() throws Exception {
        File zip = getTestPackageZip("test-xml-command");
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
        assertEquals(1, task.getCommands().size());
    }

}
