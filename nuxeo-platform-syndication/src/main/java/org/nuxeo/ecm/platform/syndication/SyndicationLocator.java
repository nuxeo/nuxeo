/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.syndication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 *
 * @author <a href="mailto:bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public final class SyndicationLocator {

    public static final String CHAR_ENCODING = "UTF-8";

    public static final String DOC_PARAM_NAME = "docRef";

    public static final String SYNDIC_PARAM_NAME = "feedType";

    public static final String QUERY_PARAM_NAME = "searchQuery";

    public static final String SEARCHTYPE_PARAM_NAME = "searchType";

    private static final String URL_PREFIX_DOCUMENT = "/getSyndicationDocument.faces?";

    private static final String URL_PREFIX_SEARCH = "/getSyndicationSearch.faces?";

    private static final String SYNDICATE_DOCUMENT = "document";

    private static final String SYNDICATE_SEARCH = "search";

    // Utility class.
    private SyndicationLocator() {
    }

    /**
     * Provides the full syndication URL for RSS/ATOM Readers.
     *
     * @param serverLocation
     * @param docRef
     * @param type
     * @return URL like
     *         /getSyndication.faces?docRef=serverLocation/Type:Id&feedType={RSS|ATOM}
     */
    public static String getSyndicationDocumentUrl(
            RepositoryLocation serverLocation, DocumentRef docRef, String type) {

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }
        /*if (null == type) {
            throw new IllegalArgumentException("null feedType");
        }*/
        final StringBuffer urlBuf = new StringBuffer();
        urlBuf.append(URL_PREFIX_DOCUMENT);
        urlBuf.append(DOC_PARAM_NAME);
        urlBuf.append("=");
        if (serverLocation != null) {
            urlBuf.append(encode(serverLocation.getName()));
        } else {
            urlBuf.append("null");
        }
        urlBuf.append("/");
        urlBuf.append(docRef.type());
        urlBuf.append(":");
        urlBuf.append(encode(docRef.reference().toString()));

        urlBuf.append("&");
        urlBuf.append(SYNDIC_PARAM_NAME);
        urlBuf.append("=");
        if (type!=null)
        	urlBuf.append(encode(type));

        return urlBuf.toString();
    }

    public static String getSyndicationDocumentUrl(NavigationContext nav) {

        RepositoryLocation serverLocation = nav.getCurrentServerLocation();
        DocumentRef docRef = nav.getCurrentDocument().getRef();

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }
        final StringBuffer urlBuf = new StringBuffer();

        if (serverLocation != null) {
            urlBuf.append(encode(serverLocation.getName()));
        } else {
            urlBuf.append("null");
        }
        urlBuf.append("/");
        urlBuf.append(docRef.type());
        urlBuf.append(":");
        urlBuf.append(encode(docRef.reference().toString()));

        return urlBuf.toString();
    }

    public static String getSyndicationSearchUrl(
            RepositoryLocation serverLocation, String searchQuery,
            String searchType, String syndicType) {

        if (null == searchQuery) {
            throw new IllegalArgumentException("null searchQuery");
        }
        if (null == searchType) {
            throw new IllegalArgumentException("null searchType");
        }
        final StringBuffer urlBuf = new StringBuffer();
        urlBuf.append(URL_PREFIX_SEARCH);
        urlBuf.append(QUERY_PARAM_NAME);
        urlBuf.append("=");
        urlBuf.append(encode(searchQuery));

        urlBuf.append("&");
        urlBuf.append(SEARCHTYPE_PARAM_NAME);
        urlBuf.append("=");
        urlBuf.append(encode(searchType));

        urlBuf.append("&");
        urlBuf.append(SYNDIC_PARAM_NAME);
        urlBuf.append("=");
        if (syndicType!=null)
        	urlBuf.append(encode(syndicType));

        return urlBuf.toString();
    }

    public static String getSyndicationServer(
            NavigationContext navigationContext) {

        final FacesContext facesContext = FacesContext.getCurrentInstance();
        String reqCtxPath;
        if (facesContext != null) {
            reqCtxPath = facesContext.getExternalContext().getRequestContextPath();
        } else {
            reqCtxPath = "http://localhost:8080/nuxeo";
        }

        return reqCtxPath;
    }

    public static String getFullSyndicationDocumentUrl(
            NavigationContext navigationContext, String type) {

        return getSyndicationServer(navigationContext)
                + getSyndicationDocumentUrl(
                        navigationContext.getCurrentServerLocation(),
                        navigationContext.getCurrentDocument().getRef(), type);
    }

    public static String getFullSyndicationUrl(String syndicationType,
            String documentLocation, Map<String, String> param) {

        final StringBuffer url = new StringBuffer();

        url.append(documentLocation);

        if (syndicationType.equals(SYNDICATE_DOCUMENT)) {
            url.append(URL_PREFIX_DOCUMENT);
        } else if (syndicationType.equals(SYNDICATE_SEARCH)) {
            url.append(URL_PREFIX_SEARCH);
        }

        Set<String> keys = param.keySet();
        for (String key : keys) {
            url.append(key);
            url.append("=");
            //url.append(encode(param.get(key)));
            url.append(param.get(key));
            url.append("&");
        }
        return url.toString();
    }

    public static String getFullSyndicationDocumentUrl(String documentLocation,
            Map<String, String> param) {
        return getFullSyndicationUrl(SYNDICATE_DOCUMENT, documentLocation, param);
    }

    public static String getFullSyndicationSearchUrl(String documentLocation,
            Map<String, String> param) {
        return getFullSyndicationUrl(SYNDICATE_SEARCH, documentLocation, param);
    }

    public static String getFullSyndicationSearchUrl(
            NavigationContext navigationContext, String query, String type,
            String syndicType) {

        return getSyndicationServer(navigationContext)
                + getSyndicationSearchUrl(
                        navigationContext.getCurrentServerLocation(), query,
                        type, syndicType);
    }

    /**
     * Encodes the given string to be safely used in an URL.
     *
     * @param txt
     * @return
     */
    private static String encode(String txt) {
        String safetxt;
        try {
            safetxt = URLEncoder.encode(txt, CHAR_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        return safetxt;
    }

}
