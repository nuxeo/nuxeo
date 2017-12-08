/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.osgi.util.jar.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author matic
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(ConditionalIgnoreRule.Feature.class)
public class TestClassLoaderInstrumentation {

    protected JarBuilder jarBuilder;

    @Before
    public void setupJarBuilder() throws IOException {
        jarBuilder = new JarBuilder();
    }

    @After
    public void deleteBuiltFiles() {
        jarBuilder.deleteBuiltFiles();
    }

    @Test
    public void canDeleteJar() throws Exception {
        URL firstURL = jarBuilder.buildFirst();
        URL otherURL = jarBuilder.buildOther();
        URL[] jarURLs = new URL[] { firstURL, otherURL };
        try (URLClassLoader ucl = new URLClassLoader(jarURLs, null)) {
            assertThat(ucl.loadClass(JarBuilder.First.class.getName()), notNullValue());
            JarFile jarFile = new JarFile(jarURLs[1].getFile());
            jarFile.getManifest();
            jarFile.close();
            File file = new File(jarFile.getName());
            assertThat(file.delete(), is(true));
            assertThat(ucl.findResource("first.marker"), notNullValue());
            assertThat(ucl.findResource("other.marker"), nullValue());
        }
    }

}
