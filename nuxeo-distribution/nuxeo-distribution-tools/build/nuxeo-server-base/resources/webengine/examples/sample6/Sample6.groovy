
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;

/**
 * Managing links.
 * <p>
 * Almost any template page will contain links to other pages in your application.
 * These links are usually absolute paths to other WebObjects or WebAdapters (including parameters if any).
 * Maintaining these links when application object changes is painful when you are using modular applications
 * (that may contribute new views or templates).
 * <p>
 * WebEngine is providing a flexible way to ease link management.
 * First, you should define all of your links in <i>module.xml</i> configuration file.
 * A Link is described by a target URL, an enablement condition, and one or more categories that can be used to organize links.
 * <ul>
 * Here are the possible conditions that you can use on links:
 * <li> type - represent the target Web Object type. If present the link will be enabled only in the context of such an object.
 * <li> adapter - represent the target Web Adapter name. If present the link will be enabled only if the active adapter is the same as this one.
 * <li> facet - a set of facets that the target web object must have in order to enable the link.
 * <li> guard - a guard to be tested in order to enable the link. This is using the guard mechanism of WebEngine.
 * </ul>
 * If several conditions are specified an <code>AND</code> will be used between them.
 * <p>
 * Apart conditions you can <i>group</i> links in categories.
 * Using categories and conditions you can quickly find in a template which are all enabled links that are part of a category.
 * This way, you can control which links are written in the template without needing to do conditional code to check the context if links are enabled.
 * <p>
 * Conditions and categories manage thus where and when your links are displayed in a page. Apart this you also want to have a target URL for each link.
 * <ul>
 * You have two choices in specifying such a target URL:
 * <li> define a custom link handler using the <code>handler</handler> link attribute.
 * The handler will be invoked each time the link code need to be written in the output stream so that it can programatically generate the link code.
 * <li> use the builtin link handler. The builtin link handler will append the <code>path</code> attribute you specified in link definition
 * to the current WebObject path on the request. This behavior is good enough for most of the use cases.
 * <li>
 * </ul>
 * <p>
 * <p>
 * This example will demonstrate how links work. Look into <code>module.xml</code> for link definitions
 * and then in <code>skin/views/Document/index.ftl</code> on how they are used in the template.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="sample6")
@Produces(["text/html"])
public class Sample6 extends ModuleRoot {

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
