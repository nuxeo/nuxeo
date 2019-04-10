/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
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
