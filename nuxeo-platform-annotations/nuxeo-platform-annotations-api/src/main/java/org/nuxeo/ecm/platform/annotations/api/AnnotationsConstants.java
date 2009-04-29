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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

/**
 * @author Alexandre Russel
 *
 */
public interface AnnotationsConstants {

    String DEFAULT_BASE_URI = "urn:annotation:";
    String DEFAULT_GRAPH_NAME = "annotations";
    String CODEC_PREFIX = "nxdoc";

    String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
    String A = "http://www.w3.org/2000/10/annotation-ns";
    String D = "http://purl.org/dc/elements/1.1/";
    String NX = "http://rdf.nuxeo.org/nxdoc";
    String H = "http://www.w3.org/1999/xx/http";

    String A_ANNOTATES = A + "#annotates";
    Resource a_annotates = new ResourceImpl(A_ANNOTATES);

    String A_CREATED = A + "#created";

    String A_BODY = A + "#body";
    Resource aBody = new ResourceImpl(A_BODY);

    String A_CONTEXT = A + "#context";

    String D_CREATOR = D + "creator";
    String D_DATE = D + "date";

    String H_BODY = H + "#body";

    String NX_VERSIONS = NX + "#versions";

    String NX_PROXIES = NX + "#proxies";

    String NX_ANNOTABLE = NX + "#annotable";

    String NX_COMPANY = NX + "#company";

    String NX_ACP = NX + "#acp";

    String NX_BODY_CONTENT = NX + "#body";

    Resource nx_body_content = new ResourceImpl(NX_BODY_CONTENT);

    String RDF_ABOUT = RDF + "#about";
    String RDF_TYPE = RDF + "#type";
    String RDF_ = RDF + "#_";
    String RDF_BAG = RDF + "#Bag";

    String annotationBaseUrl = "http://localhost:8080/nuxeo/Annotations/";
    String annotationBaseUrlPropertyKey = "org.nuxeo.ecm.platform.annotations.api.annotationBaseUrl";

    enum ExtensionPoint {
        uriResolver, urlPatternFilter, metadataMapper, permissionManager,
        annotabilityManager, eventListener, annotationIDGenerator, permissionMapper,
        interceptors
    }

}
