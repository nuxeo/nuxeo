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

package org.nuxeo.wss.handlers.fakews;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.wss.servlet.WSSRequest;

public class FakeWSRequest extends WSSRequest {

    public FakeWSRequest(HttpServletRequest httpRequest, String sitePath) {
        super(httpRequest, sitePath);
    }

    public String getAction() {
        String soapAction = getHttpRequest().getHeader("SOAPAction");
        if (soapAction == null) {
            return "";
        } else {
            return soapAction.trim();
        }
    }
}
