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

package org.nuxeo.ecm.platform.annotations.repository.service;

import org.nuxeo.ecm.platform.annotations.repository.descriptor.DocumentAnnotabilityDescriptor;
import org.nuxeo.ecm.platform.annotations.repository.descriptor.DocumentEventListenerDescriptor;
import org.nuxeo.ecm.platform.annotations.repository.descriptor.EventIdDescriptor;
import org.nuxeo.ecm.platform.annotations.repository.descriptor.GraphManagerEventListenerDescriptor;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryConstants.ExtensionPoint;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Alexandre Russel
 */
public class AnnotationsRepositoryComponent extends DefaultComponent {

    protected AnnotationsRepositoryServiceImpl annotationsRepositoryService;

    protected AnnotationsRepositoryConfigurationServiceImpl confImpl;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        ExtensionPoint point = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (point) {
        case documentAnnotability:
            DocumentAnnotability annotability = newInstance(((DocumentAnnotabilityDescriptor) contribution).getKlass());
            annotationsRepositoryService.setDocumentAnnotability(annotability);
            break;
        case documentEventListener:
            AnnotatedDocumentEventListener listener = newInstance(((DocumentEventListenerDescriptor) contribution).getListener());
            String listenerName = ((DocumentEventListenerDescriptor) contribution).getName();
            confImpl.addEventListener(listenerName, listener);
            break;
        case jcrLifecycleEventId:
            String eventId = ((EventIdDescriptor) contribution).getEventId();
            confImpl.addEventId(eventId);
            break;
        case graphManagerEventListener:
            GraphManagerEventListener graphListener = newInstance(((GraphManagerEventListenerDescriptor) contribution).getKlass());
            confImpl.setGraphManagerEventListener(graphListener);
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
        annotationsRepositoryService = new AnnotationsRepositoryServiceImpl();
        confImpl = new AnnotationsRepositoryConfigurationServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        annotationsRepositoryService.clear();
        annotationsRepositoryService = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AnnotationsRepositoryService.class.isAssignableFrom(adapter)) {
            return (T) annotationsRepositoryService;
        } else if (AnnotationsRepositoryConfigurationService.class.isAssignableFrom(adapter)) {
            return (T) confImpl;
        }
        return null;
    }
}
