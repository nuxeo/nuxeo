package org.nuxeo.segment.io;

import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface SegmentIO {

    String getWriteKey();

    void identify(NuxeoPrincipal principal);

    void identify(NuxeoPrincipal principal, Map<String, String> metadata);

    void track(NuxeoPrincipal principal, String eventName);

    void track(NuxeoPrincipal principal, String eventName,
            Map<String, String> metadata);

    void flush();
}
