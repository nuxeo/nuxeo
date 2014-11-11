package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * <h1>Web Module Extensibility.</h1>
 *
 * This sample is demonstrating how existing web modules can be extended. To
 * extend another module you should use the {@code base=BaseModule} in the
 * {@code NuxeoWebModule} directive in {@code MANIFEST.MF} file. This
 * way the new module will inherit all templates and resources defined in the
 * base module. You can thus create a chain of inherited web modules.
 * <p>
 * Here is how template resolution will be impacted by the module inheritance: <br>
 * <i>If a template T is not found in skin directory of derived module then
 * search the template inside the base module and so on until a template is
 * found or no more base module exists.</i> The view resolution is similar to
 * the template one but it will use the {@code WebObject} inheritance too:
 * <br>
 * <i></i> <br>
 * <b>Note</b> that only the <i>skin</i> directory is stacked over the one in
 * the base module. The other directories in the module are not inheritable.
 * <p>
 * Also, resource types defined by the base module will become visible in the
 * derived one.
 * <p>
 * In this example you will also find a very useful feature of WebEngine: the
 * builtin <b>view service adapter</b>. This adapter can be used on any web
 * object to locate any view declared on that object. Let's say we define a view
 * named <i>info</i> for the <i>Document</i> WebObject type. And the following
 * request path will point to a Document WebObject: {@code /my/doc}. Then
 * to display the <i>info</i> view we can use the builtin views adapter this
 * way: {@code /my/doc/@views/info}.
 * <p>
 * Obviously, you can redefine the WebObject corresponding to your document type
 * and add a new method that will dispatch the view <info>info</info> using a
 * pretty path like {@code /my/doc/info}. But this involves changing code.
 * If you don't want this then the views adapter will be your friend.
 *
 * <p>
 * <p>
 * This example will extend the resource defined in sample4 and will reuse and
 * add more templates. Look into template files to see how base module templates
 * are reused.
 *
 * <h1> Managing links.</h1>
 * <p>
 * Almost any template page will contain links to other pages in your
 * application. These links are usually absolute paths to other WebObjects or
 * WebAdapters (including parameters if any). Maintaining these links when
 * application object changes is painful when you are using modular applications
 * (that may contribute new views or templates).
 * <p>
 * WebEngine is providing a flexible way to ease link management. First, you
 * should define all of your links in <i>module.xml</i> configuration file. A
 * Link is described by a target URL, an enablement condition, and one or more
 * categories that can be used to organize links.
 * <ul>
 * Here are the possible conditions that you can use on links:
 * <li>type - represent the target Web Object type. If present the link will be
 * enabled only in the context of such an object.
 * <li>adapter - represent the target Web Adapter name. If present the link will
 * be enabled only if the active adapter is the same as this one.
 * <li>facet - a set of facets that the target web object must have in order to
 * enable the link.
 * <li>guard - a guard to be tested in order to enable the link. This is using
 * the guard mechanism of WebEngine.
 * </ul>
 * If several conditions are specified an {@code AND} will be used between
 * them.
 * <p>
 * Apart conditions you can <i>group</i> links in categories. Using categories
 * and conditions you can quickly find in a template which are all enabled links
 * that are part of a category. This way, you can control which links are
 * written in the template without needing to do conditional code to check the
 * context if links are enabled.
 * <p>
 * Conditions and categories manage thus where and when your links are displayed
 * in a page. Apart this you also want to have a target URL for each link.
 * <ul>
 * You have two choices in specifying such a target URL:
 * <li>define a custom link handler using the
 * {@code handler</handler> link attribute.}
 * The handler will be invoked each time the link code need to be written in the output stream so that it can programatically generate the link code.
 * <li> use the builtin link handler. The builtin link handler will append the {@code path}
 * attribute you specified in link definition to the current WebObject path on
 * the request. This behavior is good enough for most of the use cases.
 * <li>
 * </ul>
 * <p>
 * <p>
 * This example will demonstrate how links work. Look into
 * {@code module.xml} for link definitions and then in
 * {@code skin/views/Document/index.ftl} on how they are used in the
 * template.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "Repository")
@Produces("text/html;charset=UTF-8")
public class ExtendedDocumentsObject extends DocumentsObject {
    /**
     * We are reusing bindings declared in the main class from sample5 and only
     * add a new one.
     */
    @Path("info")
    @GET
    public Object getInfo() {
        return "This is the 'info' segment added by the derived module";
    }

}
