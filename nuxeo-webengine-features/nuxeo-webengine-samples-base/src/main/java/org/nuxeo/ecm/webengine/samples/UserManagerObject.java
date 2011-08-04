package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * UserManager object.
 * You can see the @WebObject annotation that defines a WebObject of type "UserManager"
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="UserManager")
@Produces("text/html;charset=UTF-8")
public class UserManagerObject extends DefaultObject {

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
    public Object getUserByQueryString(@QueryParam("name") String name) {
        if (name == null) {
            return doGet();
        } else {
            return redirect(getPath() + "/user/" + name);
        }
    }

    /**
     * Get the user JAX-RS resource given the user name
     */
    @Path("user/{name}")
    public Object getUser(@PathParam("name") String name) {
        // create a new instance of a WebObject which type is "User" and push this object on the request chain
        // the User object is initialized with the String "Username: name"
        return newObject("User", "Username: " + name);
    }

}

