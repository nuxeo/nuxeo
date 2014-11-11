package org.nuxeo.webengine.sites;

import net.sf.json.JSONArray;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentTree;

public class SiteDocumentTree extends DocumentTree{

    public SiteDocumentTree(WebContext ctx, DocumentModel rootDoc) {
        super(ctx, rootDoc);
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new SiteTreeSerializer();
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        return new SiteContentProvider(ctx.getCoreSession());
    }

    protected String enter(WebContext ctx, String path, JSonTreeSerializer serializer) {
        TreeItem item = tree.findAndReveal(path);
        if (item != null) {
            item.expand();
            if ( !item.hasChildren()){
                item.collapse();
            }
            JSONArray result = new JSONArray();
            if (item.isContainer()) {
                result = serializer.toJSON(item.getChildren());
            }
            return result.toString();
        } else {
            return null;
        }
    }

}
