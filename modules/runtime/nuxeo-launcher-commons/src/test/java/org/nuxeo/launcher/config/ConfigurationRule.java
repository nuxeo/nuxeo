/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import static org.nuxeo.common.function.ThrowableConsumer.asConsumer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.nuxeo.common.Environment;

/**
 * A rule which configures a NUXEO_HOME directory for tests.
 * <p/>
 * Before the test starts, this rule will:
 * <ul>
 * <li>reset nuxeo {@link Environment}</li>
 * <li>delete the {@code target/ClassName/MethodName} directory</li>
 * <li>create the {@code target/ClassName/MethodName} directory holding the nuxeo home</li>
 * <li>copy the content from {@code testDefault} directory to it</li>
 * <li>copy the content from {@code MethodName} directory to it</li>
 * <li>move the {@code nuxeo.conf} file present in the nuxeo home to the {@code bin} directory</li>
 * </ul>
 *
 * @since 11.5
 */
public class ConfigurationRule extends TestWatcher {

    protected static final Logger log = LogManager.getLogger(ConfigurationRule.class);

    protected static final String CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY = "custom.environment";

    protected static final String DEFAULT_BUILD_DIRECTORY = "target";

    protected final String baseDirectory;

    protected Path nuxeoHome;

    protected Path nuxeoConf;

    public ConfigurationRule(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    protected void starting(Description description) {
        try {
            // clear nuxeo environment
            Environment.setDefault(null);
            // build nuxeo home directory for test
            String buildDirectory = getBuildDirectory();
            nuxeoHome = Path.of(buildDirectory, description.getTestClass().getSimpleName(), description.getMethodName())
                            .toAbsolutePath();
            // use commons-io as it deletes directories recursively
            FileUtils.deleteQuietly(nuxeoHome.toFile());
            // copy nuxeo home testDefault directory
            FileUtils.copyDirectory(getDefaultHome().toFile(), nuxeoHome.toFile());
            // copy nuxeo home specific directory
            getOptionalResourcePath(description.getMethodName()).ifPresent(asConsumer(p -> {
                log.info("Running the test: {} with nuxeo home: {}", description.getMethodName(), p);
                FileUtils.copyDirectory(p.toFile(), nuxeoHome.toFile());
            }));
            // create bin as we're moving nuxeo.conf to it
            Path nuxeoBin = nuxeoHome.resolve("bin");
            Files.createDirectories(nuxeoBin);
            // move nuxeo conf to the expected location
            nuxeoConf = Files.move(nuxeoHome.resolve("nuxeo.conf"), nuxeoBin.resolve("nuxeo.conf"));
            // create nxserver/config as we may dump the configuration.properties
            Files.createDirectories(nuxeoHome.resolve("nxserver").resolve("config"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the nuxeo home {@link Path} configured by this rule
     */
    public Path getNuxeoHome() {
        return nuxeoHome;
    }

    /**
     * @return the nuxeo conf {@link Path} configured by this rule
     */
    public Path getNuxeoConf() {
        return nuxeoConf;
    }

    /**
     * Returns the Maven build directory, depending on the {@value #CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY} system property.
     */
    public String getBuildDirectory() {
        String customEnvironment = System.getProperty(CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY);
        return customEnvironment == null ? DEFAULT_BUILD_DIRECTORY
                : String.format("%s-%s", DEFAULT_BUILD_DIRECTORY, customEnvironment);
    }

    protected Path getDefaultHome() {
        return getOptionalResourcePath("testDefault").orElseThrow(() -> new AssertionError(
                "Unable to find the default nuxeo home for test, check the test configuration"));
    }

    /**
     * @return the resource {@link Path} under the given {@link #baseDirectory}
     */
    public Path getResourcePath(String resource) {
        return getOptionalResourcePath(resource).orElseThrow(() -> new AssertionError(
                "Unable to find the resource: " + baseDirectory + '/' + resource + ", check the test configuration"));
    }

    /**
     * @return the resource {@link Path} under the given {@link #baseDirectory} as {@link Optional}
     */
    public Optional<Path> getOptionalResourcePath(String resource) {
        return Optional.ofNullable(getClass().getClassLoader().getResource(baseDirectory + '/' + resource))
                       .map(URL::getPath)
                       .map(Path::of);
    }
}
