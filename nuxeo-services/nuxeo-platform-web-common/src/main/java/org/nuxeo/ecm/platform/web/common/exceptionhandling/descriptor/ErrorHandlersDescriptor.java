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
 *
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
    private final List<ErrorHandler> messages = new ArrayList<ErrorHandler>();

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
