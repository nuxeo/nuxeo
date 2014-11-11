/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryReaderImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Event service configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXAuditEventsService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.audit.service.NXAuditEventsService");

    private static final String EVENT_EXT_POINT = "event";

    private static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    private static final String ADAPTER_POINT = "adapter";

    /**
     * If passed as true on the event properties, event not logged
     *
     * @since 5.7
     */
    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    private static final String BACKEND_EXT_POINT = "backend";
    
    protected static final Log log = LogFactory.getLog(NXAuditEventsService.class);

    protected final Set<ExtendedInfoDescriptor> extendedInfoDescriptors = new HashSet<ExtendedInfoDescriptor>();

    // the adapters that will injected in the EL context for extended
    // information
    protected final Set<AdapterDescriptor> documentAdapters = new HashSet<AdapterDescriptor>();

    protected final Set<String> eventNames = new HashSet<String>();

    protected AuditBackend backend;    

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        backend.deactivate();
        super.deactivate(context);
    }
        
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doRegisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doRegisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doRegisterAdapter((AdapterDescriptor) contribution);
        } else if (extensionPoint.equals(BACKEND_EXT_POINT)) {
            doRegisterBackend((AuditBackendDescriptor)contribution);
        }
    }

    protected void doRegisterBackend(AuditBackendDescriptor desc) {
        if (backend!=null) {
            try {
                backend.deactivate();
            } catch (Exception e) {
                log.error("Unable to properly deactivate previous backend");
            }            
        }
        try {
            backend = desc.newInstance();
        } catch (Exception e ) {
            log.error("Unable to instanciate Backend", e);
            return;
        }       
        try {
            backend.activate(this);
        } catch (Exception e) {
            log.error("Unable to init Backend", e);
        }
    }
    
    protected void doRegisterEvent(EventDescriptor desc) {
        String eventName = desc.getName();
        boolean eventEnabled = desc.getEnabled();
        if (eventEnabled) {
            eventNames.add(eventName);
            if (log.isDebugEnabled()) {
                log.debug("Registered event: " + eventName);
            }
        } else if (eventNames.contains(eventName) && !eventEnabled) {
            doUnregisterEvent(desc);
        }
    }

    protected void doRegisterExtendedInfo(ExtendedInfoDescriptor desc) {
        if (log.isDebugEnabled()) {
            log.debug("Registered extended info mapping : " + desc.getKey());
        }
        extendedInfoDescriptors.add(desc);
    }

    protected void doRegisterAdapter(AdapterDescriptor desc) {
        if (log.isDebugEnabled()) {
            log.debug("Registered adapter : " + desc.getName());
        }
        documentAdapters.add(desc);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doUnregisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doUnregisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doUnregisterAdapter((AdapterDescriptor) contribution);
        }
    }

    protected void doUnregisterEvent(EventDescriptor desc) {
        eventNames.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered event: " + desc.getName());
        }
    }

    protected void doUnregisterExtendedInfo(ExtendedInfoDescriptor desc) {
        // FIXME: this doesn't look right
        extendedInfoDescriptors.remove(desc.getKey());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered extended info: " + desc.getKey());
        }
    }

    protected void doUnregisterAdapter(AdapterDescriptor desc) {
        // FIXME: this doesn't look right
        documentAdapters.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered adapter: " + desc.getName());
        }
    }

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.getCanonicalName().equals(
                DocumentHistoryReader.class.getCanonicalName())) {
            return adapter.cast(new DocumentHistoryReaderImpl());
        } else {
            if (backend!=null) {
                return adapter.cast(backend);    
            } else {
                log.error("Can not provide service " + adapter.getCanonicalName() + " since backend is undefined");
                return null;
            }            
        }
    }

    public Set<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return extendedInfoDescriptors;
    }

    public Set<AdapterDescriptor> getDocumentAdapters() {
        return documentAdapters;
    }

    public AuditBackend getBackend() {
        return backend;
    }
    
}
