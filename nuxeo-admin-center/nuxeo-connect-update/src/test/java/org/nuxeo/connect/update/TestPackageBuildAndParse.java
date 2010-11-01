package org.nuxeo.connect.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.impl.UpdateServiceImpl;
import org.nuxeo.connect.update.impl.task.InstallTask;
import org.nuxeo.connect.update.impl.task.UninstallTask;
import org.nuxeo.connect.update.impl.xml.PackageDefinitionImpl;
import org.nuxeo.connect.update.util.PackageBuilder;

import junit.framework.TestCase;

public class TestPackageBuildAndParse extends TestCase {

    public void testBuildAndParse() throws Exception {

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

        builder.hotReloadSupport(true);
        builder.supported(true);
        builder.validationState(NuxeoValidationState.INPROCESS);
        builder.productionState(ProductionState.PRODUCTION_READY);
        builder.requireTermsAndConditionsAcceptance(true);

        String manifest = builder.buildManifest();
        System.out.println(manifest);

        XMap xmap = UpdateServiceImpl.createXmap();
        InputStream xmlIn = new ByteArrayInputStream(manifest.getBytes());
        PackageDefinitionImpl packageDef = (PackageDefinitionImpl) xmap.load(xmlIn);

        assertEquals("nuxeo-automation", packageDef.getName());
        assertEquals("Nuxeo", packageDef.getVendor());
        assertEquals(NuxeoValidationState.INPROCESS, packageDef.getValidationState());
        assertEquals(ProductionState.PRODUCTION_READY, packageDef.getProductionState());
        assertEquals(true, packageDef.requireTermsAndConditionsAcceptance());
        assertEquals(true, packageDef.isSupported());
        assertEquals(true, packageDef.supportsHotReload());


    }
}
