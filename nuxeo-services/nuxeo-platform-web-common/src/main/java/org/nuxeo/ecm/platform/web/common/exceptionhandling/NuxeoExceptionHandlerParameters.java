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
 *     arussel, Benjamin JALON
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingListener;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.RequestDumper;

/**
 * @author arussel, Benjamin JALON
 *
 */
public class NuxeoExceptionHandlerParameters {


    protected String bundleName;

    protected String defaultErrorPage;

    protected RequestDumper requestDumper;

    protected Log errorLog;

    protected List<ErrorHandler> handlers;

    protected ExceptionHandlingListener listener;

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public RequestDumper getRequestDumper() {
        return requestDumper;
    }

    public void setRequestDumper(RequestDumper requestDumper) {
        this.requestDumper = requestDumper;
    }

    public ExceptionHandlingListener getListener() {
        return listener;
    }

    public void setListener(ExceptionHandlingListener listener) {
        this.listener = listener;
    }

    public Log getLogger() {
        return errorLog;
    }

    public void setLoggerName(String loggerName) {
        errorLog = LogFactory.getLog(loggerName);
    }

    public List<ErrorHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ErrorHandler> handlers) {
        this.handlers = handlers;
    }

    public String getDefaultErrorPage() {
        return defaultErrorPage;
    }

    public void setDefaultErrorPage(String defaultErrorPage) {
        this.defaultErrorPage = defaultErrorPage;
    }


}
