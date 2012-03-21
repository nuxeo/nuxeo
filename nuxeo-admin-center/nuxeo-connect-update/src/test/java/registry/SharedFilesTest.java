/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package registry;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.PackageDef;
import org.nuxeo.connect.update.PackageTestCase;
import org.nuxeo.connect.update.standalone.task.update.UpdateManager;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * We have two packages pkg1 and pkg2:
 * <ul>
 * <li>pkg1 is installing 2 files: shared and lib1.jar in bundles dir.
 * <li>pkg2 is installing 2 files: shared and lib2.jar in bundles dir.
 * </ul>
 *
 * First we install pkg1, the pkg2 => expect pkg2 is not really copying the
 * shared but it updates the shared.files registry adding a new reference to
 * that jar. (we will use different content for these files to be able to track
 * the file that was really copied) Also, we expect that lin1.jar and lib2.jar
 * was copied.
 *
 * Then we uninstall pkg1 and we expect that lib1.jar is not removed (and the
 * jar is the one installed by pkg1). But lib1.jar must be removed. Then we
 * uninstall pkg2 and we expect all the 3 files were removed.
 *
 * @since 5.5
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SharedFilesTest extends PackageTestCase {

    protected File bundles;

    @Override
    @Before
    public void setUp() throws Exception {
        // be sure these directories exists
        Environment.getDefault().getConfig().mkdirs();
        bundles = new File(Environment.getDefault().getHome(), "bundles");
        bundles.mkdirs();
        // cleanup
        FileUtils.emptyDirectory(bundles);
        new File(service.getDataDir(), "registry.xml").delete();
        FileUtils.deleteTree(new File(service.getDataDir(), "backup"));
    }

    protected void createFakeBundles() throws Exception {
        // create some fake bundles
        FileUtils.writeFile(new File(bundles, "b1-1.0.jar"), "b1-1.0.jar");
        FileUtils.writeFile(new File(bundles, "b2-1.0.jar"), "b2-1.0.jar");
    }

    public UpdateManager getManager() throws Exception {
        UpdateManager mgr = new UpdateManager(
                Environment.getDefault().getHome(), new File(
                        service.getDataDir(), "registry.xml"));
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
     * @deprecated this is no more the case - since handling explicit
     *             constraints was fixed.
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
     * Here a downgrade is made - by default downgrade is not allowed see
     * {@link #ensurePkg21WithDowngrade()}
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
        HashSet<String> set = new HashSet<String>(Arrays.asList(bundles.list()));
        assertEquals(set.size(), names.length);
        for (String name : names) {
            assertTrue(set.contains(name));
        }
        for (String name : names) {
            assertEquals(name, FileUtils.readFile(new File(bundles, name)));
        }
    }

    public class Pkg1 extends PackageDef {

        public Pkg1() throws Exception {
            super("pkg1", "5.5");
        }

        @Override
        protected void updatePackage(PackageBuilder builder) throws Exception {
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
            super("pkg2", "5.5");
        }

        @Override
        protected void updatePackage(PackageBuilder builder) throws Exception {
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
