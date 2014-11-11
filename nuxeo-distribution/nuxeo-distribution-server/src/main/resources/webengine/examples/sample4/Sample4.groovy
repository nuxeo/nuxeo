
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;

/**
 * Working with Nuxeo Documents.
 *
 * Nuxeo Documents are transparently mapped to WebObjects so that you can easily access your documents
 * through WebEngine.
 * Nuxeo Documents are defined by a document type and can be structured in a hierarchy based on their type.
 * The ancestor of all document types is the "Document" type.
 * Document types are transparently mapped to WebObject types, so that you don't need to explicitely declare
 * WebObjects that expose documents. By default all documents are exposed as DocumentObject instances (which is an WebObject).
 * If you need specific control over your document type you need then to explicitely declare a new WebObject using the same
 * type name as your document type. This way, the default binding to DocumentObject will be replaced with your own WebObject.
 * <p>
 * <b>Note</b> that it is recommended to subclass the DocumentObject when redefining document WebObjects.
 * <p>
 * Also, Documents as WebObjects may have a set of facets. Documents facets are transparently exposed as WebObject facets.
 * When redefining the WebObject used to expose a Document you can add new facets using @WebObject annotation
 * (these new facets that are not existing at document level but only at WebObject level).
 * <p>
 * To work with documents you need first to get a view on the repository. This can be done using the following methods:
 * <br>
 * <code>DocumentFactory.getDocumentRoot(ctx, path)</code> or <code>DocumentFactory.getDocument(ctx, path)</code>
 * <br>
 * The difference between the two methods is that the getDocumentRoot is also setting
 * the newly created document WebObject as the root of the request chain.
 * The document WebObject created using the DocumentFactory helper class will represent the root of your repository view.
 * To go deeper in the repository tree you can use the <code>newDocument</code> methods on the DocumentObject instance.
 * <p>
 * <b>Remember</b> that when working with documents you may need to log in to be able to access the repository.
 * (it depends on whether or not the repository root is accessible to Anonymous user)
 * For this reason we provide in this example a way to login into the repository.
 * This also demonstrates <b>how to handle errors</b> in WebEngine. The mechanism is simple:
 * At your module resource level you redefine a method
 * <code>public Object handleError(WebApplicationException e)</code> that will be invoked each time
 * an uncatched exception is thrown during the request. From that method you should return a suitable response to render the error.
 * To ensure exceptions are correclty redirected to your error handler you must catch all exceptions thrown in your resource methods
 * and rethrowing them as following: <code> ... } catch (Throwable t) { throw WebException.wrap(t); } </code>.
 * The exception wrapping is automatically converting exceptions to the ones defined by WebEngine model.
 * <p>
 * The default exception handling defined in ModuleRoot class is simply printing the exception on the output stream.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="sample4")
@Produces(["text/html"])
public class Sample4 extends ModuleRoot {

  @GET
  public Object doGet() {
    return getView("index");
  }

  /**
   * Get a repository view rooted under "/default-domain".
   */
  @Path("repository")
  public Object getRepositoryView() {
    return DocumentFactory.newDocumentRoot(ctx, "/default-domain");
  }


  /**
   * Example on how to handle errors
   */
  public Response handleError(WebApplicationException e) {
    if (e instanceof WebSecurityException) {
      // display a login page
      return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
    } else if (e instanceof WebResourceNotFoundException) {
      return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
    } else {
      // not interested in that exception - use default handling
      return super.handleError(e);
    }
  }

}

