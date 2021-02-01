/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.actions;

import static org.nuxeo.ecm.platform.actions.ActionService.FILTERS_XP;
import static org.nuxeo.ecm.platform.actions.ActionService.ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Custom registry with extra API that can lookup filter registry for embedded filters registration.
 */
public class ActionRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(ActionRegistry.class);

    private final Map<String, Action> programmaticActions = new ConcurrentHashMap<>();

    // mapping of actions by category
    protected Map<String, List<String>> categories = new ConcurrentHashMap<>();

    // mapping of types per compat category
    protected Map<String, String> typeByCategory = new ConcurrentHashMap<>();

    protected static XAnnotatedObject xFilter;

    static {
        XMap fxmap = new XMap();
        fxmap.register(DefaultActionFilter.class);
        xFilter = fxmap.getObject(DefaultActionFilter.class);
    }

    public synchronized void addAction(Action action) {
        String id = action.getId();
        if (id == null) {
            log.debug("Cannot add action with null id.");
            return;
        }
        if (log.isDebugEnabled()) {
            if (programmaticActions.containsKey(id)) {
                log.debug("Overriding action: '{}'", action);
            } else {
                log.debug("Registering action: '{}'", action);
            }
        }
        applyCompatibility(action);
        programmaticActions.put(id, action);
        setInitialized(false);
    }

    public synchronized Action removeAction(String id) {
        if (id == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Unregistering action: '{}'", id);
        }
        Action action = programmaticActions.remove(id);
        setInitialized(false);
        return action;
    }

    @Override
    public void initialize() {
        super.initialize();
        initCache();
    }

    protected void initCache() {
        categories.clear();
        contributions.entrySet()
                     .stream()
                     .filter(x -> !disabled.contains(x.getKey()))
                     .map(Map.Entry::getValue)
                     .map(ActionDescriptor.class::cast)
                     .forEach(a -> a.getCategories()
                                    .forEach(event -> categories.computeIfAbsent(event, k -> new ArrayList<>())
                                                                .add(a.getId())));
        programmaticActions.entrySet()
                           .stream()
                           .filter(x -> !disabled.contains(x.getKey()))
                           .map(Map.Entry::getValue)
                           .filter(Action::getAvailable)
                           .forEach(a -> a.getCategoryList()
                                          .forEach(event -> categories.computeIfAbsent(event, k -> new ArrayList<>())
                                                                      .add(a.getId())));
    }

    /**
     * Public initialization method to be called at component start.
     */
    public void setTypeCompatibility(List<TypeCompatibility> compats) {
        typeByCategory.clear();
        compats.forEach(compat -> compat.getCategories().forEach(cat -> typeByCategory.put(cat, compat.getType())));
    }

    protected Registry getFilterRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .getExtensionPointRegistry(ID.getName(), FILTERS_XP)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown registry for extension point '%s--%s'", ID, FILTERS_XP)));
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        super.register(ctx, xObject, element, tag);
        ActionDescriptor action = getInstance(ctx, xObject, element);
        if (action != null) {
            List<Element> innerFilters = action.getFilterElements();
            if (!innerFilters.isEmpty()) {
                Registry filterRegistry = getFilterRegistry();
                for (Element innerFilter : innerFilters) {
                    if (!innerFilter.hasAttribute("append") && !innerFilter.hasAttribute("merge")) {
                        // compat: inner filters are merged by default
                        innerFilter.setAttribute("merge", "true");
                    }
                    filterRegistry.register(ctx, xFilter, innerFilter, tag);
                }
            }
        }
    }

    @Override
    public void unregister(String tag) {
        super.unregister(tag);
        getFilterRegistry().unregister(tag);
    }

    public Action getAction(String id) {
        if (id == null) {
            return null;
        }
        if (programmaticActions.containsKey(id)) {
            return programmaticActions.get(id);
        }
        return this.<ActionDescriptor> getContribution(id).map(desc -> {
            Action action = new Action(desc);
            applyCompatibility(action);
            return action;
        }).orElse(null);
    }

    public List<Action> getActions(String category) {
        checkInitialized();
        if (category == null) {
            return Collections.emptyList();
        }
        List<String> ids = categories.get(category);
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).filter(id -> !disabled.contains(id)).map(id -> {
            if (contributions.containsKey(id)) {
                ActionDescriptor desc = (ActionDescriptor) contributions.get(id);
                Action action = new Action(desc);
                applyCompatibility(category, action);
                return action;
            } else {
                return programmaticActions.get(id);
            }
        }).sorted().collect(Collectors.toList());
    }

    protected void applyCompatibility(Action action) {
        if (action != null && action.getType() == null) {
            // iterate over all categories to apply compat
            for (String cat : action.getCategories()) {
                if (applyCompatibility(cat, action)) {
                    break;
                }
            }
        }
    }

    protected boolean applyCompatibility(String category, Action action) {
        if (category != null && action != null && action.getType() == null) {
            String type = typeByCategory.get(category);
            if (type != null) {
                action.setType(type);
                applyCustomCompatibility(type, action);
                return true;
            }
        }
        return false;
    }

    /**
     * Displays specific help messages for migration of actions.
     *
     * @since 6.0
     */
    protected boolean applyCustomCompatibility(String compatType, Action action) {
        // 6.0 BBB: home/admin tab actions migrated to widgets
        if ("admin_rest_document_link".equals(compatType) || "home_rest_document_link".equals(compatType)) {
            boolean applied = false;
            String link = action.getLink();
            if (link != null && !link.startsWith("/")) {
                action.setLink("/" + link);
                applied = true;
            }
            if (applied) {
                log.warn("Applied compatibility to action '{}', its configuration should be reviewed: "
                        + "make sure the link references an absolute path", action.getId());
                return true;
            }
        }
        return false;
    }

}
