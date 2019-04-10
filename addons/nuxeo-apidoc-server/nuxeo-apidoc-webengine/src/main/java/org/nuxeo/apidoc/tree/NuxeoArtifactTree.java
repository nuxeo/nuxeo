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
