package org.nuxeo.runtime.tomcat.adapter.tests;
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.osgi.util.jar.JarFileCloser;
import org.nuxeo.runtime.tomcat.adapter.tests.metaindex.BuildMetaIndex;

import sun.misc.MetaIndex;

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */

/**
 * @author matic
 * @since 5.6
 */
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
    @Ignore
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
    @Ignore
    public void dontOpenJar() throws MalformedURLException {
        File bundles = new File("bundles");
        MetaIndex.registerDirectory(bundles);
        URLClassLoader cl = newClassLoader(bundles);
        URL template = cl.findResource("templates/FileOpen.ftl");
        // TODO check launchers
        assertThat(template,notNullValue());
        Assert.fail();
    }

    private URLClassLoader newClassLoader(File bundles)
            throws MalformedURLException {
        URL[] urls = new URL[] {
                new File(bundles, "classes.jar").toURI().toURL(),
                new File(bundles, "resources.jar").toURI().toURL()
        };
        URLClassLoader cl = new URLClassLoader(urls);
        return cl;
    }


    @Test
    public void canDeleteJar() throws FileNotFoundException, IOException, ClassNotFoundException {
        URL firstURL = jarBuilder.buildFirst();
        URL otherURL = jarBuilder.buildOther();
        URL[] jarURLs = new URL[] { firstURL, otherURL };
        URLClassLoader ucl = new URLClassLoader(jarURLs, null);
        assertThat(ucl.loadClass(TestClassLoaderInstrumentation.class.getName()), notNullValue());
        JarFile jarFile = new JarFile(jarURLs[1].getFile());
        jarFile.getManifest();
        new JarFileCloser(new URLClassLoader(new URL[] {}), ucl).close(jarFile);
        File file = new File(jarFile.getName());
        assertThat(file.delete(), is(true));
        assertThat(ucl.findResource("first.marker"), notNullValue());
        assertThat(ucl.findResource("other.marker"), nullValue());
    }


}
