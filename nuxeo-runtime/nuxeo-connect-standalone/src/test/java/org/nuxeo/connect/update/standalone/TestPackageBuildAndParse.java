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
 *     Mathieu Guillaume
 *     Julien Carsique
 *
 */
package org.nuxeo.connect.update.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.junit.Test;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.ProductionState;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.runtime.api.Framework;

public class TestPackageBuildAndParse extends PackageTestCase {

    @Test
    public void testBuildAndParse() throws Exception {

        String termsAndConditions = "You have to be crazy to use this package";

        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(PackageType.ADDON);
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
        File file = Framework.createTempFile("nxinstall-file-", ".tmp");
        Framework.trackFile(file, builder);
        File tofile = Framework.createTempFile("nxinstall-tofile-", ".tmp");
        Framework.trackFile(tofile, builder);
        builder.addInstallScript("<install>\n  <copy file=\"" + file.getAbsolutePath() + "\" tofile=\""
                + tofile.getAbsolutePath() + "\" overwrite=\"true\"/>\n</install>\n");

        builder.addTermsAndConditions(termsAndConditions);
        builder.hotReloadSupport(true);
        builder.supported(true);
        builder.validationState(NuxeoValidationState.INPROCESS);
        builder.productionState(ProductionState.PRODUCTION_READY);
        builder.requireTermsAndConditionsAcceptance(true);
        builder.visibility(PackageVisibility.MARKETPLACE);

        // test on package def
        String manifest = builder.buildManifest();
        // System.out.println(manifest);
        XMap xmap = StandaloneUpdateService.createXmap();
        InputStream xmlIn = new ByteArrayInputStream(manifest.getBytes());
        PackageDefinitionImpl packageDef = (PackageDefinitionImpl) xmap.load(xmlIn);
        assertEquals("nuxeo-automation", packageDef.getName());
        assertEquals("Nuxeo", packageDef.getVendor());
        assertEquals(NuxeoValidationState.INPROCESS, packageDef.getValidationState());
        assertEquals(ProductionState.PRODUCTION_READY, packageDef.getProductionState());
        assertTrue(packageDef.requireTermsAndConditionsAcceptance());
        assertTrue(packageDef.isSupported());
        assertTrue(packageDef.supportsHotReload());
        assertEquals(PackageVisibility.MARKETPLACE, packageDef.getVisibility());

        // test on real unziped package
        File zipFile = builder.build();
        File tmpDir = new File(Environment.getDefault().getTemp(), "TestPkg" + System.currentTimeMillis());
        tmpDir.mkdirs();
        ZipUtils.unzip(zipFile, tmpDir);
        LocalPackage pkg = new LocalPackageImpl(tmpDir, PackageState.REMOTE, service);
        Framework.trackFile(tmpDir, pkg);
        assertEquals(termsAndConditions, pkg.getTermsAndConditionsContent());
        assertEquals("nuxeo-automation", pkg.getName());
        assertEquals("Nuxeo", pkg.getVendor());
        assertEquals(NuxeoValidationState.INPROCESS, pkg.getValidationState());
        assertEquals(ProductionState.PRODUCTION_READY, pkg.getProductionState());
        assertTrue(pkg.requireTermsAndConditionsAcceptance());
        assertTrue(pkg.isSupported());
        assertTrue(pkg.supportsHotReload());
        assertEquals(PackageVisibility.MARKETPLACE, pkg.getVisibility());
    }

}
