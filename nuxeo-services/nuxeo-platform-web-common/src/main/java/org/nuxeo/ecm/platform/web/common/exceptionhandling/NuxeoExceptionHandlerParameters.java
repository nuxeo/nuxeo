/*
 * (C) Cop
yright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getBundleName()
     */
    public String getBundleName() {
        return bundleName;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setBundleName(java.lang.String)
     */
    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getRequestDumper()
     */
    public RequestDumper getRequestDumper() {
        return requestDumper;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setRequestDumper(org.nuxeo.ecm.platform.web.common.exceptionhandling.service.RequestDumper)
     */
    public void setRequestDumper(RequestDumper requestDumper) {
        this.requestDumper = requestDumper;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getListener()
     */
    public ExceptionHandlingListener getListener() {
        return listener;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setListener(org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingListener)
     */
    public void setListener(ExceptionHandlingListener listener) {
        this.listener = listener;
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getLogger()
     */
    public Log getLogger() {
        return errorLog;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setLoggerName(java.lang.String)
     */
    public void setLoggerName(String loggerName) {
        errorLog = LogFactory.getLog(loggerName);
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getHandlers()
     */
    public List<ErrorHandler> getHandlers() {
        return handlers;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setHandlers(java.util.List)
     */
    public void setHandlers(List<ErrorHandler> handlers) {
        this.handlers = handlers;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#getDefaultErrorPage()
     */
    public String getDefaultErrorPage() {
        return defaultErrorPage;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#setDefaultErrorPage(java.lang.String)
     */
    public void setDefaultErrorPage(String defaultErrorPage) {
        this.defaultErrorPage = defaultErrorPage;
    }

    
}