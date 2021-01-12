/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_AUDIT_ENABLED_PROP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryReaderImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBulkerDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditStorageDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Event service configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXAuditEventsService extends DefaultComponent implements ComponentManager.Listener {

    private static final Logger log = LogManager.getLogger(NXAuditEventsService.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.audit.service.NXAuditEventsService");

    private static final String EVENT_EXT_POINT = "event";

    private static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    private static final String ADAPTER_POINT = "adapter";

    private static final String BACKEND_EXT_POINT = "backend";

    private static final String STORAGE_EXT_POINT = "storage";

    /**
     * If passed as true on the event properties, event not logged
     *
     * @since 5.7
     */
    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    protected final Map<String, List<ExtendedInfoDescriptor>> eventExtendedInfoDescriptors = new HashMap<>();

    protected final Set<String> eventNames = new HashSet<>();

    protected AuditBackend backend;

    protected static final AuditBackendDescriptor DEFAULT_BACKEND_CONFIG = new AuditBackendDescriptor();

    /**
     * @deprecated since 10.10, audit bulker is now handled with nuxeo-stream, no replacement
     */
    @Deprecated(since = "10.10")
    protected AuditBulker bulker;

    /**
     * @deprecated since 10.10, audit bulker is now handled with nuxeo-stream, no replacement
     */
    @Deprecated(since = "10.10")
    protected AuditBulkerDescriptor bulkerConfig = new AuditBulkerDescriptor();

    protected Map<String, AuditStorage> auditStorages = new HashMap<>();

    protected AuditBackendDescriptor getAuditBackendDescriptor() {
        return this.<AuditBackendDescriptor> getRegistryContribution(BACKEND_EXT_POINT).orElse(DEFAULT_BACKEND_CONFIG);
    }

    @Override
    public int getApplicationStartedOrder() {
        return getAuditBackendDescriptor().getApplicationStartedOrder();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void start(ComponentContext context) {
        this.<EventDescriptor> getRegistryContributions(EVENT_EXT_POINT).forEach(this::doRegisterEvent);
        backend = getAuditBackendDescriptor().newInstance(this);
        backend.onApplicationStarted();
        if (Framework.isBooleanPropertyFalse(STREAM_AUDIT_ENABLED_PROP)) {
            bulker = bulkerConfig.newInstance(backend);
            bulker.onApplicationStarted();
        }
    }

    @Override
    public void afterRuntimeStart(ComponentManager mgr, boolean isResume) {
        // init storages after runtime was started (as we don't have start order for storages which are backends)
        for (AuditStorageDescriptor descriptor : this.<AuditStorageDescriptor> getRegistryContributions(
                STORAGE_EXT_POINT)) {
            AuditStorage storage = descriptor.newInstance();
            if (storage instanceof AuditBackend) {
                ((AuditBackend) storage).onApplicationStarted();
            }
            auditStorages.put(descriptor.getId(), storage);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void stop(ComponentContext context) {
        eventNames.clear();
        eventExtendedInfoDescriptors.clear();
        try {
            if (bulker != null) {
                bulker.onApplicationStopped();
            }
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

    protected void doRegisterEvent(EventDescriptor desc) {
        String eventName = desc.getName();
        eventNames.add(eventName);
        desc.getExtendedInfoDescriptors()
            .stream()
            .filter(ExtendedInfoDescriptor::getEnabled)
            .forEach(extInfoDesc -> eventExtendedInfoDescriptors.computeIfAbsent(eventName, k -> new ArrayList<>())
                                                                .add(extInfoDesc));
        log.debug("Registered event: {}", eventName);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == NXAuditEventsService.class) {
            return adapter.cast(this);
        } else if (adapter.getCanonicalName().equals(DocumentHistoryReader.class.getCanonicalName())) {
            return adapter.cast(new DocumentHistoryReaderImpl());
        } else {
            if (backend != null) {
                return adapter.cast(backend);
            } else {
                log.error("Can not provide service {} since backend is undefined", adapter::getCanonicalName);
                return null;
            }
        }
    }

    public Set<String> getAuditableEventNames() {
        return Collections.unmodifiableSet(eventNames);
    }

    public AuditBackend getBackend() {
        return backend;
    }

    public Set<AdapterDescriptor> getDocumentAdapters() {
        return new HashSet<>(getRegistryContributions(ADAPTER_POINT));
    }

    /**
     * @since 7.4
     */
    public Map<String, List<ExtendedInfoDescriptor>> getEventExtendedInfoDescriptors() {
        return Collections.unmodifiableMap(eventExtendedInfoDescriptors);
    }

    public Set<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return new HashSet<>(getRegistryContributions(EXTENDED_INFO_EXT_POINT));
    }

    /**
     * @since 9.3
     */
    public AuditStorage getAuditStorage(String id) {
        return auditStorages.get(id);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof AuditBulkerDescriptor) {
            bulkerConfig = (AuditBulkerDescriptor) contribution;
            ComponentName compName = contributor.getName();
            String message = String.format(
                    "AuditBulker on component %s is deprecated because it is now handled with nuxeo-stream, no replacement.",
                    compName);
            DeprecationLogger.log(message, "10.10");
            addRuntimeMessage(Level.WARNING, message, Source.EXTENSION, compName.getName());
        }
    }

}
