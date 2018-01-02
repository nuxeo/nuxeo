/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.admin.operation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.admin.permissions.AbstractPermissionsPurge;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@LocalDeploy({ "org.nuxeo.admin.center:OSGI-INF/operation-contrib.xml" })
public class TestPermissionsPurgeOperation extends AbstractPermissionsPurge {

    @Inject
    protected AutomationService automationService;

    @Override
    public void scheduleWork(List<String> usernames) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        Map<String, Serializable> params = new HashMap<>();

        params.put("usernames", StringUtils.join(usernames, ","));

        automationService.run(ctx, PermissionsPurge.ID, params);
    }
}
