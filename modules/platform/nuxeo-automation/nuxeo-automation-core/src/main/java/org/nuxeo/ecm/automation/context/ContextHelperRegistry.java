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

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Registry for {@link ContextHelperDescriptor} descriptors.
 * <p>
 * Modified as of 11.5 to implement {@link Registry}.
 *
 * @since 7.3
 */
public class ContextHelperRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(ContextHelperRegistry.class);

    protected static final List<String> RESERVED_VAR_NAMES = List.of("CurrentDate", "Context", "ctx", "This", "Session",
            "CurrentUser", "currentUser", "Env", "Document", "currentDocument", "Documents", "params", "input");

    protected static final Collector<ContextHelperDescriptor, ?, Map<String, ContextHelper>> COLLECTOR = Collectors.toMap(
            ContextHelperDescriptor::getId, ContextHelperDescriptor::getContextHelper);

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);

        if (shouldRemove(ctx, xObject, element, extensionId)) {
            contributions.remove(id);
            return null;
        }

        if (RESERVED_VAR_NAMES.contains(id)) {
            log.error("The context helper with id '{}' cannot be registered: this identifier is reserved. "
                    + "Please use another one (reserved identifiers: {})", id, RESERVED_VAR_NAMES);
            return null;
        }

        Object contrib;
        Object existing = contributions.get(id);
        if (shouldMerge(ctx, xObject, element, extensionId, id, existing)) {
            contrib = getMergedInstance(ctx, xObject, element, existing);
            if (existing != null) {
                log.warn("The context helper id/alias '{}' is overridden by the following helper: {}", id, contrib);
            }
        } else {
            contrib = getInstance(ctx, xObject, element);
        }
        contributions.put(id, contrib);

        Boolean enable = shouldEnable(ctx, xObject, element, extensionId);
        if (enable != null) {
            if (Boolean.TRUE.equals(enable)) {
                disabled.remove(id);
            } else {
                disabled.add(id);
            }
        }

        return (T) contrib;
    }

    public Map<String, ContextHelper> getContextHelpers() {
        return this.<ContextHelperDescriptor> getContributionValues().stream().collect(COLLECTOR);
    }

}
