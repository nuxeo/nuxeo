/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.fprpc;

/**
 * Constants for FP-RPC protocol.
 *
 * @author Thierry Delprat
 */
public class FPRPCConts {

    public static String FP_CONTENT_TYPE_HEADER = "X-Vermeer-Content-Type";

    public static String VERMEER_CT = "application/x-vermeer-rpc";

    public static String VERMEER_ENCODED_CONTENT_TYPE = "application/x-vermeer-urlencoded";

    public static String FORM_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static String CMD_PARAM = "Cmd";

    public static String METHOD_PARAM = "method";

    public static String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static String MS_WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    // public static String MAC_FINDER_USERAGENT = "WebDAV";

    public String getMETHOD_PARAM() {
        return METHOD_PARAM;
    }

    public String getFORM_ENCODED_CONTENT_TYPE() {
        return FORM_ENCODED_CONTENT_TYPE;
    }

}
