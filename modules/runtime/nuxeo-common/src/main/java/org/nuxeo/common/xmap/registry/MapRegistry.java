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
public class MapRegistry<T> extends AbstractRegistry<T> {

    private static final Logger log = LogManager.getLogger(MapRegistry.class);

    protected Map<String, T> contributions = Collections.synchronizedMap(new LinkedHashMap<>());

    protected Set<String> disabled = ConcurrentHashMap.newKeySet();

    protected static final String DEFAULT_KEY = "null";

    @Override
    public void initialize() {
        contributions.clear();
        disabled.clear();
        super.initialize();
    }

    public Map<String, T> getContributions() {
        checkInitialized();
        return (Map<String, T>) contributions.entrySet()
                                             .stream()
                                             .filter(x -> !disabled.contains(x.getKey()))
                                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                                     (v1, v2) -> v2, LinkedHashMap::new));
    }

    public List<T> getContributionValues() {
        checkInitialized();
        return (List<T>) contributions.entrySet()
                                      .stream()
                                      .filter(x -> !disabled.contains(x.getKey()))
                                      .map(Map.Entry::getValue)
                                      .collect(Collectors.toList());
    }

    public Optional<T> getContribution(String id) {
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

    protected String computeId(Context ctx, XAnnotatedObject<T> xObject, Element element) {
        String id = (String) xObject.getRegistryId().getValue(ctx, element);
        if (id == null) {
            // prevent NPE on map key
            id = DEFAULT_KEY;
        }
        return id;
    }

    protected boolean shouldMerge(Context ctx, XAnnotatedObject<T> xObject, Element element, String extensionId, String id,
            Object existing) {
        if (super.shouldMerge(ctx, xObject, element, extensionId)) {
            XAnnotatedMember merge = xObject.getMerge();
            if (existing != null && xObject.getCompatWarnOnMerge() && !merge.hasValue(ctx, element)
                    && !onlyHandlesEnablement(ctx, xObject, element, true)) {
                log.warn(
                        "The contribution with id '{}' on extension '{}' has been implicitly merged: "
                                + "the compatibility mechanism on its descriptor class '{}' detected it, "
                                + "and the attribute merge=\"true\" should be added to this definition.",
                        id, extensionId, existing.getClass().getName());
            }
            return true;
        }
        return false;
    }

    @Override
    protected T doRegister(Context ctx, XAnnotatedObject<T> xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);

        if (shouldRemove(ctx, xObject, element, extensionId)) {
            contributions.remove(id);
            return null;
        }

        T contrib;
        T existing = contributions.get(id);
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

}
