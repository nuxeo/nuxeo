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
 *     jcarsique
 */
package org.nuxeo.connect.update.standalone;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public abstract class PackageTestCase {

    protected static final Log log = LogFactory.getLog(PackageTestCase.class);

    public static final String TEST_PACKAGES_PREFIX = "packages/";

    protected PackageUpdateService service;

    /**
     * Calls {@link #setupService()} to setup the service
     *
     * @see #setupService()
     */
    @Before
    public void setUp() throws Exception {
        setupService();
    }

    @After
    public void tearDown() throws Exception {
        if (service instanceof StandaloneUpdateService) {
            tearDownStandaloneUpdateService();
        }
    }

    protected File getTestPackageZip(String name) throws IOException, URISyntaxException {
        File zip = Framework.createTempFile("nuxeo-" + name + "-", ".zip");
        Framework.trackFile(zip, zip);
        URI uri = getResource(TEST_PACKAGES_PREFIX + name).toURI();
        if (uri.getScheme().equals("jar")) {
            String part = uri.getSchemeSpecificPart(); // file:/foo/bar.jar!/a/b
            String basePath = part.substring(part.lastIndexOf("!") + 1);
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                createZip(zip, fs.getPath(basePath));
            }
        } else { // file: scheme
            createZip(zip, new File(uri).toPath());
        }
        return zip;
    }

    protected URL getResource(String name) {
        return getClass().getClassLoader().getResource(name);
    }

    /** Zips a directory into the given ZIP file. */
    protected void createZip(File zip, Path basePath) throws IOException {
        try (ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)))) {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }
                    String rel = basePath.relativize(path).toString();
                    if (rel.startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    zout.putNextEntry(new ZipEntry(rel));
                    try (InputStream in = Files.newInputStream(path)) {
                        org.apache.commons.io.IOUtils.copy(in, zout);
                    }
                    zout.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            zout.flush();
        }
    }

    /**
     * Default implementation sets a {@link StandaloneUpdateService}
     */
    protected void setupService() throws IOException, PackageException {
        File tmpHome = Framework.createTempFile("tmphome", null);
        Framework.trackFile(tmpHome, tmpHome);
        FileUtils.forceDelete(tmpHome);
        tmpHome.mkdirs();
        Environment env = new Environment(tmpHome);
        Environment.setDefault(env);
        env.setServerHome(tmpHome);
        env.init();
        service = new StandaloneUpdateService(env);
        service.initialize();
        File storeDir = ((StandaloneUpdateService) service).getPersistence().getStore();
        File junkPackageFile = File.createTempFile("junk", null, storeDir);
        junkPackageFile.deleteOnExit();
    }

    protected void tearDownStandaloneUpdateService() {
        FileUtils.deleteQuietly(Environment.getDefault().getHome());
    }

}
