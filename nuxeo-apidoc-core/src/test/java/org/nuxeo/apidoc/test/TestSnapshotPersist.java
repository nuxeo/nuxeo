/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestSnapshotPersist extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestSnapshotPersist.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.ecm.automation.core");

        deployBundle("org.nuxeo.apidoc.core");
        openSession();
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    protected String dumpSnapshot(DistributionSnapshot snap) {

        StringBuilder sb = new StringBuilder();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(snap);

        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad = " ";
            for (int i = 0; i <= info.getLevel(); i++) {
                pad += " ";
            }
            sb.append(pad + "- " + info.getGroup().getName() + "("
                    + info.getGroup().getId() + ")");
            sb.append(" *** ");
            sb.append(info.getGroup().getHierarchyPath());
            sb.append("\n");
        }

        List<String> bids = snap.getBundleIds();
        List<String> cids = snap.getComponentIds();
        List<String> sids = snap.getServiceIds();
        List<String> epids = snap.getExtensionPointIds();
        List<String> exids = snap.getContributionIds();

        Collections.sort(bids);
        Collections.sort(cids);
        Collections.sort(sids);
        Collections.sort(epids);
        Collections.sort(exids);

        for (String bid : bids) {
            sb.append("bundle : " + bid);
            BundleInfo bi = snap.getBundle(bid);
            sb.append(" *** ");
            sb.append(bi.getHierarchyPath());
            sb.append("\n");
        }

        for (String cid : cids) {
            sb.append("component : " + cid);
            sb.append(" *** ");
            ComponentInfo ci = snap.getComponent(cid);
            sb.append(ci.getHierarchyPath());
            sb.append("\n");
        }

        for (String sid : sids) {
            sb.append("service : " + sid);
            sb.append(" *** ");
            ServiceInfo si = snap.getService(sid);
            sb.append(si.getHierarchyPath());
            sb.append("\n");
        }

        for (String epid : epids) {
            sb.append("extensionPoint : " + epid);
            sb.append(" *** ");
            ExtensionPointInfo epi = snap.getExtensionPoint(epid);
            sb.append(epi.getHierarchyPath());
            sb.append("\n");

        }

        for (String exid : exids) {
            sb.append("contribution : " + exid);
            sb.append(" *** ");
            ExtensionInfo exi = snap.getContribution(exid);
            sb.append(exi.getHierarchyPath());
            sb.append("\n");
        }

        return sb.toString();
    }

    @Test
    public void testPersist() throws Exception {

        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        String rtDump = dumpSnapshot(runtimeSnapshot);
        log.info("Live Dump:");
        log.info(rtDump);

        DistributionSnapshot persistent = getSnapshotManager().persistRuntimeSnapshot(
                session);
        assertNotNull(persistent);

        session.save();

        persistent = getSnapshotManager().getSnapshot(runtimeSnapshot.getKey(),
                session);
        assertNotNull(persistent);
        session.save();

        /*
         * DocumentModelList docs = session.query("select * from NXBundle"); for
         * (DocumentModel doc : docs) { log.info("Bundle : " + doc.getTitle() +
         * " --- " + doc.getPathAsString()); }
         */

        String pDump = dumpSnapshot(persistent);
        log.info("Persisted Dump:");
        log.info(pDump);

        /*
         * 
         * String[] rtDumpLines = rtDump.trim().split("\n"); String[] pDumpLines
         * = pDump.trim().split("\n");
         * 
         * assertEquals(rtDumpLines.length, pDumpLines.length);
         * 
         * for (int i = 0; i < rtDumpLines.length; i++) {
         * assertEquals(rtDumpLines[i], pDumpLines[i]); }
         */

        assertEquals(rtDump, pDump);
    }

    @After
    public void tearDown() throws Exception {
        if (session != null) {
            CoreInstance.getInstance().close(session);
        }
        super.tearDown();
    }

}
