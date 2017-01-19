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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.osgi.util.jar.URLJarFileIntrospectionError;
import org.nuxeo.osgi.util.jar.URLJarFileIntrospector;
import org.nuxeo.runtime.osgi.util.jar.index.BuildMetaIndex;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import sun.misc.MetaIndex;

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
    @Ignore("NXP-7109")
    public void canGenerateMetaIndex() throws FileNotFoundException, IOException {
        URL firstURL = jarBuilder.buildFirst();
        URL otherURL = jarBuilder.buildOther();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BuildMetaIndex.build(new PrintStream(out), firstURL.getPath(), otherURL.getPath());
        String index = out.toString();
        assertThat(index, containsString("org/nuxeo"));
        assertThat(index, containsString(firstURL.getFile()));
        assertThat(index, containsString(otherURL.getFile()));
    }

    @Test
    @Ignore("NXP-7109")
    public void dontOpenJar() throws MalformedURLException {
        File bundles = new File("bundles");
        MetaIndex.registerDirectory(bundles);
        URLClassLoader cl = newClassLoader(bundles);
        URL template = cl.findResource("templates/FileOpen.ftl");
        // TODO NXP-7109 check launchers
        assertThat(template, notNullValue());
        Assert.fail();
    }

    private URLClassLoader newClassLoader(File bundles) throws MalformedURLException {
        URL[] urls = new URL[] { new File(bundles, "classes.jar").toURI().toURL(),
                new File(bundles, "resources.jar").toURI().toURL() };
        URLClassLoader cl = new URLClassLoader(urls);
        return cl;
    }

    @Test
    public void canDeleteJar() throws FileNotFoundException, IOException, ClassNotFoundException, SecurityException,
            URLJarFileIntrospectionError {
        URL firstURL = jarBuilder.buildFirst();
        URL otherURL = jarBuilder.buildOther();
        URL[] jarURLs = new URL[] { firstURL, otherURL };
        URLClassLoader ucl = new URLClassLoader(jarURLs, null);
        assertThat(ucl.loadClass(JarBuilder.First.class.getName()), notNullValue());
        JarFile jarFile = new JarFile(jarURLs[1].getFile());
        jarFile.getManifest();
        new URLJarFileIntrospector().newJarFileCloser(ucl).close(jarFile);
        File file = new File(jarFile.getName());
        assertThat(file.delete(), is(true));
        assertThat(ucl.findResource("first.marker"), notNullValue());
        assertThat(ucl.findResource("other.marker"), nullValue());
    }

}
