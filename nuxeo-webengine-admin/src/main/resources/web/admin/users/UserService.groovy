package admin.users;

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

@WebObject(type="UserManager")
@Produces(["text/html", "*/*"])
public class UserService extends DefaultObject {

  @GET @POST
  public Object getIndex(@QueryParam("query") String query, @QueryParam("group") String group) {
    if (query != null && query != "") {
      def userManager = Framework.getService(UserManager.class)
      def results = null;
      if (group != null) {
        results = userManager.searchGroups(query)
        return getView("index").arg("groups", results);
      } else {
        results = userManager.searchPrincipals(query)
        return getView("index").arg("users", results);
      }
    }
    return getView("index");
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

  @POST
  @Path("user")
  public Response postUser() {
    def req = ctx.getRequest();
    def username = req.getParameter("username");
    def userManager = Framework.getService(UserManager.class);
    def user;
    if (username) {
        user = userManager.getPrincipal(username)
        if (user) {
            // update
            user.firstName = req.getParameter("firstName")
            user.lastName = req.getParameter("lastName")
            user.password = req.getParameter("password")
            
            def selectedGroups = req.getParameterValues("groups")
            def listGroups = Arrays.asList(selectedGroups)
            user.setGroups(listGroups)
            
            userManager.updatePrincipal(user)
        } else {
            // create
            user = new NuxeoPrincipalImpl(req.getParameter("username"))
            user.firstName = req.getParameter("firstName")
            user.lastName = req.getParameter("lastName")
            user.password = req.getParameter("password")
            
            def selectedGroups = req.getParameterValues("groups")
            def listGroups = Arrays.asList(selectedGroups)
            user.setGroups(listGroups)
            
            userManager.createPrincipal(user)
        }
    }

    return redirect(getPath()+"/user/"+user.name);
  }

  @POST
  @Path("group")
  public Response postGroup() {
    def groupName = ctx.getRequest().getParameter("groupName");
    def userManager = Framework.getService(UserManager.class)
    if (groupName) {
        def group = new NuxeoGroupImpl(groupName)
        userManager.createGroup(group)
        return redirect(getPath()+"/group/"+group.name);
    }
  }


  public List getGroups() {
    return Framework.getService(UserManager.class).getAvailableGroups();
  }

  public List getUsers() {
    return Framework.getService(UserManager.class).getAvailablePrincipals();
  }


}

