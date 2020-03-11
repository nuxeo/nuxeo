/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.connect.update.standalone.commands;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.task.Task;

public class TestUpdate extends AbstractCommandTest {

    private File oldJar;

    private File expectedJar;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File bundles = new File(Environment.getDefault().getHome(), "bundles");
        oldJar = new File(bundles, "foo-1.0.jar");
        FileUtils.writeStringToFile(oldJar, "old JAR content", UTF_8);
        expectedJar = new File(bundles, "foo-1.1.jar"); // installed by install.xml
    }

    @Override
    protected File createPackage() throws IOException, URISyntaxException {
        return getTestPackageZip("test-update");
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        assertFalse(oldJar.exists());
        assertTrue(expectedJar.exists());
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertTrue(oldJar.exists());
        assertFalse(expectedJar.exists());
    }

}
