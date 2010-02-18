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

import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestBrowse extends NXRuntimeTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
    }

    public void testBrowse() throws Exception  {;

        DistributionSnapshot runtimeSnapshot = SnapshotManager.getRuntimeSnapshot();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(runtimeSnapshot);

        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad =" ";
            for (int i = 0 ; i<=info.getLevel(); i++) {
                pad = pad+ " ";
            }
            System.out.println(pad + "- " + info.getGroup().getName() + "("+ info.getGroup().getKey()+")");
        }

        for (String bid : runtimeSnapshot.getBundleIds()) {
            System.out.println("bundle : " + bid);
        }

        for (String cid : runtimeSnapshot.getComponentIds()) {
            System.out.println("component : " + cid);
            ComponentInfo ci = runtimeSnapshot.getComponent(cid);
            //System.out.println(ci.getXmlFileContent());
        }

        for (String sid : runtimeSnapshot.getServiceIds()) {
            System.out.println("service : " + sid);
        }

        for (Class spi : runtimeSnapshot.getSpi()) {
            System.out.println("SPI : " + spi.getCanonicalName());
        }

        for (String epid : runtimeSnapshot.getExtensionPointIds()) {
            System.out.println("extensionPoint : " + epid);
            //System.out.println(ci.getXmlFileContent());
        }

    }

    protected void dumpBundleGroup(BundleGroup bGroup, int level) {

        String pad =" ";
        for (int i = 0 ; i<=level; i++) {
            pad = pad+ " ";
        }

        System.out.println(pad + "- " + bGroup.getName() + "("+ bGroup.getKey()+")");

        for (BundleGroup subGroup : bGroup.getSubGroups()) {
            dumpBundleGroup(subGroup, level+1);
        }

        for (String bundle : bGroup.getBundleIds()) {
            System.out.println( pad + "  - bundle : " + bundle);
        }
    }
}
