package wiki;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.core.api.*;

@WebModule(name="wiki")
@Path("/")
@Produces(["text/html", "*/*"])

public class Main extends DefaultModule {

    public DocumentObject newDocumentObject(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
            DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
            return (DocumentObject)(ctx.newObject(doc.getType(), doc));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
    
    @Path("{segment}")
    public DocumentObject getWiki(@PathParam("segment") String segment) {
      return new DocumentRoot(ctx, "/default-domain/workspaces/wikis/"+segment);
    }
    
  @GET
  public Object getIndex() {
    return getView("index.ftl");
  }  
  
  
}

