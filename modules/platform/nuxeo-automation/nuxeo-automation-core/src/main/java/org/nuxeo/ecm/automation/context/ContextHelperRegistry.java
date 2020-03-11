/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for {@link ContextHelperDescriptor} descriptors.
 *
 * @since 7.3
 */
public class ContextHelperRegistry extends SimpleContributionRegistry<ContextHelperDescriptor> {

    private static final Log log = LogFactory.getLog(ContextHelperRegistry.class);

    public static final String[] RESERVED_VAR_NAMES = { "CurrentDate", "Context", "ctx", "This", "Session",
            "CurrentUser", "currentUser", "Env", "Document", "currentDocument", "Documents", "params", "input" };

    @Override
    public synchronized void addContribution(ContextHelperDescriptor contextHelperDescriptor) {
        String id = contextHelperDescriptor.getId();
        ContextHelper contextHelper = contextHelperDescriptor.getContextHelper();
        if (currentContribs.keySet().contains(id)) {
            log.warn("The context helper id/alias '" + id + " is overridden by the following helper: "
                    + contextHelper.toString());
        }
        if (Arrays.asList(RESERVED_VAR_NAMES).contains(id)) {
            log.warn("The context helper '" + contextHelper.toString() + "' cannot be registered:'" + id
                    + "' is reserved. Please use another one. The Nuxeo reserved aliases are "
                    + Arrays.toString(RESERVED_VAR_NAMES));
            return;
        }
        super.addContribution(contextHelperDescriptor);
    }

    @Override
    public String getContributionId(ContextHelperDescriptor metadataMappingDescriptor) {
        return metadataMappingDescriptor.getId();
    }

    public Map<String, ContextHelperDescriptor> getContextHelperDescriptors() {
        return currentContribs;
    }

}
