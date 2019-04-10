package org.nuxeo.segment.io;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface SegmentIO {

    String getWriteKey();

    Map<String, String> getGlobalParameters();

    void identify(NuxeoPrincipal principal);

    void identify(NuxeoPrincipal principal, Map<String, Serializable> metadata);

    void track(NuxeoPrincipal principal, String eventName);

    void track(NuxeoPrincipal principal, String eventName,
            Map<String, Serializable> metadata);

    void flush();

    Map<String, Boolean> getIntegrations();

    SegmentIOUserFilter getUserFilters();

    Set<String> getMappedEvents();

    Map<String, List<SegmentIOMapper>> getMappers(List<String> events);

    Map<String, List<SegmentIOMapper>> getAllMappers();
}
