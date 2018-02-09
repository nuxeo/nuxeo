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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants.ExtensionPoint;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.descriptors.AnnotabilityManagerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.AnnotationIDGeneratorDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.EventListenerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.MetadataMapperDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionManagerDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.PermissionMapperDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.URLPatternFilterDescriptor;
import org.nuxeo.ecm.platform.annotations.descriptors.UriResolverDescriptor;
import org.nuxeo.ecm.platform.annotations.proxy.AnnotationServiceProxy;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Alexandre Russel
 */
public class AnnotationsComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.annotations.services.annotationServiceFactory");

    private AnnotationServiceProxy annotationServiceProxy;

    private final AnnotationConfigurationService configuration = new AnnotationConfigurationServiceImpl();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        ExtensionPoint point = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (point) {
        case uriResolver:
            UriResolver resolver = newInstance(((UriResolverDescriptor) contribution).getKlass());
            configuration.setUriResolver(resolver);
            break;
        case urlPatternFilter:
            URLPatternFilterDescriptor descriptor = (URLPatternFilterDescriptor) contribution;
            boolean order = descriptor.getOrder().equalsIgnoreCase("Allow,Deny");
            URLPatternFilter filter = new URLPatternFilter(order, descriptor.getDenies(), descriptor.getAllows());
            configuration.setFilter(filter);
            break;
        case metadataMapper:
            MetadataMapper mapper = newInstance(((MetadataMapperDescriptor) contribution).getKlass());
            configuration.setMetadataMapper(mapper);
            break;
        case permissionManager:
            PermissionManager manager = newInstance(((PermissionManagerDescriptor) contribution).getKlass());
            configuration.setPermissionManager(manager);
            break;
        case annotabilityManager:
            AnnotabilityManager annotabilityManager = newInstance(((AnnotabilityManagerDescriptor) contribution).getKlass());
            configuration.setAnnotabilityManager(annotabilityManager);
            break;
        case eventListener:
            Class<? extends EventListener> listener = ((EventListenerDescriptor) contribution).getListener();
            configuration.addListener(newInstance(listener));
            break;
        case annotationIDGenerator:
            AnnotationIDGenerator generator = newInstance(((AnnotationIDGeneratorDescriptor) contribution).getKlass());
            configuration.setIDGenerator(generator);
            break;
        case permissionMapper:
            configuration.setPermissionMapper((PermissionMapperDescriptor) contribution);
            break;
        }
    }

    protected <T> T newInstance(Class<T> klass) {
        try {
            return klass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void activate(ComponentContext context) {
    }

    @Override
    public void deactivate(ComponentContext context) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AnnotationsService.class.isAssignableFrom(adapter)) {
            return (T) getAnnotationServiceProxy();
        } else if (AnnotationConfigurationService.class.isAssignableFrom(adapter)) {
            return (T) configuration;
        }
        return null;
    }

    private AnnotationsService getAnnotationServiceProxy() {
        if (annotationServiceProxy == null) {
            annotationServiceProxy = new AnnotationServiceProxy();
            annotationServiceProxy.initialise();
        }
        return annotationServiceProxy;
    }

}
