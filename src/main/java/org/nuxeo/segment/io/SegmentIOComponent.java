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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

import com.github.segmentio.Analytics;
import com.github.segmentio.models.EventProperties;
import com.github.segmentio.models.Traits;


/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class SegmentIOComponent extends DefaultComponent implements SegmentIO {

    public final static String WRITE_KEY = "segment.io.write.key";

    public final static String CONFIG_EP = "config";

    public final static String MAPPER_EP = "mapper";

    protected Map<String, SegmentIOMapper> mappers;

    protected Map<String, List<SegmentIOMapper>> event2Mappers;

    protected SegmentIOConfig config;

    protected Bundle bundle;

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public void activate(ComponentContext context) {
        this.bundle = context.getRuntimeContext().getBundle();
    }

    @Override
    public void deactivate(ComponentContext context) {
        flush();
        this.bundle = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equalsIgnoreCase(extensionPoint)) {
            config = (SegmentIOConfig) contribution;
        }
        else if (CONFIG_EP.equalsIgnoreCase(extensionPoint)) {
            SegmentIOMapper mapper = (SegmentIOMapper) contribution;
            mappers.put(mapper.name, mapper);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        Analytics.initialize(getKey());
        computeEvent2Mappers();
    }

    protected void computeEvent2Mappers() {
        event2Mappers = new HashMap<String, List<SegmentIOMapper>>();
        for (SegmentIOMapper mapper : mappers.values()) {
            for (String event : mapper.events) {
                List<SegmentIOMapper> m4event = event2Mappers.get(event);
                if (m4event==null) {
                    event2Mappers.put(event, new ArrayList<SegmentIOMapper>());
                    m4event = event2Mappers.get(event);
                }
                if (!m4event.contains(mapper)) {
                    m4event.add(mapper);
                }
            }
        }

    }

    protected String getKey() {
        if (config!=null) {
            return config.writeKey;
        }
        return Framework.getProperty(WRITE_KEY, "FakeKey_ChangeMe");
    }

    public void identify(NuxeoPrincipal principal) {
        identify(principal, null);
    }

    public void identify(NuxeoPrincipal principal, Map<String, String> metadata) {

        String userId = principal.getPrincipalId();
        Traits traits = new Traits();

        traits.put("email", principal.getEmail());
        traits.put("firstName", principal.getFirstName());
        traits.put("lastName", principal.getLastName());
        traits.put("email", principal.getEmail());
        if (metadata!=null) {
            traits.putAll(metadata);
        }
        Analytics.identify(userId, traits);
    }

    public void track(NuxeoPrincipal principal, String eventName) {
        track(principal, null);
    }

    public void track(NuxeoPrincipal principal, String eventName, Map<String, String> metadata) {

        String userId = principal.getPrincipalId();

        EventProperties eventProperties = new EventProperties();
        eventProperties.put("email", principal.getEmail());
        eventProperties.put("firstName", principal.getFirstName());
        eventProperties.put("lastName", principal.getLastName());
        eventProperties.put("email", principal.getEmail());

        eventProperties.putAll(metadata);
        Analytics.track(userId, eventName, eventProperties);
    }

    public void flush() {
        Analytics.flush();
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

}
