/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
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
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Tests specific command guard based on the platform version.
 *
 * @since 11.1
 */
public class TestCopyPlatformGuard extends AbstractCommandTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File distribFile = new File(new File(Environment.getDefault().getHome(), ConfigurationGenerator.TEMPLATES),
                "common/config/distribution.properties");
        FileUtils.writeStringToFile(distribFile, "org.nuxeo.distribution.version=11.1", UTF_8);
    }

    @Override
    protected File createPackage() throws IOException, URISyntaxException {
        return getTestPackageZip("test-copy-platform-guard");
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        File dst = getTargetFile("111");
        assertTrue(dst.exists());

        dst = getTargetFile("810");
        assertFalse(dst.exists());
    }

    protected File getTargetFile(String suffix) {
        String filename = String.format("test%s.properties", suffix);
        return new File(Environment.getDefault().getConfig(), filename);
    }

}
