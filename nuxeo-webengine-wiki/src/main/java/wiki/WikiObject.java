package wiki;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "Wiki")
@Produces("text/html; charset=UTF-8")
public class WikiObject extends DocumentObject {

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        setRoot(true);
    }

    @Override
    @GET
    public Response doGet() {
        return redirect(path + "/FrontPage");
    }

    @GET
    @Path("create/{segment}")
    public Response createPage(@PathParam("segment") String segment)
            throws ClientException {
        CoreSession session = ctx.getCoreSession();
        DocumentModel newDoc = session.createDocumentModel(doc.getPathAsString(), segment, "WikiPage");
        if (newDoc.getTitle().length() == 0) {
            newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
        }
        newDoc = session.createDocument(newDoc);
        session.save();
        return redirect(path + "/" + segment);
    }

}
