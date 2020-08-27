/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Feature pushing contributions to the Runtime config directory before it starts.
 * <p>
 * Allows testing config initialization and registration of incorrect configurations.
 *
 * @since 11.3
 */
public class RuntimeInitializationConfigErrorFeature implements RunnerFeature, WorkingDirectoryConfigurator {

    protected Set<String> TEST_FILES = Set.of( //
            "empty-xml.xml", //
            "invalid-xml.xml", //
            "invalid-component-activate-message.xml", //
            "invalid-component-activate.xml", //
            "invalid-component-class.xml", //
            "invalid-component-registration-message.xml", //
            "invalid-component-registration.xml", //
            "invalid-component-start-message.xml", //
            "invalid-component-start.xml", //
            "invalid-component.xml", //
            "log4j2-test.xml");

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws Exception {
        File configDir = new File(harness.getWorkingDir(), "config");
        for (String testFile : TEST_FILES) {
            copyTestFile(testFile, "-config.xml", configDir);
        }
        // add a "duplicate component" use case
        copyTestFile("invalid-component-start-message.xml", "-dupe-config.xml", configDir);
    }

    protected void copyTestFile(String name, String suffix, File configDir) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("File not found: " + name);
        }
        // Rename file so that it matches the format for "auto-discovered" files
        String destName = name.substring(0, name.indexOf(".xml")) + suffix;
        File destFile = new File(configDir, destName);
        FileUtils.copyURLToFile(url, destFile);
    }

}
