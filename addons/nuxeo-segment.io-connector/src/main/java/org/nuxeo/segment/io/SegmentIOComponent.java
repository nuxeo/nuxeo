/*
 * (C) Copyright 2014-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *     Yannis JULIENNE
 */

package org.nuxeo.segment.io;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

import com.github.segmentio.Analytics;
import com.github.segmentio.AnalyticsClient;
import com.github.segmentio.flush.Flusher;
import com.github.segmentio.models.Group;
import com.github.segmentio.models.Options;
import com.github.segmentio.models.Props;
import com.github.segmentio.models.Traits;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class SegmentIOComponent extends DefaultComponent implements SegmentIO {

    protected static Log log = LogFactory.getLog(SegmentIOComponent.class);

    protected static final String DEFAULT_DEBUG_KEY = "FakeKey_ChangeMe";

    public final static String WRITE_KEY = "segment.io.write.key";

    public final static String CONFIG_EP = "config";

    public final static String MAPPER_EP = "mapper";

    public final static String INTEGRATIONS_EP = "integrations";

    public final static String FILTERS_EP = "filters";

    protected boolean debugMode = false;

    protected Map<String, SegmentIOMapper> mappers;

    protected Map<String, List<SegmentIOMapper>> event2Mappers = new HashMap<>();

    protected List<Map<String, Object>> testData = new LinkedList<>();

    protected SegmentIOConfig config;

    protected SegmentIOIntegrations integrationsConfig;

    protected SegmentIOUserFilter userFilters;

    protected Bundle bundle;

    protected Flusher flusher;

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public void activate(ComponentContext context) {
        bundle = context.getRuntimeContext().getBundle();
        mappers = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        flush();
        bundle = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_EP.equalsIgnoreCase(extensionPoint)) {
            config = (SegmentIOConfig) contribution;
        } else if (MAPPER_EP.equalsIgnoreCase(extensionPoint)) {
            SegmentIOMapper mapper = (SegmentIOMapper) contribution;
            mappers.put(mapper.name, mapper);
        } else if (INTEGRATIONS_EP.equalsIgnoreCase(extensionPoint)) {
            integrationsConfig = (SegmentIOIntegrations) contribution;
        } else if (FILTERS_EP.equalsIgnoreCase(extensionPoint)) {
            userFilters = (SegmentIOUserFilter) contribution;
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        String key = getWriteKey();
        if (DEFAULT_DEBUG_KEY.equals(key)) {
            log.info("Run Segment.io in debug mode : nothing will be sent to the server");
            debugMode = true;
        } else {
            Analytics.initialize(key);
        }
        computeEvent2Mappers();
    }

    protected void computeEvent2Mappers() {
        event2Mappers = new HashMap<String, List<SegmentIOMapper>>();
        for (SegmentIOMapper mapper : mappers.values()) {
            for (String event : mapper.events) {
                List<SegmentIOMapper> m4event = event2Mappers.get(event);
                if (m4event == null) {
                    event2Mappers.put(event, new ArrayList<SegmentIOMapper>());
                    m4event = event2Mappers.get(event);
                }
                if (!m4event.contains(mapper)) {
                    m4event.add(mapper);
                }
            }
        }
    }

    @Override
    public String getWriteKey() {
        if (config != null) {
            if (config.writeKey != null) {
                return config.writeKey;
            }
        }
        return Framework.getProperty(WRITE_KEY, DEFAULT_DEBUG_KEY);
    }

    @Override
    public Map<String, String> getGlobalParameters() {
        if (config != null) {
            if (config.parameters != null) {
                return config.parameters;
            }
        }
        return new HashMap<>();
    }

    protected Flusher getFlusher() {
        if (flusher == null) {
            try {
                AnalyticsClient client = Analytics.getDefaultClient();
                Field field = client.getClass().getDeclaredField("flusher");
                field.setAccessible(true);
                flusher = (Flusher) field.get(client);
            } catch (ReflectiveOperationException e) {
                log.error("Unable to access SegmentIO Flusher via reflection", e);
            }
        }
        return flusher;
    }

    @Override
    public void identify(NuxeoPrincipal principal) {
        identify(principal, null);
    }

    @Override
    public Map<String, Boolean> getIntegrations() {
        if (integrationsConfig != null && integrationsConfig.integrations != null) {
            return integrationsConfig.integrations;
        }
        return new HashMap<>();
    }

    /**
     * Build common options for identify and track calls. These options contains the configured integrations values and
     * the current timestamp.
     *
     * @return the builded {@link Options} object
     */
    protected Options buildOptions() {
        Options options = new Options();
        for (Entry<String, Boolean> integration : getIntegrations().entrySet()) {
            options.setIntegration(integration.getKey(), integration.getValue());
        }
        return options.setTimestamp(new DateTime());
    }

    @Override
    public void identify(NuxeoPrincipal principal, Map<String, Serializable> metadata) {

        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal, metadata);

        if (!mustTrackprincipal(wrapper.getUserId())) {
            if (log.isDebugEnabled()) {
                log.debug("Skip user " + principal.getName());
            }
            return;
        }

        if (debugMode) {
            if (log.isInfoEnabled()) {
                log.info("send identify for " + wrapper.getUserId() + " with meta : " + metadata.toString());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("send identify with " + metadata.toString());
            }
            Traits traits = new Traits();
            traits.putAll(wrapper.getMetadata());
            Options options = buildOptions();
            if (Framework.isTestModeSet()) {
                pushForTest("identify", wrapper.getUserId(), traits, options);
            } else {
                Analytics.getDefaultClient().identify(wrapper.getUserId(), traits, options);
            }

            Map<String, Serializable> groupMeta = wrapper.getGroupMetadata();
            if (groupMeta.size() > 0 && groupMeta.containsKey("id")) {
                Traits gtraits = new Traits();
                gtraits.putAll(groupMeta);
                group((String) groupMeta.get("id"), wrapper.getUserId(), gtraits, options);
            } else {
                // automatic grouping
                if (principal.getCompany() != null) {
                    group(principal.getCompany(), wrapper.getUserId(), null, options);
                } else if (wrapper.getMetadata().get("company") != null) {
                    group((String) wrapper.getMetadata().get("company"), wrapper.getUserId(), null, options);
                }
            }
        }
    }

    protected void group(String groupId, String userId, Traits traits, Options options) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }

        if (Framework.isTestModeSet()) {
            pushForTest("group", userId, traits, options);
        } else {
            Flusher flusher = getFlusher();
            if (flusher != null) {
                Group grp = new Group(userId, groupId, traits, options);
                flusher.enqueue(grp);
            } else {
                log.warn("Can not use Group API");
            }
        }
    }

    protected Map<String, Object> pushForTest(String action, String userId, Map<String, Object> metadata,
            Options options) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put(SegmentIODataWrapper.PRINCIPAL_KEY, userId);
        if (metadata != null) {
            data.putAll(metadata);
        }
        if (options != null) {
            data.put("options", options);
        }
        testData.add(data);
        return data;
    }

    protected void pushForTest(String action, String userId, String eventName, Map<String, Object> metadata,
            Options options) {
        Map<String, Object> data = pushForTest(action, userId, metadata, options);
        data.put("eventName", eventName);
    }

    public List<Map<String, Object>> getTestData() {
        return testData;
    }

    public boolean mustTrackprincipal(String principalName) {
        SegmentIOUserFilter filter = getUserFilters();
        if (filter == null) {
            return true;
        }
        return filter.canTrack(principalName);
    }

    @Override
    public void track(NuxeoPrincipal principal, String eventName, Map<String, Serializable> metadata) {
        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal, metadata);
        Props properties = generateProperties(wrapper, ACTIONS.track.name(), eventName);
        if (properties != null) {
            Analytics.getDefaultClient().track(wrapper.getUserId(), eventName, properties, buildOptions());
        }
    }

    /**
     * Generates a Analytics Props object. If user is ignored, or the execution in a test context the return is null and
     * has been handled as app log.
     *
     * @return a filled Props object if the object has to be send for real.
     */
    protected Props generateProperties(SegmentIODataWrapper wrapper, String action, String name) {
        if (!mustTrackprincipal(wrapper.getUserId())) {
            if (log.isDebugEnabled()) {
                log.debug("Skip user " + wrapper.getUserId());
            }
            return null;
        }

        if (debugMode) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Send %s for %s user : %s with meta : %s", action, name, wrapper.getUserId(),
                        wrapper.getMetadata().toString()));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Send %s with %s", action, wrapper.getMetadata().toString()));
            }
            Props eventProperties = new Props();
            eventProperties.putAll(wrapper.getMetadata());
            if (Framework.isTestModeSet()) {
                pushForTest(action, wrapper.getUserId(), name, eventProperties, buildOptions());
            } else {
                return eventProperties;
            }
        }

        return null;
    }

    @Override
    public void screen(NuxeoPrincipal principal, String screen, Map<String, Serializable> metadata) {
        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal, metadata);
        Props properties = generateProperties(wrapper, ACTIONS.screen.name(), screen);
        if (properties != null) {
            Analytics.getDefaultClient().screen(wrapper.getUserId(), screen, properties, buildOptions());
        }
    }

    @Override
    public void page(NuxeoPrincipal principal, String name, Map<String, Serializable> metadata) {
        this.page(principal, name, null, metadata);
    }

    @Override
    public void page(NuxeoPrincipal principal, String name, String category, Map<String, Serializable> metadata) {
        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal, metadata);
        Props properties = generateProperties(wrapper, ACTIONS.page.name(), name);
        if (properties != null) {
            Analytics.getDefaultClient().page(wrapper.getUserId(), name, category, properties, buildOptions());
        }
    }

    @Override
    public void flush() {
        if (!debugMode) {
            // only flush if Analytics was actually initialized
            Analytics.flush();
        }
    }

    @Override
    public Map<String, List<SegmentIOMapper>> getMappers(List<String> events) {
        Map<String, List<SegmentIOMapper>> targetMappers = new HashMap<String, List<SegmentIOMapper>>();
        for (String event : events) {
            if (event2Mappers.containsKey(event)) {
                targetMappers.put(event, event2Mappers.get(event));
            }
        }
        return targetMappers;
    }

    @Override
    public Set<String> getMappedEvents() {
        return event2Mappers.keySet();
    }

    @Override
    public Map<String, List<SegmentIOMapper>> getAllMappers() {
        return event2Mappers;
    }

    @Override
    public SegmentIOUserFilter getUserFilters() {
        return userFilters;
    }

    @Override
    public boolean isDebugMode() {
        return debugMode;
    }
}
