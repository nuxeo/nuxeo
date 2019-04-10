/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.segment.io;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.segment.io.extension.Group;
import org.osgi.framework.Bundle;

import com.github.segmentio.Analytics;
import com.github.segmentio.AnalyticsClient;
import com.github.segmentio.flush.Flusher;
import com.github.segmentio.models.Context;
import com.github.segmentio.models.EventProperties;
import com.github.segmentio.models.Providers;
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

    public final static String PROVIDER_EP = "providers";

    public final static String FILTERS_EP = "filters";

    protected boolean debugMode = false;

    protected Map<String, SegmentIOMapper> mappers;

    protected Map<String, List<SegmentIOMapper>> event2Mappers = new HashMap<>();

    protected List<Map<String, Object>> testData = new LinkedList<>();

    protected SegmentIOConfig config;

    protected SegmentIOProviders providersConfig;

    protected SegmentIOUserFilter userFilters;

    protected Providers providers;

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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equalsIgnoreCase(extensionPoint)) {
            config = (SegmentIOConfig) contribution;
        } else if (MAPPER_EP.equalsIgnoreCase(extensionPoint)) {
            SegmentIOMapper mapper = (SegmentIOMapper) contribution;
            mappers.put(mapper.name, mapper);
        } else if (PROVIDER_EP.equalsIgnoreCase(extensionPoint)) {
            providersConfig = (SegmentIOProviders) contribution;
            providers = null;
        } else if (FILTERS_EP.equalsIgnoreCase(extensionPoint)) {
            userFilters = (SegmentIOUserFilter) contribution;
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
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

    public String getWriteKey() {
        if (config != null) {
            if (config.writeKey != null) {
                return config.writeKey;
            }
        }
        return Framework.getProperty(WRITE_KEY, DEFAULT_DEBUG_KEY);
    }

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
            } catch (Exception e) {
                log.error("Unable to access SegmentIO Flusher via reflection",
                        e);
            }
        }
        return flusher;
    }

    public void identify(NuxeoPrincipal principal) {
        identify(principal, null);
    }

    public Providers getProviders() {
        if (providers == null) {
            providers = new Providers();
            if (providersConfig != null) {
                if (!providersConfig.enableDefaults) {
                    providers.setDefault(false);
                }
                for (String name : providersConfig.providers.keySet()) {
                    providers.setEnabled(name,
                            providersConfig.providers.get(name));
                }
            }
        }
        return providers;
    }

    public void identify(NuxeoPrincipal principal,
            Map<String, Serializable> metadata) {

        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal,
                metadata);

        if (!mustTrackprincipal(wrapper.getUserId())) {
            log.debug("Skip user " + principal.getName());
            return;
        }

        if (Framework.isTestModeSet()) {
            pushForTest("identify", wrapper.getUserId(), null, metadata);
            Map<String, Serializable> groupMeta = wrapper.getGroupMetadata();
            if (groupMeta.size() > 0 && groupMeta.containsKey("id")) {
                pushForTest("group", wrapper.getUserId(), "group", groupMeta);
            }
        } else {
            if (debugMode) {
                log.info("send identify for " + wrapper.getUserId()
                        + " with meta : " + metadata.toString());
            } else {
                log.debug("send identify with " + metadata.toString());
                Traits traits = new Traits();
                traits.putAll(wrapper.getMetadata());
                Context ctx = new Context();
                ctx.setProviders(getProviders());
                Analytics.identify(wrapper.getUserId(), traits, ctx);

                Map<String, Serializable> groupMeta = wrapper.getGroupMetadata();
                if (groupMeta.size() > 0 && groupMeta.containsKey("id")) {
                    Traits gtraits = new Traits();
                    gtraits.putAll(groupMeta);
                    group((String) groupMeta.get("id"), wrapper.getUserId(),
                            gtraits, ctx);
                } else {
                    // automatic grouping
                    if (principal.getCompany() != null) {
                        group(principal.getCompany(), wrapper.getUserId(),
                                null, ctx);
                    } else if (wrapper.getMetadata().get("company") != null) {
                        group((String) wrapper.getMetadata().get("company"),
                                wrapper.getUserId(), null, ctx);
                    }
                }
            }
        }
    }

    protected void group(String groupId, String userId, Traits traits,
            Context ctx) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        Flusher flusher = getFlusher();
        if (flusher != null) {
            Group grp = new Group(userId, groupId, traits, new DateTime(), ctx);
            flusher.enqueue(grp);
        } else {
            log.warn("Can not use Group API");
        }
    }

    protected void pushForTest(String action, String principalName,
            String eventName, Map<String, Serializable> metadata) {
        Map<String, Object> data = new HashMap<>();
        if (metadata != null) {
            data.putAll(metadata);
        }
        data.put("action", action);
        if (eventName != null) {
            data.put("eventName", eventName);
        }
        data.put(SegmentIODataWrapper.PRINCIPAL_KEY, principalName);
        testData.add(data);
    }

    public List<Map<String, Object>> getTestData() {
        return testData;
    }

    protected boolean mustTrackprincipal(String principalName) {
        SegmentIOUserFilter filter = getUserFilters();
        if (filter == null) {
            return true;
        }
        return filter.canTrack(principalName);
    }

    public void track(NuxeoPrincipal principal, String eventName) {
        track(principal, null);
    }

    public void track(NuxeoPrincipal principal, String eventName,
            Map<String, Serializable> metadata) {

        SegmentIODataWrapper wrapper = new SegmentIODataWrapper(principal,
                metadata);

        if (!mustTrackprincipal(wrapper.getUserId())) {
            log.debug("Skip user " + principal.getName());
            return;
        }

        if (Framework.isTestModeSet()) {
            pushForTest("track", wrapper.getUserId(), eventName, metadata);
        } else {
            if (debugMode) {
                log.info("send track for " + eventName + " user : "
                        + wrapper.getUserId() + " with meta : "
                        + metadata.toString());
            } else {
                log.debug("send track with " + metadata.toString());
                EventProperties eventProperties = new EventProperties();
                eventProperties.putAll(wrapper.getMetadata());
                Context ctx = new Context();
                ctx.setProviders(getProviders());
                Analytics.track(wrapper.getUserId(), eventName,
                        eventProperties, new DateTime(), ctx);
            }
        }
    }

    public void flush() {
        if (!debugMode) {
            // only flush if Analytics was actually initialized
            Analytics.flush();
        }
    }

    public Map<String, List<SegmentIOMapper>> getMappers(List<String> events) {
        Map<String, List<SegmentIOMapper>> targetMappers = new HashMap<String, List<SegmentIOMapper>>();
        for (String event : events) {
            if (event2Mappers.containsKey(event)) {
                targetMappers.put(event, event2Mappers.get(event));
            }
        }
        return targetMappers;
    }

    public Set<String> getMappedEvents() {
        return event2Mappers.keySet();
    }

    public Map<String, List<SegmentIOMapper>> getAllMappers() {
        return event2Mappers;
    }

    public SegmentIOUserFilter getUserFilters() {
        return userFilters;
    }

}
