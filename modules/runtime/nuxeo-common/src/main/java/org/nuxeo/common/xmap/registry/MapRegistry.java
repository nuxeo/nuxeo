/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Registry for multiple contributions identified by a unique id.
 *
 * @since 11.5
 */
public class MapRegistry extends AbstractRegistry implements Registry {

    private static final Logger log = LogManager.getLogger(MapRegistry.class);

    protected Map<String, Object> contributions = Collections.synchronizedMap(new LinkedHashMap<>());

    protected Set<String> disabled = ConcurrentHashMap.newKeySet();

    protected static final String DEFAULT_KEY = "null";

    @Override
    public void initialize() {
        contributions.clear();
        disabled.clear();
        super.initialize();
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getContributions() {
        checkInitialized();
        return (Map<String, T>) contributions.entrySet()
                                             .stream()
                                             .filter(x -> !disabled.contains(x.getKey()))
                                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                                     (v1, v2) -> v2, LinkedHashMap::new));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getContributionValues() {
        checkInitialized();
        return (List<T>) contributions.entrySet()
                                      .stream()
                                      .filter(x -> !disabled.contains(x.getKey()))
                                      .map(Map.Entry::getValue)
                                      .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getContribution(String id) {
        if (id == null) {
            id = DEFAULT_KEY;
        }
        checkInitialized();
        if (disabled.contains(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) contributions.get(id));
    }

    public Set<String> getDisabledContributions() {
        checkInitialized();
        return Collections.unmodifiableSet(disabled);
    }

    protected String computeId(Context ctx, XAnnotatedObject xObject, Element element) {
        String id = (String) xObject.getRegistryId().getValue(ctx, element);
        if (id == null) {
            // prevent NPE on map key
            id = DEFAULT_KEY;
        }
        return id;
    }

    protected boolean shouldMerge(Context ctx, XAnnotatedObject xObject, Element element, String extensionId, String id,
            Object existing) {
        if (super.shouldMerge(ctx, xObject, element, extensionId)) {
            XAnnotatedMember merge = xObject.getMerge();
            if (existing != null && xObject.getCompatWarnOnMerge() && !merge.hasValue(ctx, element)
                    && !onlyHandlesEnablement(ctx, xObject, element, true)) {
                logWarn(String.format(
                        "The contribution with id '%s' on extension '%s' has been implicitly merged: "
                                + "the compatibility mechanism on its descriptor class '%s' detected it, "
                                + "and the attribute merge=\"true\" should be added to this definition.",
                        id, extensionId, existing.getClass().getName()), extensionId, id);
            }
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);

        if (shouldRemove(ctx, xObject, element, extensionId)) {
            contributions.remove(id);
            return null;
        }

        Object contrib;
        Object existing = contributions.get(id);
        if (shouldMerge(ctx, xObject, element, extensionId, id, existing)) {
            contrib = getMergedInstance(ctx, xObject, element, existing);
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

    protected void logError(String message, Throwable t, String extensionId, String id) {
        log.error(message, t);
    }

    protected void logWarn(String message, String extensionId, String id) {
        log.warn(message);
    }

}
