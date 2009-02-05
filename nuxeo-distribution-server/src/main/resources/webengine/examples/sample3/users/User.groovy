package users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;

/**
 * User object.
 * You can see the @WebObject annotation that is defining a WebObject of type "User"
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="User")
@Produces(["text/html", "*/*"])
public class User extends DefaultObject {

  /**
   * Get the index view of the User object.
   * The view file is located in <code>skin/views/User</code> so that it can be easily extended
   * by a derived module. See extensibility sample.
   */
  @GET
  public Object doGet() {
    return getView("index");
  }

  /**
   * This method is not implemented but demonstrates how DELETE requests can be used
   */
  @DELETE
  public Object doRemove(@PathParam("name") String name) {
    //TODO ... remove user here ...
    // redirect to the UserManager (the previous object in the request chain)
    return redirect(getPrevious().getPath());
  }

  /**
   * This method is not implemented but demonstrates how PUT requests can be used
   */
  @PUT
  public Object doPut(@PathParam("name") String name) {
    //TODO ... update user here ...
    // redirect to myself
    return redirect(getPath());
  }

}

