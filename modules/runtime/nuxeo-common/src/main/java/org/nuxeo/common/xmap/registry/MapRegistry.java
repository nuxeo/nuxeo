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
        checkInitialized();
        if (disabled.contains(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) contributions.get(id));
    }

    protected String computeId(Context ctx, XAnnotatedObject xObject, Element element) {
        String id = (String) xObject.getRegistryId().getValue(ctx, element);
        if (id == null) {
            // prevent NPE on map key
            id = "null";
        }
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);
        XAnnotatedMember remove = xObject.getRemove();
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            contributions.remove(id);
            return null;
        }
        Object contrib;
        XAnnotatedMember merge = xObject.getMerge();
        if (merge != null && Boolean.TRUE.equals(merge.getValue(ctx, element))) {
            Object contribution = contributions.get(id);
            if (contribution != null && xObject.getCompatWarnOnMerge() && !merge.hasValue(ctx, element)) {
                log.warn(
                        "The contribution with id '{}' on extension '{}' has been implicitly merged: "
                                + "the compatibility mechanism on its descriptor class '{}' detected it, "
                                + "and the attribute merge=\"true\" should be added to this definition.",
                        id, extensionId, contribution.getClass().getName());
            }
            contrib = xObject.newInstance(ctx, element, contribution);
        } else {
            contrib = xObject.newInstance(ctx, element);
        }
        contributions.put(id, contrib);
        XAnnotatedMember enable = xObject.getEnable();
        if (enable != null && enable.hasValue(ctx, element)) {
            Object enabled = enable.getValue(ctx, element);
            if (enabled != null && Boolean.FALSE.equals(enabled)) {
                disabled.add(id);
            } else {
                disabled.remove(id);
            }
        }
        return (T) contrib;
    }

}
