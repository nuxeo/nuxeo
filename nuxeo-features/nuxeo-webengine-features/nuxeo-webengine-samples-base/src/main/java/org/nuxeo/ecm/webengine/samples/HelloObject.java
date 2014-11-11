package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * This is a very simple resource example, that prints the "Hello World!" message.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "Hello")
@Produces("text/html;charset=UTF-8")
public class HelloObject extends DefaultObject {

    @GET
    public String doGet() {
        return "Hello World!";
    }

    @GET
    @Path("{name}")
    public String doGet(@PathParam("name") String name) {
        return "Hello " + name + "!";
    }

}

