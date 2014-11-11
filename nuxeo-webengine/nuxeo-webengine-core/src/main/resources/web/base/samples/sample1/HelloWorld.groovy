package sample1;

import javax.ws.rs.*;

/**
 * Hello World example.
 * <p>
 * This demonstrates how to declare JAX-RS resources using a web module.
 * The resource is declared in module.xml file as following:
 * <pre>
 *   <resources>
 *     <resource class="sample1.HelloWorld"/>
 *   </resources>
 * </pre>
 * The resource class is a regular JAX-RS resource annotated with @Path that specifies the path information
 * to be used to access this resource in the context of WebEngine servlet.
 * <p>
 * This class provides two examples on how to use @GET annotation method to handle GET requests.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("/sample1")
@Produces(["text/html", "*/*"])
public class HelloWorld {

  @GET
  public String doGet() {
    return "Hello World!";
  }

  @GET
  @Path("{name}")
  public String doGet(@PathParam("name") String name) {
    return "Hello "+name+"!";
  }
  
}
