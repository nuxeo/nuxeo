package org.nuxeo.webengine.sites;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;

public class SiteTreeSerializer extends JSonTreeSerializer{

    @Override
    public String getUrl(TreeItem item) {
        WebContext ctx = WebEngine.getActiveContext();
        StringBuffer sb = new StringBuffer(ctx.getModulePath());
        DocumentModel d = (DocumentModel)ctx.getUserSession().get(JsonAdapter.ROOT_DOCUMENT);
        if (d != null ) {
            sb.append("/").append(SiteHelper.getString(d, "webc:url", ""));
        }
        sb.append(item.getPath().toString());
        return sb.toString();
    }

}
