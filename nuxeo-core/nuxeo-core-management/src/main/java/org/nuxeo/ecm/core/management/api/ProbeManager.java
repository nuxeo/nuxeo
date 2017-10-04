/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.management.api;

import java.util.Collection;

import org.nuxeo.ecm.core.management.probes.HealthCheckProbesDescriptor;
import org.nuxeo.ecm.core.management.statuses.HealthCheckResult;

public interface ProbeManager extends ProbeRunnerMBean {

    Collection<ProbeInfo> getAllProbeInfos();

    Collection<ProbeInfo> getInSuccessProbeInfos();

    Collection<ProbeInfo> getInFailureProbeInfos();

    ProbeInfo runProbe(ProbeInfo probe);

    ProbeInfo runProbe(String name);

    ProbeInfo getProbeInfo(String name);

    ProbeInfo getProbeInfo(Class<? extends Probe> probeClass);

    /**
     * List of probes evaluated for a health check
     *
     * @since 9.3
     */
    Collection<ProbeInfo> getHealthCheckProbes();

    /**
     * Return the status of all the probes evaluated for a healthCheck. The probes are run if the last run was more than
     * a short while ago
     *
     * @since 9.3
     */
    HealthCheckResult getOrRunHealthChecks();

    /**
     * This probe is taken into account for the healthCheck
     *
     * @since 9.3
     */
    void registerProbeForHealthCheck(HealthCheckProbesDescriptor descriptor);

    /**
     * Return the status of the given probe. The probe is run only if the last run was more than a short while ago
     *
     * @since 9.3
     */
    HealthCheckResult getOrRunHealthCheck(String probe);
}
