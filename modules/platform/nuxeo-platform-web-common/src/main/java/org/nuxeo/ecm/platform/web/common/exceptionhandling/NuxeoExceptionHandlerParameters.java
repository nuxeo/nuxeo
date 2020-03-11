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
