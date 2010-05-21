/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.apidoc.test;

import java.util.Collections;
import java.util.List;

import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestSnapshotPersist extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.apidoc.core");
        openSession();
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    protected String dumpSnapshot(DistributionSnapshot snap) {

        StringBuffer sb = new StringBuffer();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(snap);

        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad =" ";
            for (int i = 0 ; i<=info.getLevel(); i++) {
                pad = pad+ " ";
            }
            sb.append(pad + "- " + info.getGroup().getName() + "("+ info.getGroup().getId()+")");
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
            sb.append("\n");
        }

        for (String cid : cids) {
            sb.append("component : " + cid);
            sb.append("\n");
            ComponentInfo ci = snap.getComponent(cid);
        }

        for (String sid : sids) {
            sb.append("service : " + sid);
            sb.append("\n");
        }

        for (String epid : epids) {
            sb.append("extensionPoint : " + epid);
            sb.append("\n");
        }

        for (String exid : exids) {
            sb.append("contribution : " + exid);
            sb.append("\n");
        }

        return sb.toString();
    }

    public void testPersist() throws Exception {

        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        String rtDump = dumpSnapshot(runtimeSnapshot);
        System.out.println("Live Dump:");
        System.out.println(rtDump);

        DistributionSnapshot persistent = getSnapshotManager().persistRuntimeSnapshot(session);
        assertNotNull(persistent);

        persistent = getSnapshotManager().getSnapshot(runtimeSnapshot.getKey(), session);
        assertNotNull(persistent);
        session.save();

        /*DocumentModelList docs = session.query("select * from NXBundle");
        for (DocumentModel doc : docs) {
            System.out.println("Bundle : " + doc.getTitle() + " --- " + doc.getPathAsString());
        }*/

        String pDump = dumpSnapshot(persistent);
        System.out.println("Persisted Dump:");
        System.out.println(pDump);

        assertEquals(rtDump, pDump);


    }

}
