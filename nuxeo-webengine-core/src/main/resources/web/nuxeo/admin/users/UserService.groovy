package nuxeo.admin.users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebAdapter(name="users", targetType="Admin")
@Produces(["text/html", "*/*"])
public class UserService extends DefaultAdapter {

  @GET @POST
  public Object getIndex(@QueryParam("query") String query, @QueryParam("group") String group) {
    if (query != null && query != "") {
      def userManager = Framework.getService(UserManager.class)
      def results = null;
      if (group != null) {
        results = userManager.searchGroups(query)
        return getView("index.ftl").arg("groups", results);
      } else {
        results = userManager.searchPrincipals(query)
        return getView("index.ftl").arg("users", results);
      }
    }
    return getView("index.ftl");
  }


  @Path("user/{user}")
  public Object searchUsers(@PathParam("user") String user) {
    def userManager = Framework.getService(UserManager.class)
    def principal = userManager.getPrincipal(user);
    if (principal == null) {
      throw new WebResourceNotFoundException("User not found: "+user);
    }
    return newObject("User", principal);
  }

  @Path("group/{group}")
  public Object searchGroups(@PathParam("group") String group) {
    def userManager = Framework.getService(UserManager.class)
    def principal = userManager.getGroup(group);
    if (principal == null) {
      throw new WebResourceNotFoundException("User not found: "+user);
    }
    return newObject("Group", principal);
  }

}

