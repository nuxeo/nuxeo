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
package org.nuxeo.apidoc.tree;

import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTree;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeModelImpl;

public class NuxeoArtifactTree extends JSonTree {

    protected DistributionSnapshot ds;

    public NuxeoArtifactTree(WebContext ctx, DistributionSnapshot ds) {
        tree = new TreeModelImpl();
        this.ds = ds;
        tree.setContentProvider(getProvider(ctx));
        tree.setInput(ds);
    }

    public void setDs(DistributionSnapshot ds) {
        this.ds = ds;
        tree.setContentProvider(new NuxeoArtifactContentProvider(ds));
    }

    @Override
    protected Object getInput(WebContext ctx) {
        return ds;
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        return new NuxeoArtifactContentProvider(ds);
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new NuxeoArtifactSerializer(ctx, ds);
    }

}
