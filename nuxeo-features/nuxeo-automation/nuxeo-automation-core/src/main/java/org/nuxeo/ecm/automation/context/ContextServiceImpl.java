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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 7.3
 */
public class ContextServiceImpl implements ContextService {

    protected final ContextHelperRegistry contextHelperRegistry;

    public ContextServiceImpl(ContextHelperRegistry contextHelperRegistry) {
        this.contextHelperRegistry = contextHelperRegistry;
    }

    @Override
    public Map<String, ContextHelper> getHelperFunctions() {
        Map<String, ContextHelper> contextHelpers = new HashMap<>();
        Map<String, ContextHelperDescriptor> contextHelperDescriptors = contextHelperRegistry.getContextHelperDescriptors();
        for (ContextHelperDescriptor contextHelperDescriptor : contextHelperDescriptors.values()) {
            if (contextHelperDescriptor.isEnabled()) {
                contextHelpers.put(contextHelperDescriptor.getId(), contextHelperDescriptor.getContextHelper());
            }
        }
        return contextHelpers;
    }

}
