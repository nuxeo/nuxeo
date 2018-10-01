/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.client;

import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Constants {

    public static final String CTYPE_AUTOMATION = "application/json+nxautomation";

    public static final String CTYPE_ENTITY = MediaType.APPLICATION_JSON;

    public static final String CTYPE_MULTIPART_RELATED = "multipart/related"; // for
                                                                              // blobs
                                                                              // upload

    public static final String CTYPE_MULTIPART_MIXED = "multipart/mixed"; // for
                                                                          // blobs
                                                                          // download

    /** @since 8.4 */
    public static final String CTYPE_MULTIPART_EMPTY = "application/nuxeo-empty-list"; // for empty blobs

    public static final String REQUEST_ACCEPT_HEADER = CTYPE_ENTITY + ", */*";

    public static final String CTYPE_REQUEST = "application/json+nxrequest; charset=UTF-8";

    public static final String CTYPE_REQUEST_NOCHARSET = "application/json+nxrequest";

    public static final String KEY_ENTITY_TYPE = "entity-type";

    /**
     * Header to specify a comma separated list of schemas to be included in the returned doc.
     * <p>
     * If the header is not specified, the default properties are returned (the minimal document properties: common,
     * dublincore, file). To specify all the schemas you can use the <code>*</code> as value. Example:
     *
     * <pre>
     * X-NXDocumentProperties: *
     * X-NXDocumentProperties: dublincore, file
     * </pre>
     */
    public static final String HEADER_NX_SCHEMAS = "X-NXDocumentProperties";

    /**
     * Header to inform the server that no return entity is wanted. It must be <code>true</code> or <code>false</code>.
     * If not specified, false will be used by default.
     * <p>
     * This can be used to avoid the server sending back the response entity to the client - the operation will be
     * treated as a void operation.
     * <p>
     * For example the operation <code>Blob.Attach</code> returns back the attached blob. This may generate a lot of
     * network traffic that is not needed by the client (sending back the same blob as the one sent by the client as the
     * operation input). In such situation you should set this header to true.
     */
    public static final String HEADER_NX_VOIDOP = "X-NXVoidOperation";


    private Constants() {
    }

}
