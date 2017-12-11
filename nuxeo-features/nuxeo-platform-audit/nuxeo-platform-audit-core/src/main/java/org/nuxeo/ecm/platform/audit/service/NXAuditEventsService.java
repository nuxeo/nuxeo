/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryReaderImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBulkerDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditStorageDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentManager.Listener;
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

    protected final Set<ExtendedInfoDescriptor> extendedInfoDescriptors = new HashSet<>();

    protected final Map<String, List<ExtendedInfoDescriptor>> eventExtendedInfoDescriptors = new HashMap<>();

    // the adapters that will injected in the EL context for extended
    // information
    protected final Set<AdapterDescriptor> documentAdapters = new HashSet<>();

    protected final Set<String> eventNames = new HashSet<>();

    protected AuditBackend backend;

    protected AuditBackendDescriptor backendConfig = new AuditBackendDescriptor();

    protected AuditBulker bulker;

    protected AuditBulkerDescriptor bulkerConfig = new AuditBulkerDescriptor();

    protected Map<String, AuditStorageDescriptor> auditStorageDescriptors = new HashMap<>();
    
    protected Map<String, AuditStorage> auditStorages = new HashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        return backendConfig.getApplicationStartedOrder();
    }

    @Override
    public void start(ComponentContext context) {
        backend = backendConfig.newInstance(this);
        backend.onApplicationStarted();
        bulker = bulkerConfig.newInstance(backend);
        bulker.onApplicationStarted();
        // init storages after runtime was started (as we don't have started order for storages which are backends)
        Framework.getRuntime().getComponentManager().addListener(new Listener() {

            @Override
            public void afterStart(ComponentManager mgr, boolean isResume) {
                for (Entry<String, AuditStorageDescriptor> descriptor : auditStorageDescriptors.entrySet()) {
                    AuditStorage storage = descriptor.getValue().newInstance();
                    if (storage instanceof AuditBackend) {
                        ((AuditBackend) storage).onApplicationStarted();
                    }
                    auditStorages.put(descriptor.getKey(), storage);
                }
            }

            @Override
            public void afterStop(ComponentManager mgr, boolean isStandby) {
                uninstall();
            }

        });
    }

    @Override
    public void stop(ComponentContext context) {
        try {
            bulker.onApplicationStopped();
        } finally {
            backend.onApplicationStopped();
            // clear storages
            auditStorages.values().forEach(storage -> {
                if (storage instanceof AuditBackend) {
                    ((AuditBackend) storage).onApplicationStopped();
                }
            });
            auditStorages.clear();
        }
    }

    protected void doRegisterAdapter(AdapterDescriptor desc) {
        if (log.isDebugEnabled()) {
            log.debug("Registered adapter : " + desc.getName());
        }
        documentAdapters.add(desc);
    }

    protected void doRegisterEvent(EventDescriptor desc) {
        String eventName = desc.getName();
        boolean eventEnabled = desc.getEnabled();
        if (eventEnabled) {
            eventNames.add(eventName);
            if (log.isDebugEnabled()) {
                log.debug("Registered event: " + eventName);
            }
            for (ExtendedInfoDescriptor extInfoDesc : desc.getExtendedInfoDescriptors()) {
                if (extInfoDesc.getEnabled()) {
                    if (eventExtendedInfoDescriptors.containsKey(eventName)) {
                        eventExtendedInfoDescriptors.get(eventName).add(extInfoDesc);
                    } else {
                        List<ExtendedInfoDescriptor> toBeAdded = new ArrayList<>();
                        toBeAdded.add(extInfoDesc);
                        eventExtendedInfoDescriptors.put(eventName, toBeAdded);
                    }
                } else {
                    if (eventExtendedInfoDescriptors.containsKey(eventName)) {
                        eventExtendedInfoDescriptors.get(eventName).remove(extInfoDesc);
                    }
                }
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

    protected void doUnregisterAdapter(AdapterDescriptor desc) {
        // FIXME: this doesn't look right
        documentAdapters.remove(desc);
        if (log.isDebugEnabled()) {
            log.debug("Unregistered adapter: " + desc.getName());
        }
    }

    protected void doUnregisterEvent(EventDescriptor desc) {
        eventNames.remove(desc.getName());
        eventExtendedInfoDescriptors.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered event: " + desc.getName());
        }
    }

    protected void doUnregisterExtendedInfo(ExtendedInfoDescriptor desc) {
        // FIXME: this doesn't look right
        extendedInfoDescriptors.remove(desc);
        if (log.isDebugEnabled()) {
            log.debug("Unregistered extended info: " + desc.getKey());
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.getCanonicalName().equals(DocumentHistoryReader.class.getCanonicalName())) {
            return adapter.cast(new DocumentHistoryReaderImpl());
        } else {
            if (backend != null) {
                return adapter.cast(backend);
            } else {
                log.error("Can not provide service " + adapter.getCanonicalName() + " since backend is undefined");
                return null;
            }
        }
    }

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    public AuditBackend getBackend() {
        return backend;
    }

    public Set<AdapterDescriptor> getDocumentAdapters() {
        return documentAdapters;
    }

    /**
     * @since 7.4
     */
    public Map<String, List<ExtendedInfoDescriptor>> getEventExtendedInfoDescriptors() {
        return eventExtendedInfoDescriptors;
    }

    public Set<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return extendedInfoDescriptors;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doRegisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doRegisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doRegisterAdapter((AdapterDescriptor) contribution);
        } else if (contribution instanceof AuditBackendDescriptor) {
            backendConfig = (AuditBackendDescriptor)contribution;
        }  else if (contribution instanceof AuditBulkerDescriptor) {
            bulkerConfig = (AuditBulkerDescriptor)contribution;
        } else if (contribution instanceof  AuditStorageDescriptor) {
            AuditStorageDescriptor auditStorageDesc = (AuditStorageDescriptor) contribution;
            auditStorageDescriptors.put(auditStorageDesc.getId(), auditStorageDesc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doUnregisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doUnregisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doUnregisterAdapter((AdapterDescriptor) contribution);
        }
    }

    /**
     * @since 9.3
     */
    public AuditStorage getAuditStorage(String id) {
        return auditStorages.get(id);
    }

}
