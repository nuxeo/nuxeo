package org.nuxeo.webengine.sites;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type="sites", guard="user=Administrator")
@Produces({"text/html; charset=UTF-8", "*/*; charset=UTF-8"})
public class Sites extends DefaultObject {

    @GET
    public Object doGet() {
        return dispatch("/");
    }

    @Path("{modulePath}")
    public Object dispatch(@PathParam("modulePath") String path) {
        System.out.println("try to get site with path:" + path);
        if ( "/".equals(path) ){
            try {
                List<Object> sites = getWebContainers();
                return getTemplate("list_sites.ftl").arg("sites", sites);
            } catch (ClientException e){
                e.printStackTrace();
            }
        } else {
            return newObject("site", path);
        }
        return null;

    }

    public  List<Object> getWebContainers() throws ClientException{
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();

//        DocumentModelList list  = session.query("SELECT * FROM Workspace WHERE webc:isWebContainer = true");
        DocumentModelList list  = session.query("SELECT * FROM Workspace");
        List<Object> sites = new ArrayList<Object>();

        for ( DocumentModel d : list ){
            try {
                Map<String , String> site = new HashMap<String, String>();
                site.put("href", getValue(d, "webc:url"));
                site.put("name", getValue(d, "webc:name"));
                sites.add(site);
            } catch (Exception e){
                System.out.println("ignore site :" + d);
            }
        }
        return sites;
    }

    protected String getValue(DocumentModel d, String xpath) throws ClientException{
        Property p = d.getProperty(xpath);
        if ( p != null) {
            Serializable v = p.getValue();
            if ( v != null ) {
                return v.toString();
            }
        }
        return "";
    }


}
