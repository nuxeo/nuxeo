package wiki;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.core.api.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;
import org.nuxeo.ecm.core.rest.*;

@WebObject(type="Wiki")
@Produces(["text/html", "*/*"])
public class WikiObject extends DocumentObject {

  public void initialize(Object... args) {
    super.initialize(args);
    setRoot(true);
  }

  @GET
  public Response doGet() {
    return redirect(path+"/FrontPage");
  } 

  @GET
  @Path("create/{segment}")
  public Response createPage(@PathParam("segment") String segment) {
    def session = ctx.getCoreSession();
    def newDoc = session.createDocumentModel(doc.getPathAsString(), segment, "WikiPage");
    if (newDoc.getTitle().length() == 0) {
      newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
    }
    newDoc = session.createDocument(newDoc);
    session.save();
    return redirect(path+"/"+segment);
  }

}

