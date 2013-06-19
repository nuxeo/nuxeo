/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Constants {

    public static final String CTYPE_AUTOMATION = "application/json+nxautomation";

    public static final String CTYPE_ENTITY = "application/json+nxentity";

    public static final String CTYPE_MULTIPART_RELATED = "multipart/related"; // for
                                                                                // blobs
                                                                                // upload

    public static final String CTYPE_MULTIPART_MIXED = "multipart/mixed"; // for
                                                                            // blobs
                                                                            // download

    public static final String REQUEST_ACCEPT_HEADER = CTYPE_ENTITY + ", */*";

    public static final String CTYPE_REQUEST = "application/json+nxrequest; charset=UTF-8";

    public static final String CTYPE_REQUEST_NOCHARSET = "application/json+nxrequest";

    public static final String KEY_ENTITY_TYPE = "entity-type";

    /**
     * Header to specify a comma separated list of schemas to be included in
     * the returned doc.
     * <p>
     * If the header is not specified, the default properties are returned (the
     * minimal document properties: common, dublincore, file). To specify all the schemas you can use the
     * <code>*</code> as value. Example:
     *
     * <pre>
     * X-NXDocumentProperties: *
     * X-NXDocumentProperties: dublincore, file
     * </pre>
     */
    public static final String HEADER_NX_SCHEMAS = "X-NXDocumentProperties";

    /**
     * Header to inform the server that no return entity is wanted. It must be
     * <code>true</code> or <code>false</code>. If not specified, false
     * will be used by default.
     * <p>
     * This can be used to avoid the server sending back the response entity to
     * the client - the operation will be treated as a void operation.
     * <p>
     * For example the operation <code>Blob.Attach</code> returns back the
     * attached blob. This may generate a lot of network traffic that is not
     * needed by the client (sending back the same blob as the one sent by the
     * client as the operation input). In such situation you should set this
     * header to true.
     */
    public static final String HEADER_NX_VOIDOP = "X-NXVoidOperation";

    private Constants() {
    }

}
