package sample1;

import javax.ws.rs.*;

/**
 * Singleton Hello World example.
 * <p>
 * This demonstrates how to declare JAX-RS resources without using explicit @Path annotations.
 * The resource, and the path it is bound to, is declared in module.xml file as following:
 * <pre>
 *   <resources>
 *     <resource class="sample1.HelloWorldSingleton" path="/sample1/singleton" singleton="true"/>
 *   </resources>
 * </pre>
 * This is an Nuxeo extension to JAX-RS.
 * The resource class is like a regular JAX-RS resource, but without the @Path annotation.
 * Note that this resource is a singleton - it will be instantiated only once at registration!
 * This means the resource instance may be concurrently accessed
 * within different threads.
 * <p>
 * You can also use per request instances by removing the singleton="true" attribute.
 * <p>
 * This class provides two examples on how to use @GET annotation method to handle GET requests.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Produces(["text/html", "*/*"])
public class HelloWorldSingleton {

  protected volatile String lastHello;

  @GET
  public String doGet() {
    return "Hello World!";
  }

  @GET
  @Path("{name}")
  public String doGet(@PathParam("name") String name) {
    String response = null;
    if (lastHello == null) {
      response = "Hello "+name+"!";
    } else {
      response = "Was Hello "+lastHello+"! Now is Hello "+name+"!";
    }
    lastHello = name;
    return response;
  }
  
}
