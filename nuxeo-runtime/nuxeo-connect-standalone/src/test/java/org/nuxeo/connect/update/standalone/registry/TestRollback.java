/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.standalone.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.PackageDef;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.update.Entry;
import org.nuxeo.connect.update.task.update.UpdateManager;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * @since 7.1
 */
public class TestRollback extends SharedFilesTest {
    public static final String JARNAME = "some-jar";

    public class AddonPackage extends PackageDef {
        public String getFileName() {
            return JARNAME + "-5.6.jar";
        }

        public AddonPackage() {
            super("some-addon", "1.0.0", PackageType.ADDON, service);
        }

        public AddonPackage(String name, String version, PackageType type, PackageUpdateService service) {
            super(name, version, type, service);
        }

        @Override
        protected void writeInstallCommands(XmlWriter writer) throws Exception {
            writer.start("update");
            writer.attr("file", "${package.root}/bundles");
            writer.attr("todir", "${env.bundles}");
            writer.end();
        }

        @Override
        protected void updatePackage() throws Exception {
            addFile("bundles" + File.separator + getFileName(), getFileName());
        }
    }

    public class HotFixPackage1 extends AddonPackage {
        @Override
        public String getFileName() {
            return JARNAME + "-5.6.0-HF01.jar";
        }

        public HotFixPackage1() {
            super("5.6.0-HF01", "1.0.0", PackageType.HOT_FIX, service);
        }

        public HotFixPackage1(String name, String version, PackageType type, PackageUpdateService service) {
            super(name, version, type, service);
        }

        @Override
        protected void writeInstallCommands(XmlWriter writer) throws Exception {
            writer.start("update");
            writer.attr("file", "${package.root}/bundles");
            writer.attr("todir", "${env.bundles}");
            writer.attr("upgradeOnly", "true");
            writer.end();
        }
    }

    public class HotFixPackage2 extends HotFixPackage1 {
        @Override
        public String getFileName() {
            return JARNAME + "-5.6.0-HF02.jar";
        }

        public HotFixPackage2() throws Exception {
            super("5.6.0-HF02", "1.0.0", PackageType.HOT_FIX, service);
        }
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-11734">NXP-11734 - Fix hotfix uninstallation</a>:
     * <ul>
     * <li>install two hotfix packages both upgrading a bundle (already present with a base version)
     * <li>uninstall the second package
     * </ul>
     * The expected version for the first hotfix, not the base version.
     */
    @Test
    public void testHotfixUninstall() throws Exception {
        final String BASEFILENAME = JARNAME + "-5.6.jar";
        FileUtils.writeFile(new File(bundles, BASEFILENAME), BASEFILENAME);
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles(BASEFILENAME);
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        HotFixPackage1 hotfix1 = new HotFixPackage1();
        hotfix1.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(hotfix1.getFileName());

        // Check that the issue only happens with two successive hotfixes on the same JAR
        hotfix1.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles(BASEFILENAME);

        hotfix1.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(hotfix1.getFileName());

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        hotfix2.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(hotfix1.getFileName());

        hotfix1.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles(BASEFILENAME);
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-10164">NXP-10164 - Fix how Update command manages the
     * upgrade-only attribute</a>:
     * <ol>
     * <li>Install addon, install HF, uninstall HF, uninstall addon
     * <li>Install addon, install HF, uninstall addon
     * <li>Install HF, install addon, uninstall HF, uninstall addon
     * <li>Install HF, install addon, uninstall addon
     * </ol>
     * The JAR hoftix version must be installed disregarding the install order between HF and addon.<br/>
     * No JAR version must be installed when addon is not installed.
     */
    @Test
    public void testUpgradeOnly1() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        AddonPackage addon = new AddonPackage();
        addon.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(addon.getFileName());

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        hotfix2.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(addon.getFileName());

        addon.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles();
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-10164">NXP-10164 - Fix how Update command manages the
     * upgrade-only attribute</a>:
     * <ol>
     * <li>Install addon, install HF, uninstall HF, uninstall addon
     * <li>Install addon, install HF, uninstall addon
     * <li>Install HF, install addon, uninstall HF, uninstall addon
     * <li>Install HF, install addon, uninstall addon
     * </ol>
     * The JAR hoftix version must be installed disregarding the install order between HF and addon.<br/>
     * No JAR version must be installed when addon is not installed.
     */
    @Test
    public void testUpgradeOnly2() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        AddonPackage addon = new AddonPackage();
        addon.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(addon.getFileName());

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        addon.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        assertNull("Remaining version should be upgradeOnly", entry.getLastVersion(false));
        ensureFiles();
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-10164">NXP-10164 - Fix how Update command manages the
     * upgrade-only attribute</a>:
     * <ol>
     * <li>Install addon, install HF, uninstall HF, uninstall addon
     * <li>Install addon, install HF, uninstall addon
     * <li>Install HF, install addon, uninstall HF, uninstall addon
     * <li>Install HF, install addon, uninstall addon
     * </ol>
     * The JAR hoftix version must be installed disregarding the install order between HF and addon.<br/>
     * No JAR version must be installed when addon is not installed.
     */
    @Test
    public void testUpgradeOnly3() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles();

        AddonPackage addon = new AddonPackage();
        addon.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        hotfix2.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(addon.getFileName());

        addon.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles();
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-10164">NXP-10164 - Fix how Update command manages the
     * upgrade-only attribute</a>:
     * <ol>
     * <li>Install addon, install HF, uninstall HF, uninstall addon
     * <li>Install addon, install HF, uninstall addon
     * <li>Install HF, install addon, uninstall HF, uninstall addon
     * <li>Install HF, install addon, uninstall addon
     * </ol>
     * The JAR hoftix version must be installed disregarding the install order between HF and addon.<br/>
     * No JAR version must be installed when addon is not installed.
     */
    @Test
    public void testUpgradeOnly4() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles();

        PackageDef addon = new AddonPackage();
        addon.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        addon.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        assertNull("Remaining version should be upgradeOnly", entry.getLastVersion(false));
        ensureFiles();
    }

    /**
     * Test mixing <a href="https://jira.nuxeo.com/browse/NXP-10164">NXP-10164 - Fix how Update command manages the
     * upgrade-only attribute</a> and <a href="https://jira.nuxeo.com/browse/NXP-11734">NXP-11734 - Fix hotfix
     * uninstallation</a>:
     * <ol>
     * <li>Install addon, install HF1, install HF2, uninstall HF2, uninstall addon
     * </ol>
     * The JAR hoftix version must be installed disregarding the install order between HF and addon.<br/>
     * No JAR version must be installed when addon is not installed.
     */
    @Test
    public void testUpgradeOnlyWithHotfixUninstall() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        AddonPackage addon = new AddonPackage();
        addon.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(addon.getFileName());

        HotFixPackage1 hotfix1 = new HotFixPackage1();
        hotfix1.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix1.getFileName());

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 3, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        hotfix2.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix1.getFileName());

        addon.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        assertNull("Remaining version should be upgradeOnly", entry.getLastVersion(false));
        ensureFiles();
    }
}
