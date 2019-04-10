/*
 * (C) Copyright 2014-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Yannis JULIENNE
 */
package org.nuxeo.segment.io;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface SegmentIO {

    enum ACTIONS {
        track, screen, page, identify
    }

    String getWriteKey();

    Map<String, String> getGlobalParameters();

    /**
     * @since 10.2
     */
    void identify(NuxeoPrincipal principal);

    /**
     * @since 10.2
     */
    void identify(NuxeoPrincipal principal, Map<String, Serializable> metadata);

    /**
     * @since 10.2
     */
    void track(NuxeoPrincipal principal, String eventName, Map<String, Serializable> metadata);

    /**
     * @since 10.2
     */
    void screen(NuxeoPrincipal principal, String screen, Map<String, Serializable> metadata);

    /**
     * @since 10.2
     */
    void page(NuxeoPrincipal principal, String name, Map<String, Serializable> metadata);

    /**
     * @since 10.2
     */
    void page(NuxeoPrincipal principal, String name, String category, Map<String, Serializable> metadata);

    void flush();

    /**
     * @since 10.2
     */
    boolean mustTrackprincipal(String principalName);

    Map<String, Boolean> getIntegrations();

    SegmentIOUserFilter getUserFilters();

    Set<String> getMappedEvents();

    Map<String, List<SegmentIOMapper>> getMappers(List<String> events);

    Map<String, List<SegmentIOMapper>> getAllMappers();

    /**
     * @return true if the service is in debug mode
     * @since 9.10
     */
    boolean isDebugMode();
}
