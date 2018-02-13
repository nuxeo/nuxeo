/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to parse a URI or build one given parameters.
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public final class URIUtils {

    private static final Log log = LogFactory.getLog(URIUtils.class);

    // This is an utility class.
    private URIUtils() {
    }

    /**
     * Creates an URI query given the request parameters.
     *
     * @return an URI query given the request parameters.
     */
    public static String getURIQuery(Map<String, String> parameters) {
        String query = null;
        if (parameters != null) {
            try {
                List<String> items = new ArrayList<>();
                for (Map.Entry<String, String> paramInfo : parameters.entrySet()) {
                    String key = paramInfo.getKey();
                    String value = paramInfo.getValue();
                    if (key != null) {
                        if (value == null) {
                            value = "";
                        }
                        items.add(String.format("%s=%s", URLEncoder.encode(key, UTF_8.name()),
                                URLEncoder.encode(value, UTF_8.name())));
                    }
                }
                query = String.join("&", items);
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to get uri query", e);
            }
        }
        return query;
    }

    /**
     * Returns an URI path given the uri.
     */
    public static String getURIPath(String uri) {
        if (uri == null) {
            return null;
        }
        String path = uri;
        int index = uri.indexOf('?');
        if (index != -1) {
            path = uri.substring(0, index);
        }
        return path;
    }

    /**
     * @return a map with request parameters information given an URI query.
     */
    public static Map<String, String> getRequestParameters(String uriQuery) {
        Map<String, String> parameters = null;
        if (uriQuery != null && uriQuery.length() > 0) {
            try {
                String strippedQuery;
                int index = uriQuery.indexOf('?');
                if (index != -1) {
                    strippedQuery = uriQuery.substring(index + 1);
                } else {
                    strippedQuery = uriQuery;
                }
                String[] items = strippedQuery.split("&");
                if (items.length > 0) {
                    parameters = new LinkedHashMap<>();
                    for (String item : items) {
                        String[] param = item.split("=");
                        if (param.length == 2) {
                            parameters.put(URLDecoder.decode(param[0], "UTF-8"),
                                    URLDecoder.decode(param[1], "UTF-8"));
                        } else if (param.length == 1) {
                            parameters.put(URLDecoder.decode(param[0], "UTF-8"), null);
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to get request parameters from uri", e);
            }
        }
        return parameters;
    }

    public static String addParametersToURIQuery(String uriString, Map<String, String> parameters) {
        if (uriString == null || parameters == null || parameters.isEmpty()) {
            return uriString;
        }
        String uriPath = getURIPath(uriString);
        Map<String, String> existingParams = getRequestParameters(uriString.substring(uriPath.length()));
        if (existingParams == null) {
            existingParams = new LinkedHashMap<>();
        }
        existingParams.putAll(parameters);
        String res;
        if (!existingParams.isEmpty()) {
            String newQuery = getURIQuery(existingParams);
            res = uriPath + '?' + newQuery;
        } else {
            res = uriPath;
        }
        return res;
    }

    public static String quoteURIPathComponent(String s, boolean quoteSlash) {
        return quoteURIPathComponent(s, quoteSlash, true);
    }

    /**
     * Quotes a URI path component, with ability to quote "/" and "@" characters or not depending on the URI path
     *
     * @since 5.6
     * @param s the uri path to quote
     * @param quoteSlash true if "/" character should be quoted and replaced by %2F
     * @param quoteAt true if "@" character should be quoted and replaced by %40
     */
    public static String quoteURIPathComponent(String s, boolean quoteSlash, boolean quoteAt) {
        return quoteURIPathComponent(s, quoteSlash ? "%2F" : null, quoteAt ? "%40" : null);
    }

    /**
     * Quotes a URI path token. For example, a blob filename. It replaces "/" by "-".
     *
     * @since 7.3
     * @param s the uri path token to quote
     */
    public static String quoteURIPathToken(String s) {
        return quoteURIPathComponent(s, "-", "%40");
    }

    /**
     * Quotes a URI path component, with ability to quote "/" and "@" characters or not depending on the URI path
     *
     * @since 5.6
     * @param s the uri path to quote
     * @param slashSequence if null, do not quote "/", otherwise, replace "/" by the given sequence
     * @param atSequence if null, do not quote "@", otherwise, replace "@" by the given sequence
     */
    protected static String quoteURIPathComponent(String s, String slashSequence, String atSequence) {
        if ("".equals(s)) {
            return s;
        }
        URI uri;
        try {
            // fake scheme so that a colon is not mistaken as a scheme
            uri = new URI("x", s, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Illegal characters in: " + s, e);
        }
        String r = uri.toASCIIString().substring(2);
        // replace reserved characters ;:$&+=?/[]@
        // FIXME: find a better way to do this...
        r = r.replace(";", "%3B");
        r = r.replace(":", "%3A");
        r = r.replace("$", "%24");
        r = r.replace("&", "%26");
        r = r.replace("+", "%2B");
        r = r.replace("=", "%3D");
        r = r.replace("?", "%3F");
        r = r.replace("[", "%5B");
        r = r.replace("]", "%5D");
        if (atSequence != null) {
            r = r.replace("@", atSequence);
        }
        if (slashSequence != null) {
            r = r.replace("/", slashSequence);
        }
        return r;
    }

    public static String unquoteURIPathComponent(String s) {
        if ("".equals(s)) {
            return s;
        }
        URI uri;
        try {
            uri = new URI("http://x/" + s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Illegal characters in: " + s, e);
        }
        String path = uri.getPath();
        if (path.startsWith("/") && !s.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

}
