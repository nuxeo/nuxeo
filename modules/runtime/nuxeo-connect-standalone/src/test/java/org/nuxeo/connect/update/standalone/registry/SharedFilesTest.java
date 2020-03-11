/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.standalone.registry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.PackageDef;
import org.nuxeo.connect.update.standalone.PackageTestCase;
import org.nuxeo.connect.update.task.update.UpdateManager;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * We have two packages pkg1 and pkg2:
 * <ul>
 * <li>pkg1 is installing 2 files: shared and lib1.jar in bundles dir.
 * <li>pkg2 is installing 2 files: shared and lib2.jar in bundles dir.
 * </ul>
 * First we install pkg1, then pkg2 => expect pkg2 is not really copying the shared but it updates the shared.files
 * registry adding a new reference to that JAR (we will use different content for these files to be able to track the
 * file that was really copied). Also, we expect that lib1.jar and lib2.jar were copied.<br/>
 * Then we uninstall pkg1 and we expect that shared is not removed (and the JAR is the one installed by pkg1). But
 * lib2.jar must be removed. Then we uninstall pkg2 and we expect all the 3 files were removed.
 *
 * @since 5.5
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class SharedFilesTest extends PackageTestCase {

    protected File bundles;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // be sure these directories exists and cleanup if needed
        Environment.getDefault().getConfig().mkdirs();
        bundles = new File(Environment.getDefault().getHome(), "bundles");
        org.apache.commons.io.FileUtils.deleteQuietly(bundles);
        bundles.mkdirs();
        service.getRegistry().delete();
        org.apache.commons.io.FileUtils.deleteDirectory(service.getBackupDir());
    }

    protected void createFakeBundles() throws Exception {
        // create some fake bundles
        FileUtils.writeStringToFile(new File(bundles, "b1-1.0.jar"), "b1-1.0.jar", UTF_8);
        FileUtils.writeStringToFile(new File(bundles, "b2-1.0.jar"), "b2-1.0.jar", UTF_8);
    }

    public UpdateManager getManager() throws Exception {
        UpdateManager mgr = new UpdateManager(Environment.getDefault().getHome(), service.getRegistry());
        mgr.load();
        return mgr;
    }

    protected void ensureBaseVersion() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles("b1-1.0.jar", "b2-1.0.jar");
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }
    }

    public void ensurePkg1() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(2, mgr.getRegistry().size());
        ensureFiles("b1-1.1.jar", "b2-1.1.jar");
    }

    /**
     * @deprecated this is no more the case - since handling explicit constraints was fixed.
     */
    @Deprecated
    public void ensurePkg1AfterBlockingDowngrade() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(2, mgr.getRegistry().size());
        ensureFiles("b1-1.2.jar", "b2-1.1.jar");
    }

    public void ensurePkg2() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(2, mgr.getRegistry().size());
        ensureFiles("b1-1.2.jar", "b2-1.0.jar", "lib2-1.0.jar");
    }

    public void ensurePkg12() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(3, mgr.getRegistry().size());
        ensureFiles("b1-1.2.jar", "b2-1.1.jar", "lib2-1.0.jar");
    }

    /**
     * Here a downgrade is made - by default downgrade is not allowed see {@link #ensurePkg21WithDowngrade()}
     *
     * @throws Exception
     */
    public void ensurePkg21() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(3, mgr.getRegistry().size());
        ensureFiles("b1-1.2.jar", "b2-1.1.jar", "lib2-1.0.jar");
    }

    public void ensurePkg21WithDowngrade() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(3, mgr.getRegistry().size());
        ensureFiles("b1-1.1.jar", "b2-1.1.jar", "lib2-1.0.jar");
    }

    protected void ensureFiles(String... names) throws Exception {
        HashSet<String> set = new HashSet<>(Arrays.asList(bundles.list()));
        assertEquals("Number of files in " + bundles.toString(), names.length, set.size());
        for (String name : names) {
            assertTrue("Missing file: " + name, set.contains(name));
        }
        for (String name : names) {
            assertEquals("Wrong file content for " + name, name,
                    FileUtils.readFileToString(new File(bundles, name), UTF_8));
        }
    }

    public class Pkg1 extends PackageDef {

        public Pkg1() throws Exception {
            super("pkg1", "5.5", service);
        }

        @Override
        protected void updatePackage() throws Exception {
            addFile("bundles/b1-1.1.jar", "b1-1.1.jar");
            addFile("bundles/b2-1.1.jar", "b2-1.1.jar");
        }

        @Override
        protected void writeInstallCommands(XmlWriter writer) throws Exception {
            writer.start("update");
            writer.attr("file", "${package.root}/bundles");
            writer.attr("todir", "${env.bundles}");
            writer.attr("allowDowngrade", Boolean.toString(allowDowngrade));
            writer.attr("upgradeOnly", Boolean.toString(upgradeOnly));
            writer.end();
        }

    }

    public class Pkg2 extends PackageDef {
        public Pkg2() throws Exception {
            super("pkg2", "5.5", service);
        }

        @Override
        protected void updatePackage() throws Exception {
            addFile("lib2-1.0.jar", "lib2-1.0.jar");
            addFile("b1-1.2.jar", "b1-1.2.jar");
        }

        @Override
        protected void writeInstallCommands(XmlWriter writer) throws Exception {
            writer.start("update");
            writer.attr("file", "${package.root}/b1-1.2.jar");
            writer.attr("todir", "${env.bundles}");
            writer.attr("allowDowngrade", Boolean.toString(allowDowngrade));
            writer.attr("upgradeOnly", Boolean.toString(upgradeOnly));
            writer.end();
            writer.start("update");
            writer.attr("file", "${package.root}/lib2-1.0.jar");
            writer.attr("todir", "${env.bundles}");
            writer.attr("allowDowngrade", Boolean.toString(allowDowngrade));
            writer.attr("upgradeOnly", Boolean.toString(upgradeOnly));
            writer.end();
        }
    }

}
