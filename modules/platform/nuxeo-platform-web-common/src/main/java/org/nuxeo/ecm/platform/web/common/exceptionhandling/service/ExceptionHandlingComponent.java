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
 *     Benjamin JALON
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandlerParameters;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandlersDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ExceptionHandlerDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ListenerDescriptor;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.RequestDumpDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component for exception handling configuration.
 */
public class ExceptionHandlingComponent extends DefaultComponent implements ExceptionHandlingService {

    protected static final NuxeoExceptionHandler DEFAULT_EXCEPTION_HANDLER = new DefaultNuxeoExceptionHandler();

    protected NuxeoExceptionHandler exceptionHandler = DEFAULT_EXCEPTION_HANDLER;

    public enum ExtensionPoint {
        exceptionhandler, errorhandlers, requestdump, listener
    }

    @Override
    public void start(ComponentContext context) {
        this.<ExceptionHandlerDescriptor> getRegistryContribution(ExtensionPoint.exceptionhandler.name())
            .ifPresentOrElse(desc -> exceptionHandler = newInstance(desc.getKlass()),
                    () -> exceptionHandler = new DefaultNuxeoExceptionHandler());
        NuxeoExceptionHandlerParameters parameters = new NuxeoExceptionHandlerParameters();
        this.<ErrorHandlersDescriptor> getRegistryContribution(ExtensionPoint.errorhandlers.name()).ifPresent(desc -> {
            parameters.setBundleName(desc.getBundle());
            parameters.setHandlers(desc.getMessages());
            parameters.setLoggerName(desc.getLoggerName());
            parameters.setDefaultErrorPage(desc.getDefaultPage());
        });
        this.<RequestDumpDescriptor> getRegistryContribution(ExtensionPoint.requestdump.name()).ifPresent(desc -> {
            RequestDumper dumper = newInstance(desc.getKlass());
            dumper.setNotListedAttributes(desc.getAttributes());
            parameters.setRequestDumper(dumper);
        });
        this.<ListenerDescriptor> getRegistryContribution(ExtensionPoint.listener.name()).ifPresent(desc -> {
            parameters.setListener(newInstance(desc.getKlass()));
        });
        exceptionHandler.setParameters(parameters);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        exceptionHandler = DEFAULT_EXCEPTION_HANDLER;
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
