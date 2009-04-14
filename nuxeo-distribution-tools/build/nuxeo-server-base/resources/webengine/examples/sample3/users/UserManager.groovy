package users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;

/**
 * UserManager object.
 * You can see the @WebObject annotation that is defining a WebObject of type "UserManager"
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="UserManager")
@Produces(["text/html", "*/*"])
public class UserManager extends DefaultObject {

  /**
   * Get the index view. The view file name is computed as follows: index[-media_type_id].ftl
   * First the skin/views/UserManager is searched for that file then the current directory.
   * (The type of a module is the same as its name)
   */
  @GET
  public Object doGet() {
    return getView("index");
  }

  /**
   * A hack to accept users as user?name=xxx query parameters
   */
  @GET
  @Path("user")
  public Response getUserByQueryString(@QueryParam("name") String name) {
    if (name == null) {
      return Response.status(404).build();
    } else {
      return redirect(getPath()+"/user/"+name);
    }
  }

  /**
   *
   */
  @Path("user/{name}")
  public Object getUser(@PathParam("name") String name) {
    // create a new instance of a WebObject which type is "User" and push this object on the request chain
    // the User object is intialized with the user name
    return newObject("User", name);
  }

  /**
   * This method is not implemented but demonstrates how POST requests can be used
   */
  @POST
  @Path("user/{name}")
  public Object createUser(@PathParam("name") String name) {
    //TODO ... create user here ...
    return redirect(getPath()+"/user/"+name); // redirect to the newly created user
  }


}

