package org.nuxeo.apidoc.tree;

import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTree;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeModelImpl;

public class NuxeoArtifactTree extends JSonTree {

    DistributionSnapshot ds;

    public NuxeoArtifactTree(WebContext ctx, DistributionSnapshot ds) {
        tree = new TreeModelImpl();
        this.ds=ds;
        tree.setContentProvider(getProvider(ctx));
        tree.setInput(ds);
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
