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
public class ExceptionHandlingComponent extends DefaultComponent implements ExceptionHandlingService {

    protected NuxeoExceptionHandler exceptionHandler;

    protected final NuxeoExceptionHandlerParameters exceptionHandlerParameters = new NuxeoExceptionHandlerParameters();

    public enum ExtensionPoint {
        exceptionhandler, errorhandlers, requestdump, listener
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        ExtensionPoint ep = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (ep) {
        case exceptionhandler:
            ExceptionHandlerDescriptor ehd = (ExceptionHandlerDescriptor) contribution;
            exceptionHandler = newInstance(ehd.getKlass());
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
            RequestDumper dumper = newInstance(rdd.getKlass());
            List<String> attributes = rdd.getAttributes();
            dumper.setNotListedAttributes(attributes);
            exceptionHandlerParameters.setRequestDumper(dumper);
            break;
        case listener:
            ListenerDescriptor ld = (ListenerDescriptor) contribution;
            exceptionHandlerParameters.setListener(newInstance(ld.getKlass()));
            break;
        default:
            throw new RuntimeException("error in exception handling configuration");
        }
    }

    protected <T> T newInstance(Class<T> klass) {
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws IOException, ServletException {
        exceptionHandler.handleException(request, response, t);
    }

    @Override
    public NuxeoExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

}
