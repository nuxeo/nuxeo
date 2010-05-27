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
 */
package org.nuxeo.ecm.automation.client.jaxrs;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Constants {

    public final static String CTYPE_AUTOMATION = "application/json+nxautomation";

    public final static String CTYPE_ENTITY = "application/json+nxentity";

    public final static String CTYPE_MULTIPART_RELATED = "multipart/related"; // for blobs upload
    public final static String CTYPE_MULTIPART_MIXED = "multipart/mixed"; // for blobs download

    public final static String REQUEST_ACCEPT_HEADER = CTYPE_ENTITY+", */*";

    public final static String CTYPE_REQUEST = "application/json+nxrequest; charset=UTF-8";
    public final static String CTYPE_REQUEST_NOCHARSET = "application/json+nxrequest";

    public final static String KEY_ENTITY_TYPE = "entity-type";

    /**
     * Header to specify a comma separated list of schemas to be included in the returned doc.
     * If the header is not specified the default properties are returned (the minimal document properties)
     * To specify all the schemas you can use the <code>*</code> as value. Example:
     * <pre>
     * X-NXDocumentProperties: *
     * X-NXDocumentProperties: dublincore, file
     * </pre>
     */
    public final static String HEADER_NX_SCHEMAS = "X-NXDocumentProperties";

    /**
     * Header to inform the server that no return entity is wanted. The must be <code>true</code> or <code>false</code>.
     * If not specified false will be used  by default.
     * <p>
     * This can be used to avoid the server sending back the response entity to the client -
     * the operation will be treated as a void operation.
     * <p>
     * For example the operation <code>Blob.Attach</code> is returning back the attached blob.
     * This may generate a lot of network traffic that is not needed by the client (sending back the same blob as the one sent by the client as the operation input).
     * In such situation you should set this header to true.
     */
    public final static String HEADER_NX_VOIDOP = "X-NXVoidOperation";
}
