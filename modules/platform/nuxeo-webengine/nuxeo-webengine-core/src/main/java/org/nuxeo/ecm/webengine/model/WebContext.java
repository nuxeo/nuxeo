/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 */

package org.nuxeo.ecm.webengine.model;

import java.io.Writer;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.model.Adaptable;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.ServerInjectableProviderContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface WebContext extends Adaptable {

    /**
     * <p>
     * This variable is used in a proxy setting. The proxy should send a header with this name. Webengine will assume
     * that the base path of the application is this value.
     * <p>
     * If your application is on server localhost:8080/nuxeo/site/myapp and you proxy it via mysite.com/myapp, then the
     * header should have an empty string value.
     */
    String NUXEO_WEBENGINE_BASE_PATH = "nuxeo-webengine-base-path";

    /**
     * Gets the current web application.
     *
     * @return the web root. Cannot return null.
     */
    Module getModule();

    /**
     * Gets the jersey server injection context
     *
     * @since 9.1
     */
    ServerInjectableProviderContext getServerInjectableProviderContext();

    /**
     * Gets the jersey server http context
     *
     * @since 9.1
     */
    HttpContext getServerHttpContext();

    /**
     * Gets the root resource if any resource was defined as being the root
     * <p>
     * A root resource can be any resource from the resource chain.
     *
     * @return the root resource or null if no root was defined
     */
    Resource getRoot();

    /**
     * Set the root resource of this context
     */
    void setRoot(Resource root);

    /**
     * Gets the i18n message for the given key.
     * <p>
     * The current module i18n messages are queried first then if no message is found the global message pool is queried
     * (the one living in WEB-INF/i18n). If no message is found the key surrounded by exclamation marks is returned.
     *
     * @param key the message key
     * @return the message
     */
    String getMessage(String key);

    /**
     * The same as {@link #getMessage(String)} but with parameter support
     */
    String getMessage(String key, Object... args);

    /**
     * The same as {@link #getMessage(String)} but with parameter support
     */
    String getMessage(String key, List<Object> args);

    /**
     * Same as {@link #getMessage(String)} but uses the given locale.
     *
     * @param key the message key
     * @param locale the locale to use
     * @return the message
     */
    String getMessageL(String key, String locale);

    /**
     * The same as {@link #getMessage(String)} but uses the given locale, with parameter support
     */
    String getMessageL(String key, String locale, Object... args);

    /**
     * The same as {@link #getMessage(String)} but uses the given locale, with parameter support.
     */
    String getMessageL(String key, String locale, List<Object> args);

    /**
     * Gets the context locale.
     * <p>
     * If the locale has been set using {@link #setLocale(Locale)}, returns the last locale set. Otherwise, compute it
     * from the Accept-Language language sent by the client, using {@link HttpServletRequest#getLocale()}.
     *
     * @return the context locale
     */
    Locale getLocale();

    /**
     * Sets the current locale, that will be kept in context.
     */
    void setLocale(Locale locale);

    /**
     * Gets the web engine instance.
     *
     * @return the web engine instance. Cannot return null
     */
    WebEngine getEngine();

    /**
     * Gets the current user session.
     * <p>
     * The user session is a WebEngine abstraction for the current user session and can be used to retrieve current
     * login, core session, and to set or get user session variables.
     *
     * @return the user session. Never returns null.
     */
    UserSession getUserSession();

    /**
     * Gets the Core Session (or Repository Session) corresponding to that request.
     *
     * @return the core session. Cannot return null
     */
    CoreSession getCoreSession();

    /**
     * Gets the principal identifying the user that originated the request.
     *
     * @return the current principal. Can return null if the user has not been authenticated.
     */
    NuxeoPrincipal getPrincipal();

    /**
     * Gets the JAX-RS UriInfo.
     *
     * @return the uri info
     */
    UriInfo getUriInfo();

    /**
     * Gets HTTP Headers as defined by JAX-RS.
     *
     * @return HTTP headers object
     */
    HttpHeaders getHttpHeaders();

    /**
     * Gets the underlying HTTP servlet request object.
     *
     * @return the HTTP Request object. Cannot return null
     */
    HttpServletRequest getRequest();

    /**
     * Get HTTP Method.
     *
     * @return the method
     */
    String getMethod();

    /**
     * Gets the representation of the data form submitted by the user.
     * <p>
     * This provides access to both POST and GET parameters, or to multipart form data requests.
     *
     * @return the request form data. Cannot return null
     */
    FormData getForm();

    /**
     * Gets the URL requested by the client. Same as {@link HttpServletRequest#getRequestURL()}.
     *
     * @return the request URL. Cannot return null.
     */
    String getURL();

    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the
     * HTTP request. This is the same as {@link HttpServletRequest#getRequestURI()}.
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
     * Gets the login path for the current context.
     * <p>
     * This is the path you can use as a login form action to perform a login or a logout. After the login/logout is
     * done the current page in that context will be served.
     */
    String getLoginPath();

    /**
     * Get the path prefix that identify the current web application.
     * <p>
     * The application path will include the base path (context + servlet path).
     *
     * @return the application path. Cannot be null
     */
    String getModulePath();

    /**
     * Gets the path of the servlet. Same as servlet context path + servlet path.
     *
     * @return the site path
     */
    String getBasePath();

    /**
     * Gets the URL of the base path. This is the same as {@link #getURL()} after removing the path segments over the
     * base path.
     *
     * @return the base URL
     */
    String getBaseURL();

    /**
     * Gets the server URL without any path or trailing /. The returned string builder can be used to build the wanted
     * URL. If the server is behind a proxy, return the server url of the proxy so writing the url in a webpage is safe.
     *
     * @return a string builder
     */
    StringBuilder getServerURL();

    /**
     * Get a suitable URI path for the given Nuxeo document, that can be used to invoke this document. This method is
     * working only for root objects that implement {@link ModuleResource}
     *
     * @param document the nuxeo document
     * @return the path if any or null if no suitable path can be found XXX can this method return null?
     * @throws ClassCastException if the module root does not implementing {@link ModuleResource}
     */
    // TODO: should we remove this method from the context and create a
    // specialized service to resolve document models to paths?
    String getUrlPath(DocumentModel document);

    /**
     * Sets a context variable.
     *
     * @param key the variable key
     * @param value the variable value
     * @see #getProperty(String)
     */
    void setProperty(String key, Object value);

    /**
     * Gets a context variable.
     * <p>
     * Context variables can be used to share data between the scripts that are called in that request (and between Java
     * code too of course).
     *
     * @param key the variable key
     * @return the variable value or null if none
     */
    Object getProperty(String key);

    /**
     * Gets a context variable.
     * <p>
     * Context variables can be used to share data between the scripts that are called in that request (and between java
     * code too of course).
     *
     * @param key the variable key
     * @param defaultValue the default value to use if the property doesn't exists
     * @return the variable value or the given default value if none
     */
    Object getProperty(String key, Object defaultValue);

    /**
     * Convenience method to get a cookie value.
     *
     * @param name the cookie name
     * @return the cookie value if any null otherwise
     */
    String getCookie(String name);

    /**
     * Convenience method to get a cookie value using a default value.
     *
     * @param name the cookie name
     * @param defaultValue the value to return when cookie is not set
     * @return the cookie value if any or the default if none
     */
    String getCookie(String name, String defaultValue);

    /**
     * Gets a logger to be used by scripts for logging.
     *
     * @return a logger
     */
    Log getLog();

    Resource newObject(String typeName, Object... args);

    Resource newObject(ResourceType type, Object... args);

    AdapterResource newAdapter(Resource ctx, String adapterName, Object... args);

    /* object stack API */

    Resource push(Resource obj);

    Resource pop();

    Resource tail();

    Resource head();

    Resource getTargetObject();

    AdapterResource getTargetAdapter();

    /* template and script resolver */

    /**
     * Resolves the given path into a file.
     * <p>
     * The path is resolved as following:
     * <ol>
     * <li>if the path begin with a dot '.' then a local path is assumed and the path will be resolved relative to the
     * current executed script if any. Note that the directory stack will be consulted as well. If there is no current
     * executed script then the path will be transformed into an absolute path and next step is entered.
     * <li>the resolving is delegated to the current {@link Module#getFile(String)} that will try to resolve the path
     * relative to each directory in the directory stack
     * </ol>
     *
     * @param path the path to resolve into a file
     * @return the file or null if the path couldn't be resolved
     */
    ScriptFile getFile(String path);

    /* running scripts and rendering templates */

    /**
     * Renders the given template using the rendering engine registered in that web engine.
     * <p>
     * This is similar to the {@link #render(String, Object, Writer)} method with a null value for the <i>args</i>
     * argument.
     *
     * @param template the template to render. Can be a path absolute to the web directory or relative to the caller
     *            script if any.
     * @param writer the writer to use
     * @see #render(String, Object, Writer)
     */
    void render(String template, Writer writer);

    /**
     * Renders the given template using the rendering engine registered in that web engine. The given arguments are
     * passed to the rendering process as context variables
     *
     * @param template the template to render
     * @param args the arguments to pass
     * @param writer the writer to use
     */
    void render(String template, Object args, Writer writer);

    /**
     * Renders the given template using the rendering engine registered in that web engine.
     * <p>
     * The given arguments are passed to the rendering process as context variables.
     *
     * @param script the template to render
     * @param args the arguments to pass
     * @param writer the writer to use
     */
    void render(ScriptFile script, Object args, Writer writer);

    /**
     * Runs the given script.
     *
     * @param script the script path. Can be a path absolute to the web directory or relative to the caller script if
     *            any.
     * @param args the arguments to pass
     */
    Object runScript(String script, Map<String, Object> args);

    /**
     * Runs the given script.
     * <p>
     * This is similar to {@link #runScript(String, Map)} with a null value for the <i>args</i> argument
     *
     * @param script the script path. Can be a path absolute to the web directory or relative to the caller script if
     *            any.
     * @see #runScript(String, Map)
     */
    Object runScript(String script);

    /**
     * Runs the script using given arguments
     * <p>
     * This is similar to {@link #runScript(String, Map)} with a null value for the <i>args</i> argument
     *
     * @param script the script path. Can be a path absolute to the web directory or relative to the caller script if
     *            any.
     * @param args a map of arguments
     * @see #runScript(String, Map)
     */
    Object runScript(ScriptFile script, Map<String, Object> args);

    /**
     * Check the given expression in this context and return true if the expression is verified or false otherwise. Any
     * valid guard expression is accepted
     *
     * @see org.nuxeo.ecm.webengine.security.Guard
     * @param guard the guard to check
     */
    boolean checkGuard(String guard) throws ParseException;

    /**
     * Sets the repository name that will be used by {@link #getCoreSession()}.
     *
     * @param repoName
     * @throws IllegalArgumentException if the repository is not found.
     * @since 5.7.3
     */
    void setRepositoryName(String repoName);

}
