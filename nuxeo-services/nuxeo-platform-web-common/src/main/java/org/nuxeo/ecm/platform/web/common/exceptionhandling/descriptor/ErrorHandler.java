/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
        return getClass().getSimpleName() + "(error=" + error + ", message=" + message + ", page=" + page + ", code="
                + code + ")";
    }

}
