/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Utility class that externalize means to access a document by using an URL.
 *
 * @author DM
 * @deprecated see the url service with codecs registered through extension points
 */
@Deprecated
public final class DocumentLocator {

    public static final String URL_PREFIX = "getDocument.faces?";

    public static final String PARAM_NAME = "docRef";

    public static final String CHAR_ENCODING = "UTF-8";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DocumentLocator.class);

    private DocumentLocator() {
    }

    public static String getDocumentUrl(RepositoryLocation serverLocation, DocumentRef docRef) {
        if (serverLocation == null) {
            String nullRepoName = null;
            return getDocumentUrl(nullRepoName, docRef);
        }
        return getDocumentUrl(serverLocation.getName(), docRef);
    }

    /**
     * Returns something like getDocument.faces?docRef=ServerLocationName/DocRef.
     */
    public static String getDocumentUrl(String serverLocationName, DocumentRef docRef) {

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }

        final StringBuilder urlBuf = new StringBuilder();
        urlBuf.append(URL_PREFIX);
        urlBuf.append(PARAM_NAME);
        urlBuf.append('=');
        if (serverLocationName != null) {
            // urlBuf.append(encode(serverLocation.getUri()));
            // XXX : Uses server name instead of URI
            // decoding tests fails otherwise
            urlBuf.append(encode(serverLocationName));
        }
        urlBuf.append('/');
        urlBuf.append(docRef.type());
        urlBuf.append(':');
        urlBuf.append(encode(docRef.reference().toString()));

        return urlBuf.toString();
    }

    /**
     * Encodes the given string to be safely used in an URL.
     */
    private static String encode(String txt) {
        String safetxt;
        try {
            safetxt = URLEncoder.encode(txt, CHAR_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.error(e, e);
            return null;
        }
        return safetxt;
    }

    private static String decode(String txt) {
        final String decoded;
        try {
            decoded = URLDecoder.decode(txt, CHAR_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.error(e, e);
            return null;
        }
        return decoded;
    }

    /**
     * Returns something like http://server:port/nuxeo/getDocument.xhtml?docRef=ServerLocationName/DocRef.
     */
    public static String getFullDocumentUrl(RepositoryLocation serverLocation, DocumentRef docRef) {
        String baseUrl = BaseURL.getBaseURL();
        String docUrl = getDocumentUrl(serverLocation, docRef);
        if (baseUrl != null) {
            return baseUrl + docUrl;
        }
        return docUrl;
    }

    /**
     * Returns something like http://server:port/nuxeo/getDocument.xhtml?docRef=ServerLocationName/DocRef.
     */
    public static String getFullDocumentUrl(String serverLocation, DocumentRef docRef) {
        String baseUrl = BaseURL.getBaseURL();
        String docUrl = getDocumentUrl(serverLocation, docRef);
        if (baseUrl != null) {
            return baseUrl + docUrl;
        }
        return docUrl;
    }

    /**
     * @param docUriRef in format &lt;ServerLocationName&gt;/&lt;DocRefType&gt;:&lt;doc reference&gt;
     */
    public static DocumentLocation parseDocRef(String docUriRef) throws BadDocumentUriException {
        final int pos = docUriRef.indexOf('/');
        if (pos == -1) {
            throw new BadDocumentUriException("/ delimiter not found");
        }
        String serverLocation = docUriRef.substring(0, pos);
        serverLocation = decode(serverLocation);

        final String serverLocationName = serverLocation;

        int pos2 = docUriRef.indexOf(':', pos + 1);
        if (pos2 == -1) {
            throw new BadDocumentUriException(": delimiter not found");
        }
        final String refTypeStr = docUriRef.substring(pos + 1, pos2);
        final int refType;
        try {
            refType = Integer.parseInt(refTypeStr);
        } catch (NumberFormatException e) {
            throw new BadDocumentUriException("bad refType (not a number) " + refTypeStr);
        }

        String reference = docUriRef.substring(pos2 + 1);
        reference = decode(reference);

        final DocumentRef docRef;
        if (refType == DocumentRef.ID) {
            docRef = new IdRef(reference);
        } else if (DocumentRef.PATH == refType) {
            docRef = new PathRef(reference);
        } else {
            throw new BadDocumentUriException("bad refType " + refType);
        }

        return new DocumentLocationImpl(serverLocationName, docRef);
    }

}
