/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestBrowse extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestBrowse.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployContrib("org.nuxeo.apidoc.core", "OSGI-INF/snapshot-service-framework.xml");
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    @Test
    public void testBrowse() {

        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(runtimeSnapshot);

        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad = " ";
            for (int i = 0; i <= info.getLevel(); i++) {
                pad = pad + " ";
            }
            log.info(pad + "- " + info.getGroup().getName() + "(" + info.getGroup().getId() + ")");
        }

        for (String bid : runtimeSnapshot.getBundleIds()) {
            log.info("bundle : " + bid);
        }

        for (String cid : runtimeSnapshot.getComponentIds()) {
            log.info("component : " + cid);
            // ComponentInfo ci = runtimeSnapshot.getComponent(cid);
            // log.info(ci.getXmlFileContent());
        }

        for (String sid : runtimeSnapshot.getServiceIds()) {
            log.info("service : " + sid);
        }

        for (Class<?> spi : runtimeSnapshot.getSpi()) {
            log.info("SPI : " + spi.getCanonicalName());
        }

        for (String epid : runtimeSnapshot.getExtensionPointIds()) {
            log.info("extensionPoint : " + epid);
            // log.info(ci.getXmlFileContent());
        }
    }

    protected void dumpBundleGroup(BundleGroup bGroup, int level) {

        String pad = " ";
        for (int i = 0; i <= level; i++) {
            pad = pad + " ";
        }

        log.info(pad + "- " + bGroup.getName() + "(" + bGroup.getId() + ")");

        for (BundleGroup subGroup : bGroup.getSubGroups()) {
            dumpBundleGroup(subGroup, level + 1);
        }

        for (String bundle : bGroup.getBundleIds()) {
            log.info(pad + "  - bundle : " + bundle);
        }
    }

    @Test
    public void testIntrospection() {

        String cid = "org.nuxeo.ecm.core.lifecycle.LifeCycleService";
        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        ComponentInfo ci = runtimeSnapshot.getComponent(cid);
        assertNotNull(ci);

        assertEquals(2, ci.getExtensionPoints().size());

        for (ExtensionPointInfo epi : ci.getExtensionPoints()) {
            log.info(epi.getId());
        }

        String epid = "org.nuxeo.ecm.core.lifecycle.LifeCycleService--types";

        ExtensionPointInfo epi = runtimeSnapshot.getExtensionPoint(epid);
        assertNotNull(epi);

        Collection<ExtensionInfo> contribs = epi.getExtensions();
        assertFalse(contribs.isEmpty());
    }

}
