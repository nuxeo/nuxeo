package sample4;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;

/**
 * WebEngine Object Model.
 *
 * This sample is explaining the basics of  Nuxeo WebEngine Object Model.
 * <p>
 * 
 * <h3>Resource Model</h3>
 * There are three type of resources defined by WebEngine:
 * <ul>
 * <li> Module Resource - this is the Module entry point as we've seen in sample3.
 * This is the root resource. The other type of resources are JAX-RS sub-resources.
 * <li> Web Object - this represents an object that can be requested via HTTP methods.
 *  This resource is usually wrapping some internal object to expose it as a JAX-RS resource.
 * <li> Web Adapter - this is a special kind of resource that can be used to adapt Web Objects
 * to application specific needs.
 * These adapters are usefull to add new functionalities on Web Objects without breaking application modularity
 * or adding new methods on resources.
 * This is helping in creating extensible applications, in keeping the code cleaner and in focusing better on the REST aproach
 * of the application.
 * For example let say you defined a DocumentObject which will expose docuemnts as JAX-RS resources.
 * A JAX-RS resources will be able to respond to any HTTP method like GET, POST, PUT, DELETE.
 * So let say we use:
 * <ul>
 * <li> <code>GET</code> to get a view on the DocumentObject
 * <li> <code>POST</code> to create a DocumentObject
 * <li> <code>PUT</code> to update a document object
 * <li> <code>DELETE</code> to delete a DocumentObject.
 * </ul>
 * But what if I want to put a lock on the document? Or to query the lock state? or to remove the lock?
 * Or more, to create a document version? or to get a document version?
 * A simple way is to add new methods on the DocumentObject resource that will handle requests top lock, unlock, version etc.
 * Somethig like <code>@GET @Path("lock") getLock()</code> or <code>@POST @Path("lock") postLock()</code>.
 * But this approach is not flexible because you cannot easily add new fonctionalities on existing resources in a dynamic way.
 * And also, doing so, you will end up with a cluttered code, having many methods for each new aspect of the Web Object you need to handle.
 * To solve this problem, WebEngine is defining Web Adapters, so that they can be used to add new fonctionality on existing objects.
 * For example, to handle the lock actions on an Web Object we will define a new class LockAdapter which will implement
 * the <code>GET</code>, <code>POST</code>, <code>DELETE</code> methods to manage the lock fonctionality on the target Web Object.
 * Adapters are specified using an '@' prefix on the segment in an HTTP request path. This is needed by WebEngine to differentiate
 * Web Objects from Web Adapters.
 * Thus in our lock example to request the lock adapter on an object you will use a request path of like the following:
 * <code>GET /my/document/@lock</code> or <code>POST /my/document/@lock</code> etc.
 * <p>
 * When defining a Web Adapter you can specify on which type of Web Object it may be used. (tis is done using annotations)
 * </ul>
 * All WebEngine resources have a type, a super type, an optional set of facets and an optional guard (these are declared using annotations)
 * Using types and super types you can create hierarchies or resources, so that derived resource tyeps will inherit attributes of the super types.
 * <p>
 *
 * There is a builtin adapter that is managing Web Objects views. The adapter name is <code>@views</code>. 
 * You will see in the view model an example on how to use it.
 * <p>
 * 
 * <h3>View Model</h3>
 * The view model is an extension of the template model we discussed in the previous sample.
 * The difference between views and templates is that views are allways attached to an Web Object. Also, the view file resolution is
 * a bit different from template files. Templates are all living in <code>skin</skin> directory. Views may live in two places:
 * <ul>
 * <li> in the skin/views/${type-name} folders where type-name is the type of the resource (WebObject, WebAdapter, WebModule) the view is applying on.
 * This place will be consulted first when a view file is resolved, so it can be used by derived modules to replace views on already defined objects.
 * <li> in the same folder (e.g. java package) as the resource class.
 * This place is useful to defining views inside JARs along with resource classes.
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebModule(name="sample4")
@Path("/sample4")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {

  @GET
  public Object doGet() {
    return getView("index");
  }

}

