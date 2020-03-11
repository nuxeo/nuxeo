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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author arussel
 */
@XObject("errorHandlers")
public class ErrorHandlersDescriptor {

    @XNode("@bundle")
    private String bundle;

    @XNode("@loggerName")
    private String loggerName;

    @XNode("@defaultpage")
    private String defaultPage;

    @XNodeList(value = "handlers/handler", type = ArrayList.class, componentType = ErrorHandler.class)
    private final List<ErrorHandler> messages = new ArrayList<>();

    public String getBundle() {
        return bundle;
    }

    public List<ErrorHandler> getMessages() {
        return messages;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getDefaultPage() {
        return defaultPage;
    }

}
