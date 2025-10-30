/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * @since 2025.0
 */
public class ModuleUnderTest {

    protected static final String CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY = "custom.environment";

    protected static final String DEFAULT_OUTPUT_DIRECTORY = "target";

    /**
     * @since 2023.40
     */
    public static String getClassLoaderResourceAsString(String name) {
        try (var stream = ModuleUnderTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("The resource is null");
            }
            return IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve resource: " + name, e);
        }
    }

    public static String getOutputDirectory() {
        String customEnvironment = System.getProperty(CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY);
        return customEnvironment == null ? DEFAULT_OUTPUT_DIRECTORY
                : String.format("%s-%s", DEFAULT_OUTPUT_DIRECTORY, customEnvironment);
    }
}
