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

package org.nuxeo.apidoc.api;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class BundleGroupTreeHelper {

    protected final DistributionSnapshot distrib;

    public BundleGroupTreeHelper(DistributionSnapshot distrib) {
        this.distrib = distrib;
    }

    protected void browseBundleGroup(BundleGroup group, int level, List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);

        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level+1, tree);
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
        for (BundleGroup group : distrib.getBundleGroups()) {
            browseBundleGroup(group, 0, tree);
        }
        return tree;
    }

}
