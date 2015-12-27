/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

    private static final Log log = LogFactory.getLog(Activator.class);

    private static Activator instance;

    public static Activator getInstance() {
        return instance;
    }

    protected ServiceTracker httpServiceTracker;

    protected BundleContext context;

    protected ServiceReference pkgAdm;

    @Override
    public void start(BundleContext context) {
        instance = this;
        this.context = context;
        pkgAdm = context.getServiceReference(PackageAdmin.class.getName());
        // TODO workaround to disable service tracker on regular Nuxeo distribs until finding a better solution
        if (!"Nuxeo".equals(context.getProperty(Constants.FRAMEWORK_VENDOR))) {
            httpServiceTracker = new ServiceTracker(context, HttpService.class.getName(), this);
            httpServiceTracker.open();
        }

        ApplicationManager.getInstance().start(context);
    }

    @Override
    public void stop(BundleContext context) {
        ApplicationManager.getInstance().stop(context);

        if (httpServiceTracker != null) {
            httpServiceTracker.close();
            httpServiceTracker = null;
        }
        ServletRegistry.dispose();
        instance = null;
        context.ungetService(pkgAdm);
        pkgAdm = null;
        this.context = null;
    }

    public BundleContext getContext() {
        return context;
    }

    public PackageAdmin getPackageAdmin() {
        return (PackageAdmin) context.getService(pkgAdm);
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = context.getService(reference);
        try {
            if (service instanceof HttpService) {
                ServletRegistry.getInstance().initHttpService((HttpService) service);
            }
        } catch (ServletException | NamespaceException e) {
            throw new RuntimeException("Failed to initialize http service", e);
        }
        return service;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        try {
            if (ServletRegistry.getInstance().getHttpService() == service) {
                ServletRegistry.getInstance().initHttpService(null);
            }
        } catch (ServletException | NamespaceException e) {
            log.error("Failed to remove http service", e);
        } finally {
            context.ungetService(reference);
        }
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        try {
            if (ServletRegistry.getInstance().getHttpService() == service) {
                ServletRegistry.getInstance().initHttpService(null);
                ServletRegistry.getInstance().initHttpService((HttpService) service);
            }
        } catch (ServletException | NamespaceException e) {
            log.error("Failed to update http service", e);
        }
    }
}
