package org.nuxeo.webengine.sites;

import java.util.Vector;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentContentProvider;

public class SiteContentProvider extends DocumentContentProvider {

    private static final long serialVersionUID = 1L;

    public SiteContentProvider(CoreSession session) {
        super(session);
    }



    @Override
    public Object[] getChildren(Object obj) {
        Object[] objects = super.getChildren(obj);
        Vector<Object> v= new Vector<Object>();
        for( Object o : objects){
            DocumentModel d = (DocumentModel) o;
            // filter pages
            if ( SiteHelper.getBoolean(d, "webp:pushtomenu", false)){
                v.add(d);
            }
        }
        return v.toArray();
    }

}
