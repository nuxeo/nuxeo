
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;

/**
 * Web Module Extensibility.
 *
 * This sample is demonstrating how existing web modules can be extended.
 * To extend another module you should use the <code>base="BaseModule"</code> in the <code>@WebModule</code>
 * annotation. This way the new module will inherit all templates and resources defined in the base module.
 * You can thus create a chain of inherited web modules.
 * <p>
 * Here is how template resolval will be impacted by the module inheritance:
 * <br>
 * <i>If a template T is not found in skin directory of derived module then search the template inside the base module and so on
 * until a template is found or no more base module exists.</i>
 * The view resolval is similar to the template one but it will use the WebObject inheritance too:
 * <br>
 * <i></i>
 * <br>
 * <b>Note</b> that only the <i>skin</i> directory is stacked over the one in the base module.
 * The other directories in the module are not inheritable.
 * <p>
 * Also, resource types defined by the base module will become visible in the derived one.
 * <p>
 * In this example you will also find a very useful feature of WebEngine: the builtin <b>view service adapter</b>.
 * This adapter can be used on any web object to locate any view declared on that object.
 * Let's say we define a view named <i>info</i> for the <i>Document</i> WebObject type.
 * And the following request path will point to a Document WebObject: <code>/my/doc</code>.
 * Then to display the <i>info</i> view we can use the builtin views adapter this way:
 * <code>/my/doc/@views/info</code>.
 * <p>
 * Obviously, you can redefine the WebObject corresponding to your document type and add a new method that will dispatch
 * the view <info>info</info> using a pretty path like <code>/my/doc/info</code>. But this involves changing code.
 * If you don't want this then the views adapter will be your friend.
 *
 * <p>
 * <p>
 * This example will extend the module defined in sample5 and will reuse and add more templates.
 * Look into template files to see how base module templates are reused.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type="sample5")
@Produces(["text/html"])
public class Sample5 extends Sample4 {

  /**
   * We are reusing bindings declared in the main class from sample5 and only a new one.
   */
  @Path("info")
  @GET
  public Object getInfo() {
    return "This is the 'info' segment added by the derived module";
  }

}

