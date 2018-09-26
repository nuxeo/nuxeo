/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.connect.update.standalone.registry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.nuxeo.connect.update.PackageDef;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.update.Entry;
import org.nuxeo.connect.update.task.update.UpdateManager;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * @since 7.1
 */
@Features(LogCaptureFeature.class)
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

    public class HotFixPackage1Corrupted extends HotFixPackage1 {

        @Override
        protected void writeInstallCommands(XmlWriter writer) {
            writer.start("update");
            writer.attr("file", "${package.root}/bundles");
            writer.attr("todir", "${env.bundles}");
            writer.attr("upgradeOnly", "true");
            writer.end();
            // NXP-19081 : add an overlapping copy task that will corrupt the registry result
            writer.start("copy");
            writer.attr("file", "${package.root}/bundles/" + getFileName());
            writer.attr("todir", "${env.bundles}");
            writer.attr("overwrite", "true");
            writer.end();
        }
    }

    public class HotFixPackage2 extends HotFixPackage1 {
        @Override
        public String getFileName() {
            return JARNAME + "-5.6.0-HF02.jar";
        }

        public HotFixPackage2() {
            super("5.6.0-HF02", "1.0.0", PackageType.HOT_FIX, service);
        }
    }

    public class HotFixPackage2Corrupted extends HotFixPackage2 {

        @Override
        protected void writeInstallCommands(XmlWriter writer) {
            writer.start("update");
            writer.attr("file", "${package.root}/bundles");
            writer.attr("todir", "${env.bundles}");
            writer.attr("upgradeOnly", "true");
            writer.end();
            // NXP-19081 : add an overlapping copy task that will corrupt the registry result
            writer.start("copy");
            writer.attr("file", "${package.root}/bundles/" + getFileName());
            writer.attr("todir", "${env.bundles}");
            writer.attr("overwrite", "true");
            writer.end();
        }
    }

    public class HotFixPackage3 extends HotFixPackage1 {
        @Override
        public String getFileName() {
            return JARNAME + "-5.6.0-HF03.jar";
        }

        public HotFixPackage3() {
            super("5.6.0-HF03", "1.0.0", PackageType.HOT_FIX, service);
        }
    }

    public class HotFixPackage4 extends HotFixPackage1 {
        @Override
        public String getFileName() {
            return JARNAME + "-5.6.0-HF04.jar";
        }

        public HotFixPackage4() {
            super("5.6.0-HF04", "1.0.0", PackageType.HOT_FIX, service);
        }
    }

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    public static class RegistryCorruptionLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            return event.getLevel().equals(Level.WARN)
                    && (event.getLoggerName().contains("UpdateManager") || event.getLoggerName().contains("Copy"));
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
        final String baseFilename = JARNAME + "-5.6.jar";
        FileUtils.writeStringToFile(new File(bundles, baseFilename), baseFilename, UTF_8);
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles(baseFilename);
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
        ensureFiles(baseFilename);

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
        ensureFiles(baseFilename);
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-19081">NXP-19081 - Package install should add missing base
     * versions in the registry</a>:
     * <ul>
     * <li>install a corrupted hotfix package copying a bundle without setting a base version
     * <li>install a second hotfix packages upgrading the same bundle and repairing the registry by adding the missing
     * base version
     * <li>uninstall the second hotfix -> the bundle introduced by the first hotfix is restored
     * <li>uninstall the corrupted hotfix -> the bundle is removed
     * </ul>
     */
    @Test
    @LogCaptureFeature.FilterWith(RegistryCorruptionLogFilter.class)
    public void testHotfixUninstallWithCorruptedRegistry() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
        File bak = new File(mgr.getBackupRoot(), "bundles");
        if (bak.isDirectory()) {
            assertEquals(0, bak.list().length);
        }

        HotFixPackage1Corrupted hotfix1Corrupted = new HotFixPackage1Corrupted();
        hotfix1Corrupted.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        Entry entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        // The jar has been deployed by the hotfix1Corrupted even if its update task is upgradeOnly, so the registry is
        // in a corrupted state with one upgradeOnly version and no baseVersion
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(hotfix1Corrupted.getFileName());

        // Check that the issue only happens with two successive hotfixes on the same JAR
        hotfix1Corrupted.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles();

        hotfix1Corrupted.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        // The jar has been deployed by the hotfix1Corrupted even if its update task is upgradeOnly, so the registry is
        // in a corrupted state with one upgradeOnly version and no baseVersion
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles(hotfix1Corrupted.getFileName());

        HotFixPackage2 hotfix2 = new HotFixPackage2();
        hotfix2.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        // Here the registry has been repaired by the hotfix2 installation (i.e there is a baseVersion)
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2.getFileName());

        hotfix2.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        // Here the hotfix1Corrupted JAR is kept thanks to the registry repair
        ensureFiles(hotfix1Corrupted.getFileName());

        hotfix1Corrupted.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles();

        // check logs
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(3, caughtEvents.size());
        assertEquals(String.format(
                "Use of the <copy /> command on JAR files is not recommended, prefer using <update /> command to ensure a safe rollback. (%s)",
                JARNAME + "-5.6.0-HF01.jar"), caughtEvents.get(0));
        assertEquals(String.format(
                "Use of the <copy /> command on JAR files is not recommended, prefer using <update /> command to ensure a safe rollback. (%s)",
                JARNAME + "-5.6.0-HF01.jar"), caughtEvents.get(1));
        assertEquals(String.format(
                "Registry repaired: JAR introduced without corresponding entry in the registry (copy task?) : bundles%s",
                File.separator + JARNAME), caughtEvents.get(2));
    }

    /**
     * Test <a href="https://jira.nuxeo.com/browse/NXP-19081">NXP-19081 - Package install should add missing base
     * versions in the registry</a>:
     * <ul>
     * <li>install a first hotfix package that just add an upgradeOnly version of a bundle in the registry
     * <li>install a corrupted hotfix package copying another version of the bundle without setting a base version
     * <li>install a third hotfix package upgrading the same bundle and repairing the registry by adding the missing
     * base version
     * <li>uninstall the third hotfix -> the bundle introduced by the second hotfix is restored
     * <li>uninstall the corrupted second hotfix -> the bundle is removed
     * <li>uninstall the first hotfix -> the bundle stays removed
     * </ul>
     */
    @Test
    @LogCaptureFeature.FilterWith(RegistryCorruptionLogFilter.class)
    public void testHotfixUninstallWithCorruptedRegistry2() throws Exception {
        UpdateManager mgr = getManager();
        assertEquals(0, mgr.getRegistry().size());
        ensureFiles();
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
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles();

        HotFixPackage2Corrupted hotfix2Corrupted = new HotFixPackage2Corrupted();
        hotfix2Corrupted.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        // The jar has been deployed by the hotfix2Corrupted even if its update task is upgradeOnly, so the registry is
        // in a corrupted state with two upgradeOnly versions and no baseVersion
        assertFalse("Should have no base version", entry.hasBaseVersion());
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        ensureFiles(hotfix2Corrupted.getFileName());

        HotFixPackage3 hotfix3 = new HotFixPackage3();
        hotfix3.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        // Here the registry has been repaired by the hotfix3 installation (i.e there is a baseVersion)
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("5.6.0-HF02", entry.getBaseVersion().getVersion());
        assertEquals("Nb versions in registry", 3, entry.getVersions().size());
        ensureFiles(hotfix3.getFileName());

        HotFixPackage4 hotfix4 = new HotFixPackage4();
        hotfix4.install();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("5.6.0-HF02", entry.getBaseVersion().getVersion());
        assertEquals("Nb versions in registry", 4, entry.getVersions().size());
        ensureFiles(hotfix4.getFileName());

        hotfix4.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("5.6.0-HF02", entry.getBaseVersion().getVersion());
        assertEquals("Nb versions in registry", 3, entry.getVersions().size());
        ensureFiles(hotfix3.getFileName());

        hotfix3.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        assertNotNull("Entry in registry", entry);
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("5.6.0-HF02", entry.getBaseVersion().getVersion());
        assertEquals("Nb versions in registry", 2, entry.getVersions().size());
        // Here the hotfix2Corrupted JAR is kept thanks to the registry repair
        ensureFiles(hotfix2Corrupted.getFileName());

        hotfix2Corrupted.uninstall();
        mgr.load();
        assertEquals("Registry size", 1, mgr.getRegistry().size());
        entry = mgr.getRegistry().get("bundles" + File.separator + JARNAME);
        // Here the base version is still here but will not be used by any rollback command because the JAR is not
        // present anymore
        assertTrue("Should have a base version", entry.hasBaseVersion());
        assertEquals("5.6.0-HF02", entry.getBaseVersion().getVersion());
        assertEquals("Nb versions in registry", 1, entry.getVersions().size());
        ensureFiles();

        hotfix1.uninstall();
        mgr.load();
        assertEquals("Registry size", 0, mgr.getRegistry().size());
        ensureFiles();

        // check logs
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(3, caughtEvents.size());
        assertEquals(String.format(
                "Use of the <copy /> command on JAR files is not recommended, prefer using <update /> command to ensure a safe rollback. (%s)",
                JARNAME + "-5.6.0-HF02.jar"), caughtEvents.get(0));
        assertEquals(String.format(
                "Registry repaired: JAR introduced without corresponding entry in the registry (copy task?) : bundles%s",
                File.separator + JARNAME), caughtEvents.get(1));
        assertEquals(String.format("Could not rollback version bundles%s since the backup file was not found",
                File.separator + JARNAME + "-5.6.0-HF02.jar"), caughtEvents.get(2));

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
