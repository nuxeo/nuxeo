/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject
public class ErrorHandler {

    @XNode("@error")
    private String error;

    @XNode("@message")
    private String message;

    @XNode("@page")
    private String page;

    @XNode("@code")
    private Integer code;

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPage() {
        return page;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(error=" + error + ", message="
                + message + ", page=" + page + ", code=" + code + ")";
    }

}
