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

    public final static String CTYPE_MULTIPART = "multipart/related"; // for blobs

    public final static String REQUEST_ACCEPT_HEADER = CTYPE_ENTITY+", */*";

    public final static String CTYPE_REQUEST = "application/json+nxrequest; charset=UTF-8";
    public final static String CTYPE_REQUEST_NOCHARSET = "application/json+nxrequest";

    public final static String KEY_ENTITY_TYPE = "entity-type";

}
