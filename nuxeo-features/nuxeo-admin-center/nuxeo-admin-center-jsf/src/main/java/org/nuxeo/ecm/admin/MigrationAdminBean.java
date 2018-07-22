/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationDescriptor;
import org.nuxeo.runtime.migration.MigrationDescriptor.MigrationStepDescriptor;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;
import org.nuxeo.runtime.migration.MigrationServiceImpl;

/**
 * Seam bean that wraps the {@link MigrationService} service to provide a JSF admin UI.
 */
@Name("migrationAdmin")
@Scope(CONVERSATION)
public class MigrationAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public List<Map<String, Object>> getMigrationInfos() {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        List<Map<String, Object>> migrationInfos = new ArrayList<>();

        Map<String, MigrationDescriptor> descriptors = ((MigrationServiceImpl) migrationService).getMigrationDescriptors();
        descriptors.values().forEach(descr -> {
            MigrationStatus status = migrationService.getStatus(descr.getId());
            Map<String, Object> migrationInfo = new HashMap<>();
            migrationInfo.put("id", descr.getId());
            migrationInfo.put("descriptor", descr);
            migrationInfo.put("status", status);
            if (!status.isRunning()) {
                // compute available steps
                String state = status.getState();
                List<MigrationStepDescriptor> steps = new ArrayList<>();
                for (MigrationStepDescriptor step : descr.getSteps().values()) {
                    if (step.getFromState().equals(state)) {
                        steps.add(step);
                    }
                }
                // sort steps by id
                Collections.sort(steps, (a, b) -> a.getId().compareTo(b.getId()));
                migrationInfo.put("steps", steps);
            }
            migrationInfos.add(migrationInfo);
        });
        // sort migrationInfos by id
        Collections.sort(migrationInfos, (a, b) -> ((String) a.get("id")).compareTo((String) b.get("id")));

        return migrationInfos;
    }

    public void probeAndSetState(String id) {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        migrationService.probeAndSetState(id);
    }

    public void runStep(String id, String step) {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        migrationService.runStep(id, step);
    }

}
