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
 *
 */
public class AnnotationsRepositoryComponent extends DefaultComponent {
    
    public static AnnotationsRepositoryComponent instance;
    
    protected AnnotationsRepositoryServiceImpl annotationsRepositoryService;

    protected AnnotationsRepositoryConfigurationServiceImpl confImpl;

    protected AnnotationsFulltextInjector injector;
    
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        ExtensionPoint point = Enum.valueOf(ExtensionPoint.class,
                extensionPoint);
        switch (point) {
        case documentAnnotability:
            DocumentAnnotability annotability = ((DocumentAnnotabilityDescriptor) contribution).getKlass().newInstance();
            annotationsRepositoryService.setDocumentAnnotability(annotability);
            break;
        case documentEventListener:
            AnnotatedDocumentEventListener listener = ((DocumentEventListenerDescriptor) contribution).getListener().newInstance();
            String listenerName = ((DocumentEventListenerDescriptor) contribution).getName();
            confImpl.addEventListener(listenerName, listener);
            break;
        case jcrLifecycleEventId:
            String eventId = ((EventIdDescriptor) contribution).getEventId();
            confImpl.addEventId(eventId);
            break;
        case graphManagerEventListener:
            GraphManagerEventListener graphListener = ((GraphManagerEventListenerDescriptor) contribution).getKlass().newInstance();
            confImpl.setGraphManagerEventListener(graphListener);
            break;
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        instance = this;
        annotationsRepositoryService = new AnnotationsRepositoryServiceImpl();
        confImpl = new AnnotationsRepositoryConfigurationServiceImpl();
        injector = new AnnotationsFulltextInjector();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        instance = null;
        annotationsRepositoryService.clear();
        annotationsRepositoryService = null;
        injector = null;
    }

    public AnnotationsFulltextInjector getFulltextInjector() {
        return injector;
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
