package org.nuxeo.webengine.sites;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;


@WebObject(type="site", guard="user=Administrator")
@Produces({"text/html; charset=UTF-8", "*/*; charset=UTF-8"})
public class Site extends DefaultObject{
    String url;

    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        url = (String) args[0];
    }

    @GET
    public Object doGet(){
        System.out.println("hello");
        DocumentModel ws = getWorkspaceByUrl(url);
        if ( ws == null) {
            return getTemplate("no_site.ftl").arg("url", url);
        }
        return null;
    }

    protected DocumentModel getWorkspaceByUrl(String url){
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            DocumentModelList list  = session.query(String.format("SELECT * FROM Workspace WHERE web.url='%s'", url));
            if ( list.size() != 0 ){
                return list.get(0);
            }
        } catch (ClientException e){
            // nothing to do
        }

        return null;
    }

}
