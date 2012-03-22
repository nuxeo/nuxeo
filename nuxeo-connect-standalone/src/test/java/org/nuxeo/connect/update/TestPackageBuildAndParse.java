package org.nuxeo.connect.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.junit.Test;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.standalone.LocalPackageImpl;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.standalone.task.InstallTask;
import org.nuxeo.connect.update.standalone.task.UninstallTask;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;

public class TestPackageBuildAndParse {

    @Test
    public void testBuildAndParse() throws Exception {

        String termsAndConditions = "You have to be crazy to use this package";

        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
        builder.platform("dm-5.3.2");
        builder.platform("dam-5.3.2");
        builder.dependency("nuxeo-core:5.3.1:5.3.2");
        builder.dependency("nuxeo-runtime:5.3.1");
        builder.title("Nuxeo Automation");
        builder.description("A service that enable building complex business logic on top of Nuxeo services using scriptable operation chains");
        builder.classifier("Open Source");
        builder.vendor("Nuxeo");
        builder.installer(InstallTask.class.getName(), true);
        builder.uninstaller(UninstallTask.class.getName(), true);
        builder.addLicense("My test license. All rights reserved.");
        File file = File.createTempFile("nxinstall-file-", ".tmp");
        file.deleteOnExit();
        File tofile = File.createTempFile("nxinstall-tofile-", ".tmp");
        tofile.deleteOnExit();
        builder.addInstallScript("<install>\n  <copy file=\""
                + file.getAbsolutePath() + "\" tofile=\""
                + tofile.getAbsolutePath()
                + "\" overwrite=\"true\"/>\n</install>\n");

        builder.addTermsAndConditions(termsAndConditions);
        builder.hotReloadSupport(true);
        builder.supported(true);
        builder.validationState(NuxeoValidationState.INPROCESS);
        builder.productionState(ProductionState.PRODUCTION_READY);
        builder.requireTermsAndConditionsAcceptance(true);

        // test on package def
        String manifest = builder.buildManifest();
        // System.out.println(manifest);
        XMap xmap = StandaloneUpdateService.createXmap();
        InputStream xmlIn = new ByteArrayInputStream(manifest.getBytes());
        PackageDefinitionImpl packageDef = (PackageDefinitionImpl) xmap.load(xmlIn);
        assertEquals("nuxeo-automation", packageDef.getName());
        assertEquals("Nuxeo", packageDef.getVendor());
        assertEquals(NuxeoValidationState.INPROCESS,
                packageDef.getValidationState());
        assertEquals(ProductionState.PRODUCTION_READY,
                packageDef.getProductionState());
        assertTrue(packageDef.requireTermsAndConditionsAcceptance());
        assertTrue(packageDef.isSupported());
        assertTrue(packageDef.supportsHotReload());

        // test on real unziped package
        File zipFile = builder.build();
        String tmpDirPath = System.getProperty("java.io.tmpdir") + "/TestPkg"
                + System.currentTimeMillis();
        File tmpDir = new File(tmpDirPath);
        tmpDir.mkdirs();
        ZipUtils.unzip(zipFile, tmpDir);
        // TODO JC: check environment
        PackageUpdateService srv = new StandaloneUpdateService(
                Environment.getDefault());
        LocalPackage pkg = new LocalPackageImpl(tmpDir, 0, srv);
        assertEquals(termsAndConditions, pkg.getTermsAndConditionsContent());
        assertEquals("nuxeo-automation", pkg.getName());
        assertEquals("Nuxeo", pkg.getVendor());
        assertEquals(NuxeoValidationState.INPROCESS, pkg.getValidationState());
        assertEquals(ProductionState.PRODUCTION_READY, pkg.getProductionState());
        assertTrue(pkg.requireTermsAndConditionsAcceptance());
        assertTrue(pkg.isSupported());
        assertTrue(pkg.supportsHotReload());
    }

}
