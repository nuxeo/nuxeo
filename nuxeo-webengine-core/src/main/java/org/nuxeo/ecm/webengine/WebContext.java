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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.FormData;

/**
 * Represents the web invocation context.
 *<p>
 * This is the main entry for scripts or java modules to access the web invocation context.
 * It provides access to the HTTP request and response, to the Nuxeo core session, to the web engine and
 * all the contextual objects like traversed documents etc.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebContext {

    /**
     * Get the web engine instance
     * @return the web engine instance. Cannot return null
     */
    WebEngine getWebEngine();

    /**
     * Get the underlying HTTP servlet request object
     * @return the HTTP Request object. Cannot return null
     */
    HttpServletRequest getRequest();

    /**
     * Get the underlying HTTP servlet response object
     * @return the HTTP Response object. Cannot return null
     */
    HttpServletResponse getResponse();

    /**
     * Get the first object in the traversal path
     * @return the first object. May return null in the case of a root request.
     */
    WebObject getFirstObject();

    /**
     * Get the last object on the traversal path
     * @return the last object. May return null in the case of a root request
     */
    WebObject getLastObject();

    /**
     * Tests whether the current request was made on the web engine root (there is no traversal path)
     *
     * @return true if this is a root request or false otherwise
     */
    boolean isRootRequest();

    /**
     * Get the first resolved object. The first resolved object is usually the root.
     * @return the first resolved object or null if none
     */
    WebObject getFirstResolvedObject();

    /**
     * Get the last resolved object.
     * A resolved object is an web object that is bound to a nuxeo document.
     * The last resolved object will be usually used as the target object of the request.
     *
     * @return the last resolved object or null if there are no resolved objects
     */
    WebObject getLastResolvedObject();

    /**
     * Get the first unresolved object if any
     * @return the first unresolved object or null if none
     */
    WebObject getFirstUnresolvedObject();

    /**
     * Tests whether there are unresolved objects on the traversal path
     * @return true if there are unresolved objects, false otherwise
     */
    boolean hasUnresolvedObjects();

    /**
     * Get the object traversal path
     * @return the traversal path. Cannot return null
     */
    List<WebObject> getTraversalPath();

    /**
     * Get the list of unresolved objects
     * @return the unresolved objects. Cannot return null.
     */
    List<WebObject> getUnresolvedObjects();

    /**
     * Get the list of resolved objects
     * @return the resolved objects. Cannot return null
     */
    List<WebObject> getResolvedObjects();

    /**
     * Attache the web object to the given nuxeo document.
     * The object is automatically resolved by this operation.
     * <p>
     * If the object cannot be attached false is returned otherwise true is returned.
     * Objects should fail to be resolved if they break the traversal path by inserting unresolved object gaps
     * between resolved objects.
     * <p>
     * This method can be used to dynamically change the traversal path
     *
     * @param object the object to bind
     * @param doc the document to bind the object to
     * @return true if object successfully resolved, false otherwise
     */
    boolean resolveObject(WebObject object, DocumentModel doc);

    /**
     * The target context object. This is the last object that was traversed and that can be seen as the target
     * object for this request.
     * When a request is made, the URL path is mapped on a chain of {@link WebObject} objects.
     * This chain is called the traversal path. The traversal will be empty when the URL path is empty (when requesting the root).
     * In order to choose the right target object, the object chain is traversed
     * (i.e. the {@link WebObject#traverse()} method called) until an object is returning false.
     * Usually this happens when the object cannot be mapped to a Nuxeo document.
     * So by default the target object is the last object that was mapped on a Nuxeo document
     * (or the last <i>resolved</i> object).
     * But anyway this can be changed by registering a custom {@link RequestHandler}.
     *
     * @return the target object (or the target object). May return null in the case of a root request.
     */
    WebObject getTargetObject();

    /**
     * Get the target script for this request if any.
     * The target script is computed as following:
     * <ul>
     * <li>If a mapping was matched for this request the mapping is consulted for the script path to be invoked
     * <li> If the previous step returns nothing the request action (the @@XXX string that may be appended to the request URI)
     * is used to find a suitable script.
     * If no action was specified in the request URI the default @@view action is assumed.
     * <li> If no action script is found try to use the unresolved path portion to locate a script on the file system
     * (in the current web root)
     * <li> If none of these steps return a valid script, the script registered to handle unknown requests is used
     * <li> If no such script was registered null is returned (and the client will get a 404)
     * </ul>
     * XXX can this method return null?
     * @return the target script or null if none.
     */
    ScriptFile getTargetScript() throws IOException;

    /**
     * The Core Session (or Repository Session) corresponding to that request.
     * @return the core session. Cannot return null
     */
    CoreSession getCoreSession() throws WebException;

    /**
     * Get the principal identifying the user that originatd the request
     * @return the current principal. Cannot return null.
     */
    Principal getPrincipal();

    /**
     * Get the Nuxeo document corresponding to the target object.
     * @return the current context document.
     *      May return null if none of the traversal objects wasn't mapped on a document
     */
    DocumentModel getTargetDocument();

    /**
     * Get the representation of the data form submitted by the user.
     * This is providing access to both POST and GET parameters, or to multipart form data requests.
     *
     * @return the request form data. Cannot return null
     */
    FormData getForm();

    /**
     * Get the mapping that was done on the request path.
     * The mapping is a rule to rewrite the URL or to set some special variables based on a regex pattern on the request path.
     * <p>
     * Mappings are user configurable and can be used to redirect some URLs to custom scripts.
     * @return the mapping if any or null if no mapping was done on this requets
     */
    Mapping getMapping();

    /**
     * Get the request path info.
     * This is the same as the {@link HttpServletRequest#getPathInfo()} with the difference that null is neveer returned.
     * In the case when the underlying {HttpServletRequest#getPathInfo()} method returns null this method will return "/"
     * @return the path info. Cannot return null.
     */
    String getPathInfo();

    /**
     * Get the URL requested by the client. Same as {@link HttpServletRequest#getRequestURL()}
     * @return the request URL. Cannot return null.
     */
    String getURL();

    /**
     * Returns the part of this request's URL from the protocol
     * name up to the query string in the first line of the HTTP request..
     * This is the same as {@link HttpServletRequest#getRequestURI()}
     * @return the request URI. Cannot return null.
     */
    String getURI();

    /**
     * Get the path portion of the request URL
     * @return the path portion of the request URL. Cannot return null.
     */
    String getUrlPath();

    /**
     * Get the path of the servlet.
     * Same as servlet context path + servlet path
     * @return the site path
     */
    String getSitePath();

    /**
     * Get the path  path corresponding to the target object of the request.
     * @return the target object path. Will never return null. If the target object is null returns "/".
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
     * Given a relative path to the current script, create an absolute one (relative to the web directory)
     * If this method is not called from a script context - the base path that will be used will be the current web root.
     * @param relPath the relative path to transform
     * @return the absolute path
     */
    String makeAbsolutePath(String relPath);

    /**
     * Set the action name for this request. The action name is the string following "@@" at the end of the request URI.
     * This method can be used to change the action that will be executed.
     * This will work only if the action was not yet executed.
     * @param name the new action name
     */
    void setActionName(String name);

    /**
     * Get the request action name
     * @see WebContext#setActionName(String)
     * @return the action name or null if none
     */
    String getActionName();

    /**
     * Get the actions that are available on the target object
     * @return the target object actions or null if no target object exists
     */
    Collection<ActionDescriptor> getActions() throws WebException;

    /**
     * Get the actions that are available on the target object and that are part of the given category
     * @param category the category to filter actions
     * @return the target object actions or null if no target object exists
     */
    Collection<ActionDescriptor> getActions(String category) throws WebException;

    /**
     * Get the actions that are available on the target object grouped by categories
     * @return a map of category -> actions or null if no target object exists
     */
    Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException;


    /**
     * Get the current web application.
     * @return the web root. Cannot return null.
     */
    WebApplication getApplication();

    /**
     * Get a context variable
     * <p>
     * Context variables can be used to share data between the scripts that are called in that request (and between java code too of course)
     *
     * @param key the variable key
     * @return the variable value or null if none
     */
    Object getProperty(String key);

    /**
     * Get a context variable
     * <p>
     * Context variables can be used to share data between the scripts that are called in that request (and between java code too of course)
     *
     * @param key the variable key
     * @param defaultValue the default value to use if the property doesn't exists
     * @return the variable value or the given default value if none
     */
    Object getProperty(String key, Object defaultValue);

    /**
     * Set a context variable
     * @param key the variable key
     * @param value the variable value
     * @see #getProperty(String)
     */
    void setProperty(String key, Object value);     // set a context variable (can be shared between scripts)

    /**
     * Get a map with environment variables. These variables are global on the web engine level.
     * @return the environment variable map. Cannot return null.
     */
    Map<String,Object> getEnvironment();               // get the environment vars (shared at engine level) ~ same as getEngine().getEnv()

    /**
     * Cancel any further processing
     * This can be used to inform the web engine that the next step in the request processing should be canceled and
     * request should end by sending a 200 OK code to the client.
     * <p>
     * This can be used to cancel rendering from request handlers.
     */
    void cancel();

    /**
     * Same as the previous method but the error code returned to the client can be specified by the caller.
     * @param errorCode the error code returned to the client
     * @see #cancel()
     */
    void cancel(int errorCode);

    /**
     * Test whether the request was previously canceled using one of the {@link #cancel()} methods
     * @return true if the request was already canceled false otherwise
     */
    boolean isCanceled();

    /**
     * Redirect the client to another URL
     * @param url the URL where to redirect
     * XXX should remove this method?
     */
    void redirect(String url) throws IOException;

    /**
     * Render the given template using the rendering engine registered in that web engine.
     * The given arguments are passed to the rendering process as context variables
     * @param template the template to render
     * @param args the arguments to pass
     * @throws WebException
     */
    void render(String template, Object args) throws WebException;

    /**
     * Render the given template using the rendering engine registered in that web engine.
     * <p>
     * This is similar to the {@link #render(String, Map)} method with a null value for the  <i>args</i> argument.
     * @param template the template to render. Can be a path absolute to the web directory or relative to the
     *          caller script if any.
     * @see #render(String, Map)
     */
    void render(String template) throws WebException;

    /**
     * Run the given script.
     * @param script the script path. Can be a path absolute to the web directory or relative to the
     *          caller script if any.
     * @param args the arguments to pass
     */
    void runScript(String script, Map<String, Object> args) throws WebException;

    /**
     * Run the given script.
     * <p>
     * This is similar to {@link #runScript(String, Map)} with a null value for the <i>args</i> argument
     * @param script the script path. Can be a path absolute to the web directory or relative to the
     *          caller script if any.
     * @see #runScript(String, Map)
     */
    void runScript(String script) throws WebException;

    /**
     * Write some text on the HTTP request output stream
     * @param text
     */
    void print(String text) throws IOException;

    /**
     * Convert the given document to a JSON String
     * @param doc the doc to convert
     * @return the JSON string
     * @throws WebException
     */
    JSONObject toJSon(DocumentModel doc) throws WebException;

    /**
     * Convert the given document to a JSON String. Only specified schemas should be included in the JSON representation
     * @param doc the doc to convert
     * @param schemas the schemas to include
     * @return the JSON string
     * @throws WebException
     */
    JSONObject toJSon(DocumentModel doc, String ... schemas) throws WebException;

    /**
     * This is a helper method that performs a query against nuxeo repository.
     * <p>
     * This method is provided as a convenience method to perform searches since the search API
     * is for now difficult to use from a scripting environment
     * <p>
     * This method will be removed in the future when the search API will be cleaned-up and
     * then replaced with {@link CoreSession#query(String)}
     *
     * @param query the query to perform
     * @return a list of documents that matched the query
     * @throws WebException
     * @deprecated will be replaced by {@link CoreSession#query(String)} in future
     */
    DocumentModelList search(String query) throws WebException;

}
