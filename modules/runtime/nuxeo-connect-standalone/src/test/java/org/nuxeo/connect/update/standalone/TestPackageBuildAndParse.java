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

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.runtime.api.Framework;

public class TestPackageBuildAndParse extends PackageTestCase {

    protected void doTestBuildAndParse(boolean cap) throws Exception {
        String name = "test-parse-" + (cap ? "cap" : "nocap");

        XMap xmap = StandaloneUpdateService.createXmap();

        // check manifest
        PackageDefinitionImpl packageDef;
        try (InputStream xmlIn = getClass().getClassLoader().getResourceAsStream(
                TEST_PACKAGES_PREFIX + name + "/package.xml")) {
            packageDef = (PackageDefinitionImpl) xmap.load(xmlIn);
        }
        assertEquals("nuxeo-automation", packageDef.getName());
        assertEquals("Nuxeo", packageDef.getVendor());
        assertTrue(packageDef.requireTermsAndConditionsAcceptance());
        assertTrue(packageDef.supportsHotReload());
        Set<String> expectedTargetPlatforms = new HashSet<>(Arrays.asList("dm-5.3.2", "dam-5.3.2"));
        if (cap) {
            expectedTargetPlatforms.add("cap-8.3");
            expectedTargetPlatforms.add("server-8.3");
        }
        assertEquals(expectedTargetPlatforms, new HashSet<>(Arrays.asList(packageDef.getTargetPlatforms())));
        Set<String> deps = new HashSet<>();
        for (PackageDependency pd : packageDef.getDependencies()) {
            deps.add(pd.toString());
        }
        Set<String> expectedDependencies = new HashSet<>(
                Arrays.asList("nuxeo-runtime:5.3.1", "nuxeo-core:5.3.1:5.3.2"));
        if (cap) {
            expectedDependencies.add("nuxeo-jsf-ui");
        }
        assertEquals(expectedDependencies, deps);

        // test on real unziped package
        File zip = getTestPackageZip(name);
        File tmpDir = new File(Environment.getDefault().getTemp(), "TestPkg" + System.currentTimeMillis());
        tmpDir.mkdirs();
        ZipUtils.unzip(zip, tmpDir);
        LocalPackage pkg = new LocalPackageImpl(tmpDir, PackageState.REMOTE, service);
        Framework.trackFile(tmpDir, pkg);
        assertEquals("You have to be crazy to use this package", pkg.getTermsAndConditionsContent());
        assertEquals("nuxeo-automation", pkg.getName());
        assertEquals("Nuxeo", pkg.getVendor());
        assertTrue(pkg.requireTermsAndConditionsAcceptance());
        assertTrue(pkg.supportsHotReload());
        assertEquals(expectedTargetPlatforms, new HashSet<>(Arrays.asList(pkg.getTargetPlatforms())));
        deps = new HashSet<>();
        for (PackageDependency pd : pkg.getDependencies()) {
            deps.add(pd.toString());
        }
        assertEquals(expectedDependencies, deps);
    }

    @Test
    public void testBuildAndParse() throws Exception {
        doTestBuildAndParse(false);
    }

    @Test
    public void testBuildAndParseWithCAP() throws Exception {
        doTestBuildAndParse(true);
    }

}
