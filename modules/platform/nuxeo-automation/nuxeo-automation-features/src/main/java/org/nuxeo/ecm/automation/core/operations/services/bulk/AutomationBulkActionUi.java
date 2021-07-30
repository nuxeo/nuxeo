/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Bulk Action that runs an automation operation dedicated for UI
 *
 * @since 11.5
 */
public class AutomationBulkActionUi extends AbstractAutomationBulkAction {

    public static final String ACTION_NAME = "automationUi";

    protected static final String QUERY_LIMIT_PREFIX = "org.nuxeo.ecm.automation.ui.bulk.queryLimit.";

    public static int getQueryLimit(String operationId) {
        return Framework.getService(ConfigurationService.class).getInteger(QUERY_LIMIT_PREFIX + operationId, 0);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }
}
