package orb.nuxeo.ecm.opensocial.mydocs.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "JSONDocument")
public class JSONDocument extends DocumentObject {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    @GET
    @Produces("text/html; charset=UTF-8")
    public Object doGet() {
        Map<String, Object> all = new HashMap<String, Object>();
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("id", getDocument().getId());
        try {
            summary.put("title", getDocument().getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;

        all.put("summary", summary);

        CoreSession session = ctx.getCoreSession();
        try {
            DocumentModelList children = session.getChildren(getDocument()
                    .getRef());
            List<Object> docs = new ArrayList<Object>();

            for (DocumentModel child : children) {
                if(child.getType() != "Space") {
                    Map<String, Object> docItem = new HashMap<String, Object>();
                    docItem.put("id", child.getId());
                    docItem.put("name", child.getName());
                    docItem.put("url", getDocumentURL(child));
                    docItem.put("icon", child.getPropertyValue("common:icon"));
                    docItem.put("title", child.getTitle());
                    docItem.put("creator", child
                            .getPropertyValue("dublincore:creator"));
                    docItem.put("modified", DATE_FORMAT
                            .format(((GregorianCalendar) child
                                    .getPropertyValue("dublincore:modified"))
                                    .getTime()));
                    if (child.hasFacet("Folderish")) {
                        docItem.put("folderish", "1");
                    } else {
                        docItem.put("folderish", "0");
                    }
                    docs.add(docItem);
                }
            }
            all.put("document", docs);
        } catch (ClientException e) {
            e.printStackTrace();
        }

        return makeJSON(all);
    }

    @Path(value = "{path}")
    public Resource traverse(@PathParam("path") String path) {
        return newDocument(path);
    }

    public DocumentObject newDocument(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
            DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
            return (DocumentObject) newObject("JSONDocument", doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    protected static String makeJSON(Map<String, Object> all) {
        JSON jsonRes = JSONSerializer.toJSON(all);
        if (jsonRes instanceof JSONObject) {
            JSONObject jsonOb = (JSONObject) jsonRes;
            return jsonOb.toString(2);
        } else if (jsonRes instanceof JSONArray) {
            JSONArray jsonOb = (JSONArray) jsonRes;
            return jsonOb.toString(2);
        } else {
            return null;
        }
    }

    protected static String getDocumentURL(DocumentModel doc) {
        DocumentViewCodecManager dvcm;
        try {
            dvcm = Framework.getService(DocumentViewCodecManager.class);
        } catch (Exception e) {
            return null;
        }
        return dvcm.getUrlFromDocumentView(new DocumentViewImpl(doc),
                false, null);
    }


}
