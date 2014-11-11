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
package org.nuxeo.ecm.platform.web.common.exceptionhandling.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandlerParameters;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandlersDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ExceptionHandlerDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ListenerDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.RequestDumpDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author arussel, Benjamin JALON
 */
public class ExceptionHandlingComponent extends DefaultComponent implements
        ExceptionHandlingService {

    protected NuxeoExceptionHandler exceptionHandler;

    protected final NuxeoExceptionHandlerParameters exceptionHandlerParameters = new NuxeoExceptionHandlerParameters();

    public enum ExtensionPoint {
        exceptionhandler, errorhandlers, requestdump, listener
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        ExtensionPoint ep = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (ep) {
        case exceptionhandler:
            ExceptionHandlerDescriptor ehd = (ExceptionHandlerDescriptor) contribution;
            exceptionHandler = ehd.getKlass().newInstance();
            exceptionHandler.setParameters(exceptionHandlerParameters);
            break;
        case errorhandlers:
            ErrorHandlersDescriptor md = (ErrorHandlersDescriptor) contribution;
            exceptionHandlerParameters.setBundleName(md.getBundle());
            exceptionHandlerParameters.setHandlers(md.getMessages());
            exceptionHandlerParameters.setLoggerName(md.getLoggerName());
            exceptionHandlerParameters.setDefaultErrorPage(md.getDefaultPage());
            break;
        case requestdump:
            RequestDumpDescriptor rdd = (RequestDumpDescriptor) contribution;
            RequestDumper dumper = rdd.getKlass().newInstance();
            List<String> attributes = rdd.getAttributes();
            dumper.setNotListedAttributes(attributes);
            exceptionHandlerParameters.setRequestDumper(dumper);
            break;
        case listener:
            ListenerDescriptor ld = (ListenerDescriptor) contribution;
            exceptionHandlerParameters.setListener(ld.getKlass().newInstance());
            break;
        default:
            throw new RuntimeException(
                    "error in exception handling configuration");
        }
    }

    public void forwardToErrorPage(final HttpServletRequest request,
            final HttpServletResponse response, final Throwable t)
            throws IOException, ServletException {
        exceptionHandler.handleException(request, response, t);
    }

    public NuxeoExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }



}
