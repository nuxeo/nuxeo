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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import org.nuxeo.ecm.core.api.versioning.VersioningService;

import java.util.Map;

/**
 * @author Laurent Doguin
 * @since 5.4.2
 */
public interface ExtendableVersioningService extends VersioningService {

    /**
     * Add versioning policies
     *
     * @since 9.1
     */
    void setVersioningPolicies(Map<String, VersioningPolicyDescriptor> versioningPolicies);

    /**
     * Add versioning filters
     *
     * @since 9.1
     */
    void setVersioningFilters(Map<String, VersioningFilterDescriptor> versioningFilters);

    /**
     * Add versioning filters
     *
     * @param versioningRestrictions the restrictions to apply in versioning system
     * @since 9.1
     */
    void setVersioningRestrictions(Map<String, VersioningRestrictionDescriptor> versioningRestrictions);

}
