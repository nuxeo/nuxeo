/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.model.Adaptable;

/**
 * Represents the web invocation context.
 * <p>
 * This is the main entry for scripts or java modules to access the web
 * invocation context. It provides access to the HTTP request and response, to
 * the Nuxeo core session, to the web engine and all the contextual objects like
 * traversed documents etc.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface WebContext extends Adaptable {

    /**
     * Must be called before using the context.
     *
     * @throws WebException
     * @throws IllegalStateException if already initialized
     */
    void initialize() throws WebException;

    /**
     * Gets the web engine instance.
     *
     * @return the web engine instance. Cannot return null
     */
    WebEngine getWebEngine();

    /**
     * Gets the underlying HTTP servlet request object.
     *
     * @return the HTTP Request object. Cannot return null
     */
    HttpServletRequest getRequest();

    /**
     * Gets the underlying HTTP servlet response object.
     *
     * @return the HTTP Response object. Cannot return null
     */
    HttpServletResponse getResponse();

    /**
     * Gets the first object in the traversal path.
     *
     * @return the first object. May return null in the case of a root request.
     */
    WebObject getFirstObject();

    /**
     * Gets the last object on the traversal path.
     *
     * @return the last object. May return null in the case of a root request
     */
    WebObject getLastObject();

    /**
     * Gets the list of resolved objects (the traversal objects).
     *
     * @return the resolved objects. Cannot return null
     */
    List<WebObject> getTraversalObjects();

    /**
     * Go up into the traversal path.
     */
    void removeLastTraversalObject();

    /**
     * Gets the target context object.
     * <p>
     * This is the last object that was traversed and that can be seen as the
     * target object for this request.
     * <p>
     * When a request is made, the URL path is mapped on a chain of
     * {@link WebObject} objects. This chain is called the traversal path. The
     * traversal will be empty when the URL path is empty (when requesting the
     * root).
     * <p>
     * In order to choose the right target object, the object chain is traversed
     * (i.e. the {@link WebObject#traverse()} method called) until an object is
     * returning false. Usually this happens when the object cannot be mapped to
     * a Nuxeo document. So by default the target object is the last object that
     * was mapped on a Nuxeo document (or the last <i>resolved</i> object). But
     * anyway this can be changed by registering a custom {@link RequestHandler}.
     *
     * @return the target object (or the target object). May return null in the
     *         case of a root request.
     */
    WebObject getTargetObject();

    /**
     * Tests whether the context is bound to a document.
     *
     * @return true if there is a traversal path (a current document), false
     *         otherwise
     */
    boolean hasTraversalPath();

    /**
     * Gets the target script for this request if any.
     * <p>
     * The target script is computed as following:
     * <ul>
     * <li> if the any targetScriptpath was specified using
     * {@link #setTargetScriptPath(String)} this will be used
     * <li> If a mapping was matched for this request the mapping is consulted
     * for the script path to be invoked
     * <li> If the previous step returns nothing the request action (the
     * {@code @@XXX} string that may be appended to the request URI) is used to
     * find a suitable script. If no action was specified in the request URI the
     * default {@code @@view} action is assumed.
     * <li> If no action script is found try to use the unresolved path portion
     * to locate a script on the file system (in the current web root)
     * <li> If none of these steps return a valid script, the script registered
     * to handle unknown requests is used
     * <li> If no such script was registered null is returned (and the client
     * will get a 404)
     * </ul>
     *
     * @return the target script or null if none.
     */
    // XXX can this method return null?
    ScriptFile getTargetScript() throws IOException;

    /**
     * The Core Session (or Repository Session) corresponding to that request.
     *
     * @return the core session. Cannot return null
     */
    CoreSession getCoreSession() throws WebException;

    /**
     * A method to be used to change the session durring the current request
     * @param session the new session to use
     */
    void setCoreSession(CoreSession session);

    /**
     * Gets the principal identifying the user that originated the request.
     *
     * @return the current principal. Cannot return null.
     */
    Principal getPrincipal();

    /**
     * Gets the Nuxeo document corresponding to the target object.
     *
     * @return the current context document. May return null if none of the
     *         traversal objects wasn't mapped on a document
     */
    DocumentModel getTargetDocument();

    /**
     * Gets the representation of the data form submitted by the user. This is
     * providing access to both POST and GET parameters, or to multipart form
     * data requests.
     *
     * @return the request form data. Cannot return null
     */
    FormData getForm();

    /**
     * Gets the request path info. The path info is build from the request path
     * info and contains additional information needed to map the request to a
     * document
     *
     * @return the path info. Cannot return null.
     *
     * @deprecated this method will be removed in future versions and PathInfo merged with WebContext
     */
    @Deprecated
    PathInfo getPathInfo();

    /**
     * Gets the URL requested by the client. Same as
     * {@link HttpServletRequest#getRequestURL()}
     *
     * @return the request URL. Cannot return null.
     */
    String getURL();

    /**
     * Returns the part of this request's URL from the protocol name up to the
     * query string in the first line of the HTTP request. This is the same as
     * {@link HttpServletRequest#getRequestURI()}
     *
     * @return the request URI. Cannot return null.
     */
    String getURI();

    /**
     * Gets the path portion of the request URL.
     *
     * @return the path portion of the request URL. Cannot return null.
     */
    String getUrlPath();

    /**
     * Get the path prefix that identify the current web application.
     * The application path will include the base path (context + servlet path)
     * @return the application path. Cannot be null
     */
    String getApplicationPath();

    /**
     * Gets the path of the servlet. Same as servlet context path + servlet path
     *
     * @return the site path
     */
    String getBasePath();

    /**
     * Gets the URL of the base path. This is the same as {@link #getURL()}
     * after removing the path segments over the base path.
     *
     * @return the base URL
     */
    String getBaseURL();

    /**
     * Gets the path path corresponding to the target object of the request.
     *
     * @return the target object path. Will never return null. If the target
     *         object is null returns "/".
     */
    String getTargetObjectUrlPath();

    /**
     * Get a suitable URI path for the given Nuxeo document, that can be used to invoke this document.
     *
     * @param document the nuxeo document
     * @return the path if any or null if no suitable path can be found
     * XXX can this method return null?
     */
    String getUrlPath(DocumentModel document); // try to resolve a nuxeo doc to a web object path

    /**
     * Gets the actions that are available on the target object.
     *
     * @return the target object actions or null if no target object exists
     */
    Collection<ActionDescriptor> getActions() throws WebException;

    /**
     * Gets the actions that are available on the target object and that are part of the given category.
     *
     * @param category the category to filter actions
     * @return the target object actions or null if no target object exists
     */
    Collection<ActionDescriptor> getActions(String category) throws WebException;

    /**
     * Gets the actions that are available on the target object grouped by categories.
     *
     * @return a map of category -> actions or null if no target object exists
     */
    Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException;

    /**
     * Gets the current web application.
     *
     * @return the web root. Cannot return null.
     */
    WebApplication getApplication();

    /**
     * Gets a context variable.
     * <p>
     * Context variables can be used to share data between the scripts that are
     * called in that request (and between Java code too of course).
     *
     * @param key
     *            the variable key
     * @return the variable value or null if none
     */
    Object getProperty(String key);

    /**
     * Gets a context variable.
     * <p>
     * Context variables can be used to share data between the scripts that are
     * called in that request (and between java code too of course).
     *
     * @param key
     *            the variable key
     * @param defaultValue
     *            the default value to use if the property doesn't exists
     * @return the variable value or the given default value if none
     */
    Object getProperty(String key, Object defaultValue);

    /**
     * Convenience method to get a cookie value
     * @param name the cookie name
     * @return the cookie value if any null otherwise
     */
    String getCookie(String name);

    /**
     * Convenience method to get a cookie value using a default value
     * @param name the cookie name
     * @param defaultValue the value to return when cookie is not set
     * @return the cookie value if any or the default if none
     */
    String getCookie(String name, String defaultValue);

    /**
     * Convenience method to set a cookie
     * @param name the cookie name
     * @param value the cookie value
     */
    void setCookie(String name, String value);

    /**
     * Sets a context variable.
     *
     * @param key the variable key
     * @param value the variable value
     * @see #getProperty(String)
     */
    void setProperty(String key, Object value);  // set a context variable (can be shared between scripts)

    /**
     * Gets a map with environment variables.
     * These variables are global on the web engine level.
     *
     * @return the environment variable map. Cannot return null.
     */
    Map<String,Object> getEnvironment(); // get the environment vars (shared at engine level) ~ same as getEngine().getEnv()

    /**
     * Cancel any further processing.
     * <p>
     * This can be used to inform the web engine that the next step in the
     * request processing should be canceled and request should end by sending a
     * 200 OK code to the client.
     * <p>
     * This can be used to cancel rendering from request handlers.
     */
    void cancel();

    /**
     * Same as the previous method but the error code returned to the client can
     * be specified by the caller.
     *
     * @param errorCode
     *            the error code returned to the client
     * @see #cancel()
     */
    void cancel(int errorCode);

    /**
     * Tests whether the request was previously canceled using one of the
     * {@link #cancel()} methods.
     *
     * @return true if the request was already canceled false otherwise
     */
    boolean isCanceled();

    /**
     * Redirects the client to another URL.
     *
     * @param url the URL where to redirect
     * XXX should remove this method?
     */
    void redirect(String url) throws IOException;

    /**
     * Renders the given template using the rendering engine registered in that
     * web engine. The given arguments are passed to the rendering process as
     * context variables
     *
     * @param template
     *            the template to render
     * @param args
     *            the arguments to pass
     * @throws WebException
     */
    void render(String template, Object args) throws WebException;

    /**
     * Renders the given template using the rendering engine registered in that
     * web engine.
     * <p>
     * This is similar to the {@link #render(String, Map)} method with a null
     * value for the <i>args</i> argument.
     *
     * @param template
     *            the template to render. Can be a path absolute to the web
     *            directory or relative to the caller script if any.
     * @see #render(String, Map)
     */
    void render(String template) throws WebException;

    /**
     * Runs the given script.
     *
     * @param script
     *            the script path. Can be a path absolute to the web directory
     *            or relative to the caller script if any.
     * @param args
     *            the arguments to pass
     */
    Object runScript(String script, Map<String, Object> args) throws WebException;

    /**
     * Runs the given script.
     * <p>
     * This is similar to {@link #runScript(String, Map)} with a null value for
     * the <i>args</i> argument
     *
     * @param script
     *            the script path. Can be a path absolute to the web directory
     *            or relative to the caller script if any.
     * @see #runScript(String, Map)
     */
    Object runScript(String script) throws WebException;


    /**
     * Executes the given file. The file can be a script, a template or a
     * resource file
     * <p>
     * In the case of a resource file this will be copied to the output stream
     *
     * @param script
     *            the file to execute
     * @param args
     *            the arguments. can be null if none
     * @return the execution result if any or null otherwise
     * @throws WebException
     */
    Object exec(ScriptFile script, Map<String, Object> args) throws WebException;

    /**
     * Resolves the given path into a file.
     * <p>
     * The path is resolved as following:
     * <ol>
     * <li> if the path begin with a dot '.' then a local path is assumed and
     * the path will be resolved relative to the current executed script if any.
     * Note that the directory stack will be consulted as well. If there is no
     * current executed script then the path will be transformed into an
     * absolute path and next step is entered.
     * <li> the resolving is delegated to the current
     * {@link WebApplication#getFile(String)} that will try to resolve the path
     * relative to each directory in the directory stack
     * </ol>
     *
     * @param path
     *            the path to resolve into a file
     * @return the file or null if the path couldn't be resolved
     * @throws IOException
     */
    ScriptFile getFile(String path) throws IOException;

    /**
     * Writes some text on the HTTP request output stream
     *
     * @param text
     */
    void print(String text) throws IOException;

    /**
     * Get a logger to be used by scripts for logging
     * @return a logger
     */
    Log getLog();

    /**
     * Converts the given document to a JSON String
     *
     * @param doc
     *            the doc to convert
     * @return the JSON string
     * @throws WebException
     */
    JSONObject toJSon(DocumentModel doc) throws WebException;

    /**
     * Converts the given document to a JSON String. Only specified schemas
     * should be included in the JSON representation
     *
     * @param doc
     *            the doc to convert
     * @param schemas
     *            the schemas to include
     * @return the JSON string
     * @throws WebException
     */
    JSONObject toJSon(DocumentModel doc, String ... schemas) throws WebException;

    /**
     * Resolves first segment from the trailing path if one exists. A new
     * {@link WebObject} will be create from that segment and attached to the
     * given document then the object will be added to the traversal path and
     * first segment will be removed from the trailing path.
     * <p>
     * This operation is modifying the target object.
     *
     * @param doc
     */
    void resolveFirstUnresolvedSegment(DocumentModel doc);

    /**
     * Gets the first segment from the trailing path if any
     *
     * @return the first unresolved segment or null if none
     */
    String getFirstUnresolvedSegment();

    /**
     * Get the current user session.
     * <p>
     * The user session is a WebEngine abstraction for the current user session and can be
     * used to retrieve current login, core session, and to set or get user session variables
     *
     * @return the user session. Never returns null.
     */
    public UserSession getUserSession();


    /** --------- Experimental methods - these may change in future ------------ */

    /**
     * Get the URL path used by the client to load the main page.
     * <p>
     * This is useful when the client is using sub-contexts to load dynamically (via ajax)
     * sub sections from the main page. In that case on the server side you may want to know
     * what is the current client context - whether the client requested a page in the default context (e.g. the main page)
     * or it is requesting a page from a sub-context of the main page. The current client context can be retrieved
     * using {@link #getClientContext()}.
     * <p>
     * Also you may want to know, when in a sub-context request, what is the URL of the main page.
     * This is useful to correctly generate links on server side scripts.
     * <p>
     * Thus this method returns the URL path requested by the client for the main page.
     */
    String getClientUrlPath();

    /**
     * Get the current client context
     * @return the current context or null if the default context
     */
    String getClientContext();

    /**
     * Get a client variable or null if it doesn't exists
     * @param key
     * @return
     */
    Object getClientVariable(String key);

    /**
     * Get a client variable or a default value if the variable doesn't exists
     * @param key
     * @param defaultValue
     * @return
     */
    Object getClientVariable(String key, Object defaultValue);

    /**
     * Set a client variable. Use null to remove it
     * @param key
     * @param value
     */
    void setClientVariable(String key, Object value);


}
