/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.nuxeo.ecm.webengine.rest.jersey.patch;

import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;
import com.sun.jersey.api.uri.ExtendedUriInfo;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.impl.MultivaluedMapImpl;
import com.sun.jersey.impl.model.ResourceClass;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.uri.rules.UriRule;
import com.sun.jersey.spi.uri.rules.UriRuleContext;
import com.sun.jersey.spi.uri.rules.UriRules;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class WebApplicationContext implements UriRuleContext, ExtendedUriInfo {

    protected final ContainerRequest request;

    protected final ContainerResponse response;

    protected final WebApplicationImpl app;

    protected Map<String, Object> properties;

    public WebApplicationContext(WebApplicationImpl app,
            ContainerRequest request, ContainerResponse response) {
        this.app = app;
        this.request = request;
        this.response = response;
    }


    // HttpContext

    public HttpRequestContext getRequest() {
        return request;
    }

    public HttpResponseContext getResponse() {
        return response;
    }

    public ExtendedUriInfo getUriInfo() {
        return this;
    }

    public Map<String, Object> getProperties() {
        if (properties != null)
            return properties;

        return properties = new HashMap<String, Object>();
    }

    // UriRuleContext

    private final LinkedList<Object> resources = new LinkedList<Object>();

    private final LinkedList<String> paths = new LinkedList<String>();

    private final LinkedList<UriTemplate> templates = new LinkedList<UriTemplate>();

    private final List<String> capturingGroupValues = new ArrayList<String>();

    public Object getResource(Class resourceClass) {
        final ResourceClass rc = app.getResourceClass(resourceClass);
        return rc.provider.getInstance(app.getResourceComponentProvider(), this);
    }

    public UriRules<UriRule> getRules(Class resourceClass) {
        final ResourceClass rc = app.getResourceClass(resourceClass);
        return rc.getRules();
    }

    public List<String> getGroupValues() {
        return capturingGroupValues;
    }

    public void setTemplateValues(List<String> names) {
        if (encodedTemplateValues == null)
            encodedTemplateValues = new MultivaluedMapImpl();

        int i = 0;
        for (String name : names) {
            final String value = capturingGroupValues.get(i++);
            encodedTemplateValues.addFirst(name, value);

            if (decodedTemplateValues != null) {
                decodedTemplateValues.addFirst(
                        UriComponent.decode(name, UriComponent.Type.PATH_SEGMENT),
                        UriComponent.decode(value, UriComponent.Type.PATH));
            }
        }
    }

    public void pushResource(Object resource, UriTemplate template) {
         resources.addFirst(resource);
         templates.addFirst(template);
    }

    public void pushRightHandPathLength(int rhpathlen) {
        paths.addFirst(getEncodedPath().substring(0,
                getEncodedPath().length() - rhpathlen));
    }

    // UriInfo

    /**
     * The percent-encoded path component.
     *
     * The path is relative to the path component of the base URI. The path
     * must not start with a '/'.
     */
    private String encodedPath;

    /**
     * The decoded path component.
     */
    private String decodedPath;

    private List<PathSegment> decodedPathSegments;
    private List<PathSegment> encodedPathSegments;

    private MultivaluedMap<String, String> decodedQueryParameters;
    private MultivaluedMap<String, String> encodedQueryParameters;

    private MultivaluedMapImpl encodedTemplateValues;
    private MultivaluedMapImpl decodedTemplateValues;

    public String getPath() {
        return getPath(true);
    }

    public String getPath(boolean decode) {
        if (decode) {
            if (decodedPath != null) return decodedPath;

            return decodedPath = UriComponent.decode(
                    getEncodedPath(),
                    UriComponent.Type.PATH);
        } else {
            return getEncodedPath();
        }
    }

    /**
     * Patch note: The encoded path is incorrectly calculated for requests o the form /context_path/servlet_path
     * (i.e. the resulting path will be /context_path/servlet_path)
     * @return
     */
    private String getEncodedPath() {
        if (encodedPath != null) return encodedPath;

        String reqUri = getRequestUri().getRawPath();
        String baseUri = getBaseUri().getRawPath();
        int len = baseUri.length();
        if (len == 1 || reqUri.length() == len) { // if reqUri equals with baseUri or baseUri is '/'
            return "";
        } else {
            return encodedPath = reqUri.substring(baseUri.length());
        }
    }

    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    public List<PathSegment> getPathSegments(boolean decode) {
        if (decode) {
            if (decodedPathSegments != null)
                return decodedPathSegments;

            return decodedPathSegments = extractPathSegments(getPath(false), true);
        } else {
            if (encodedPathSegments != null)
                return encodedPathSegments;

            return encodedPathSegments = extractPathSegments(getPath(false), false);
        }
    }

    public String getPathExtension() {
        throw new UnsupportedOperationException();
    }

    public URI getBaseUri() {
        return request.getBaseUri();
    }

    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    public URI getAbsolutePath() {
        return request.getAbsolutePath();
    }

    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(getAbsolutePath());
    }

    public URI getRequestUri() {
        return request.getRequestUri();
    }

    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
    }

    public UriBuilder getPlatonicRequestUriBuilder() {
        throw new UnsupportedOperationException();
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        if (decode) {
            if (decodedTemplateValues != null)
                return decodedTemplateValues;

            decodedTemplateValues = new MultivaluedMapImpl();
            for (Map.Entry<String, List<String>> e : encodedTemplateValues.entrySet()) {
                List<String> l = new ArrayList<String>();
                for (String v : e.getValue()) {
                    l.add(UriComponent.decode(v, UriComponent.Type.PATH));
                }
                decodedTemplateValues.put(
                        UriComponent.decode(e.getKey(), UriComponent.Type.PATH_SEGMENT),
                        l);
            }

            return decodedTemplateValues;
        } else {
            return encodedTemplateValues;
        }
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        if (decode) {
            if (decodedQueryParameters != null)
                return decodedQueryParameters;

            return decodedQueryParameters = UriComponent.decodeQuery(
                    getRequestUri(), true);
        } else {
            if (encodedQueryParameters != null)
                return encodedQueryParameters;

            return encodedQueryParameters = UriComponent.decodeQuery(
                    getRequestUri(), false);
        }
    }

    public List<String> getAncestorResourceURIs() {
        return paths;
    }

    public List<Object> getAncestorResources() {
        return resources;
    }

    public List<String> getAncestorResourceURIs(boolean decode) {
        throw new UnsupportedOperationException();
    }

    //

    public List<UriTemplate> getAncestorTemplates() {
        return templates;
    }

    public PathSegment getPathSegment(String name) {
        return getPathSegment(name, true);
    }

    public PathSegment getPathSegment(String name, boolean decode) {
        int index = 0;

        int i = -1;
        for (UriTemplate t : templates) {
            if (i == -1)
                i = t.getPathSegmentIndex(name);
            else
                i += t.getNumberOfPathSegments();
        }

        return (i != -1) ? getPathSegments(decode).get(i) : null;
    }


    //


    private static final class PathSegmentImpl implements PathSegment {
        private String path;

        private MultivaluedMap<String, String> matrixParameters;

        PathSegmentImpl(String path, MultivaluedMap<String, String> matrixParameters) {
            this.path = path;
            this.matrixParameters = matrixParameters;
        }

        public String getPath() {
            return path;
        }

        public MultivaluedMap<String, String> getMatrixParameters() {
            return matrixParameters;
        }
    }

    /**
     * Extract the path segments from the path
     * TODO: This is not very efficient
     */
    private List<PathSegment> extractPathSegments(String path, boolean decode) {
        List<PathSegment> pathSegments = new LinkedList<PathSegment>();

        if (path == null)
            return pathSegments;

        // TODO the extraction algorithm requires an absolute path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String[] subPaths = path.split("/");
        if (subPaths.length == 0) {
            PathSegment pathSegment = new PathSegmentImpl("", new MultivaluedMapImpl());
            pathSegments.add(pathSegment);
            return pathSegments;
        }

        for (String subPath : subPaths) {
            if (subPath.length() == 0)
                continue;

            MultivaluedMap<String, String> matrixMap = new MultivaluedMapImpl();
            int colon = subPath.indexOf(';');
            if (colon != -1) {
                String matrixParameters = subPath.substring(colon + 1);
                subPath = (colon == 0) ? "" : subPath.substring(0, colon);
                extractPathParameters(matrixParameters, ";", matrixMap, decode);
            }

            if (decode)
                subPath = UriComponent.decode(subPath, UriComponent.Type.PATH_SEGMENT);

            PathSegment pathSegment = new PathSegmentImpl(subPath, matrixMap);
            pathSegments.add(pathSegment);
        }

        return pathSegments;
    }

    /**
     * TODO: This is not very efficient
     */
    private void extractPathParameters(String parameters, String deliminator,
            MultivaluedMap<String, String> map, boolean decode) {
        for (String s : parameters.split(deliminator)) {
            if (s.length() == 0)
                continue;

            String[] keyVal = s.split("=");
            String key = (decode)
            ? UriComponent.decode(keyVal[0], UriComponent.Type.PATH_SEGMENT)
            : keyVal[0];
            if (key.length() == 0)
                continue;

            // parameter may not have a value, if so default to "";
            String val = (keyVal.length == 2) ?
                (decode) ? UriComponent.decode(keyVal[1], UriComponent.Type.PATH_SEGMENT) : keyVal[1] : "";

            List<String> list = map.get(key);
            if (map.get(key) == null) {
                list = new LinkedList<String>();
                map.put(key, list);
            }
            list.add(val);
        }
    }
}
