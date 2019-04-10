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
package org.nuxeo.apidoc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

public class BundleGroupTreeHelper {

    protected final DistributionSnapshot distrib;

    public BundleGroupTreeHelper(DistributionSnapshot distrib) {
        this.distrib = distrib;
    }

    protected void browseBundleGroup(BundleGroup group, int level,
            List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);
        List<BundleGroup> subGroups = group.getSubGroups();
        Collections.sort(subGroups, new NuxeoArtifactComparator());
        for (BundleGroup subGroup : subGroups) {
            browseBundleGroup(subGroup, level + 1, tree);
        }
    }

    public List<BundleGroupFlatTree> getBundleGroupSubTree(String groupId) {
        BundleGroup group = distrib.getBundleGroup(groupId);
        List<BundleGroupFlatTree> tree = new ArrayList<BundleGroupFlatTree>();
        browseBundleGroup(group, 0, tree);
        return tree;
    }

    public List<BundleGroupFlatTree> getBundleGroupTree() {
        List<BundleGroupFlatTree> tree = new ArrayList<BundleGroupFlatTree>();

        List<BundleGroup> bgroups = distrib.getBundleGroups();
        Collections.sort(bgroups, new NuxeoArtifactComparator());
        for (BundleGroup group : bgroups) {
            browseBundleGroup(group, 0, tree);
        }
        return tree;
    }

}
