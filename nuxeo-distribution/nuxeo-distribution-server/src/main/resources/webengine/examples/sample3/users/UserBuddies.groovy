package users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;

/**
 * UserBuddies object.
 * You can see the @WebAdapter annotation that is defining a WebAdapter of type "UserBuddies" that applies to any User WebObject.
 * The name used to access this adapter is the adapter name prefixed with a '@' character: <code>@buddies</code>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name="buddies", type="UserBuddies", targetType="User")
@Produces(["text/html", "*/*"])
public class UserBuddies extends DefaultAdapter {

  /**
   * Get the index view. The view file name is computed as follows: index[-media_type_id].ftl
   * First the skin/views/UserBuddies is searched for that file then the current directory.
   * (The type of a module is the same as its name)
   */
  @GET
  public Object doGet() {
    return getView("index");
  }


}

