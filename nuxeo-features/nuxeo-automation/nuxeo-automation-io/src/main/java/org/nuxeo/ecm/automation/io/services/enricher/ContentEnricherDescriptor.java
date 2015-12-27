/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.ecm.platform.actions.DefaultActionFilter;

/**
 * @since 5.7.3
 * @deprecated The JSON marshalling was migrated to nuxeo-core-io. An enricher system is also available. See
 *             org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher for an example. To migrate an
 *             existing enricher, keep the marshalling code and use it in class implementing
 *             AbstractJsonEnricher&lt;DocumentModel&gt; (the use of contextual parameters is a bit different but
 *             compatible / you have to manage the enricher's parameters yourself). Don't forget to contribute to
 *             service org.nuxeo.ecm.core.io.registry.MarshallerRegistry to register your enricher.
 */
@Deprecated
@XObject("enricher")
public class ContentEnricherDescriptor {

    protected static final Log log = LogFactory.getLog(ContentEnricherDescriptor.class);

    @XNode("@name")
    public String name;

    @XNodeList(value = "category", type = ArrayList.class, componentType = String.class)
    List<String> categories;

    @XNode("@class")
    public Class<? extends ContentEnricher> klass;

    @XNodeList(value = "filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    @XNodeList(value = "filter", type = ActionFilter[].class, componentType = DefaultActionFilter.class)
    protected ActionFilter[] filters;

    @XNodeMap(value = "parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters;

    /**
     * @return
     */
    public ContentEnricher getContentEnricher() {
        try {
            ContentEnricher enricher = klass.newInstance();
            enricher.setParameters(parameters);
            return enricher;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            log.error(String.format("Failed to create %s content enricher: %s", name, e.getMessage()));
            return null;
        }
    }

}
