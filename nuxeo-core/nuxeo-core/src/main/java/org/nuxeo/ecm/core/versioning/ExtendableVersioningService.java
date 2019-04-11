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

import java.util.Map;

/**
 * Class implementing this interface will be able to use contribution from the versioningRules extension point.
 *
 * @author Laurent Doguin
 * @since 5.4.2
 */
public interface ExtendableVersioningService extends VersioningService {

    /**
     * @return A Map containing the versioning rule for specific types.
     * @deprecated since 9.1, use policy and filter contributions instead
     */
    @Deprecated
    Map<String, VersioningRuleDescriptor> getVersioningRules();

    /**
     * Add versioning rules for specific types.
     *
     * @deprecated since 9.1, use policy and filter contributions instead
     */
    @Deprecated
    void setVersioningRules(Map<String, VersioningRuleDescriptor> versioningRules);

    /**
     * Set the default versioning rule for all document type.
     *
     * @deprecated since 9.1, use policy and filter contributions instead
     */
    @Deprecated
    void setDefaultVersioningRule(DefaultVersioningRuleDescriptor defaultVersioningRule);

    /**
     * Add versioning policies
     *
     * @param versioningPolicies
     * @since 9.1
     */
    void setVersioningPolicies(Map<String, VersioningPolicyDescriptor> versioningPolicies);

    /**
     * Add versioning filters
     *
     * @param versioningFilters
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
