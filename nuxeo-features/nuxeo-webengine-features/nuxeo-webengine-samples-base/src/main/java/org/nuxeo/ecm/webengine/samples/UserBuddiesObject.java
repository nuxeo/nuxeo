package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * UserBuddies object.
 * You can see the @WebAdapter annotation that is defining a WebAdapter of type "UserBuddies" that applies to any User WebObject.
 * The name used to access this adapter is the adapter name prefixed with a '@' character: {@code @buddies}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name="buddies", type="UserBuddies", targetType="User")
@Produces("text/html;charset=UTF-8")
public class UserBuddiesObject extends DefaultAdapter {

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

