/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

/**
 * @author Alexandre Russel
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
        uriResolver, urlPatternFilter, metadataMapper, permissionManager, annotabilityManager, eventListener, annotationIDGenerator, permissionMapper, interceptors
    }

}
