/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.ui.web.cache.LRUCachingMap;

/**
 * Cache for content views, handling cache keys set on content views.
 * <p>
 * Each content view instance will be cached if its cache key is not null. Each instance will be cached using the cache
 * key so its state is restored. Also handles refresh of caches when receiving events configured on the content view.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Default cache size, set to 5 instances per content view
     */
    public static final Integer DEFAULT_CACHE_SIZE = Integer.valueOf(5);

    protected final Map<String, String> namedCacheKeys = new HashMap<>();

    protected final Map<String, ContentView> namedContentViews = new HashMap<>();

    protected final Map<String, Map<String, ContentView>> cacheInstances = new HashMap<>();

    /**
     * Map holding content view names that need their page provider to be refreshed for a given event
     */
    protected final Map<String, Set<String>> refreshEventToContentViewName = new HashMap<>();

    /**
     * Map holding content view names that need their page provider to be reset for a given event
     */
    protected final Map<String, Set<String>> resetEventToContentViewName = new HashMap<>();

    /**
     * Add given content view to the cache, resolving its cache key and initializing it with its cache size.
     * <p>
     * Since 5.7, content views with a cache size <= 0 will be cached anyhow to handle selections, and rendering is in
     * charge of forcing cache reset when re-displaying the page.
     */
    public void add(ContentView cView) {
        if (cView != null) {
            String cacheKey = cView.getCacheKey();
            if (cacheKey == null) {
                // no cache
                return;
            }
            String name = cView.getName();
            Integer cacheSize = cView.getCacheSize();
            if (cacheSize == null) {
                cacheSize = DEFAULT_CACHE_SIZE;
            }
            if (cacheSize.intValue() <= 0) {
                // if cacheSize <= 0, selection actions will not behave
                // accurately => behave as if cacheSize was 1 in this case, and
                // use a dummy cache key. Template rendering will force cache
                // refresh when rendering the page, as if content view was not
                // cached.
                cacheSize = Integer.valueOf(1);
            }

            Map<String, ContentView> cacheEntry = cacheInstances.get(name);
            if (cacheEntry == null) {
                cacheEntry = new LRUCachingMap<>(cacheSize.intValue());
            }
            cacheEntry.put(cacheKey, cView);
            cacheInstances.put(name, cacheEntry);
            namedCacheKeys.put(name, cacheKey);
            namedContentViews.put(name, cView);
            List<String> events = cView.getRefreshEventNames();
            if (events != null && !events.isEmpty()) {
                for (String event : events) {
                    if (refreshEventToContentViewName.containsKey(event)) {
                        refreshEventToContentViewName.get(event).add(name);
                    } else {
                        Set<String> set = new HashSet<>();
                        set.add(name);
                        refreshEventToContentViewName.put(event, set);
                    }
                }
            }
            events = cView.getResetEventNames();
            if (events != null && !events.isEmpty()) {
                for (String event : events) {
                    if (resetEventToContentViewName.containsKey(event)) {
                        resetEventToContentViewName.get(event).add(name);
                    } else {
                        Set<String> set = new HashSet<>();
                        set.add(name);
                        resetEventToContentViewName.put(event, set);
                    }
                }
            }
        }
    }

    /**
     * Returns cached content view with given name, or null if not found.
     */
    public ContentView get(String name) {
        ContentView cView = namedContentViews.get(name);
        if (cView != null) {
            String oldCacheKey = namedCacheKeys.get(name);
            String newCacheKey = cView.getCacheKey();
            if (newCacheKey != null && !newCacheKey.equals(oldCacheKey)) {
                Map<String, ContentView> contentViews = cacheInstances.get(name);
                if (contentViews.containsKey(newCacheKey)) {
                    cView = contentViews.get(newCacheKey);
                    // refresh named caches
                    namedCacheKeys.put(name, newCacheKey);
                    namedContentViews.put(name, cView);
                } else {
                    // cache not here or expired => return null
                    return null;
                }
            }
        }
        return cView;
    }

    /**
     * Refresh page providers for content views in the cache with given name.refreshEventToContentViewName
     * <p>
     * Other contextual information set on the content view and the page provider will be kept.
     */
    public void refresh(String contentViewName, boolean rewind) {
        ContentView cv = namedContentViews.get(contentViewName);
        if (cv != null) {
            if (rewind) {
                cv.refreshAndRewindPageProvider();
            } else {
                cv.refreshPageProvider();
            }
        }
        Map<String, ContentView> instances = cacheInstances.get(contentViewName);
        if (instances != null) {
            for (ContentView cView : instances.values()) {
                // avoid refreshing twice the same content view, see NXP-13604
                if (cView != null && !cView.equals(cv)) {
                    if (rewind) {
                        cView.refreshAndRewindPageProvider();
                    } else {
                        cView.refreshPageProvider();
                    }
                }
            }
        }
    }

    /**
     * Resets page providers for content views in the cache with given name.
     * <p>
     * Other contextual information set on the content view will be kept.
     */
    public void resetPageProvider(String contentViewName) {
        ContentView cv = namedContentViews.get(contentViewName);
        if (cv != null) {
            cv.resetPageProvider();
        }
        Map<String, ContentView> instances = cacheInstances.get(contentViewName);
        if (instances != null) {
            for (ContentView cView : instances.values()) {
                if (cView != null) {
                    cView.resetPageProvider();
                }
            }
        }
    }

    /**
     * Resets page providers aggregates.
     *
     * @since 6.0
     */
    public void resetPageProviderAggregates(String contentViewName) {
        ContentView cv = namedContentViews.get(contentViewName);
        if (cv != null) {
            cv.resetPageProviderAggregates();
        }
        Map<String, ContentView> instances = cacheInstances.get(contentViewName);
        if (instances != null) {
            for (ContentView cView : instances.values()) {
                if (cView != null) {
                    cView.resetPageProviderAggregates();
                }
            }
        }
    }

    /**
     * Refresh page providers for content views having declared given event as a refresh event.
     * <p>
     * Other contextual information set on the content view and the page provider will be kept.
     */
    public void refreshOnEvent(String eventName) {
        if (eventName != null) {
            Set<String> contentViewNames = refreshEventToContentViewName.get(eventName);
            if (contentViewNames != null) {
                for (String contentViewName : contentViewNames) {
                    refresh(contentViewName, false);
                }
            }
        }
    }

    /**
     * Resets page providers for content views having declared given event as a reset event.
     * <p>
     * Other contextual information set on the content view will be kept.
     */
    public void resetPageProviderOnEvent(String eventName) {
        if (eventName != null) {
            Set<String> contentViewNames = resetEventToContentViewName.get(eventName);
            if (contentViewNames != null) {
                for (String contentViewName : contentViewNames) {
                    resetPageProvider(contentViewName);
                }
            }
        }
    }

    /**
     * Resets all cached information for given content view.
     */
    public void reset(String contentViewName) {
        namedContentViews.remove(contentViewName);
        namedCacheKeys.remove(contentViewName);
        cacheInstances.remove(contentViewName);
    }

    /**
     * Resets all cached information for all content views
     */
    public void resetAllContent() {
        namedContentViews.clear();
        namedCacheKeys.clear();
        cacheInstances.clear();
    }

    /**
     * Resets all cached information for all content views, as well as configuration caches (refresh and reset events
     * linked to content views).
     */
    public void resetAll() {
        resetAllContent();
        refreshEventToContentViewName.clear();
        resetEventToContentViewName.clear();
    }

    /**
     * Iterates over all cached content view instances to refresh them.
     * <p>
     * Can be costly if some page providers need to fetch content or perform costly operations at refresh.
     *
     * @since 5.7
     */
    public void refreshAll() {
        refreshAll(false);
    }

    /**
     * Iterates over all cached content view instances to refresh them.
     * <p>
     * Can be costly if some page providers need to fetch content or perform costly operations at refresh.
     *
     * @since 5.7
     */
    public void refreshAndRewindAll() {
        refreshAll(true);
    }

    /**
     * @since 5.7
     */
    protected void refreshAll(boolean rewind) {
        Set<String> cvNames = namedContentViews.keySet();
        for (String cvName : cvNames) {
            refresh(cvName, rewind);
        }
    }

}
