/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */
package org.nuxeo.ecm.quota;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor object for registering {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("quotaStatsUpdater")
public class QuotaStatsUpdaterDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@class")
    protected Class<? extends QuotaStatsUpdater> quotaStatsUpdaterClass;

    @XNode("@label")
    protected String label;

    @XNode("@descriptionLabel")
    protected String descriptionLabel;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Class<? extends QuotaStatsUpdater> getQuotaStatsUpdaterClass() {
        return quotaStatsUpdaterClass;
    }

    public String getLabel() {
        return label;
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (QuotaStatsUpdaterDescriptor) o;
        var merged = new QuotaStatsUpdaterDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = other.enabled;
        merged.quotaStatsUpdaterClass = getIfNull(other.quotaStatsUpdaterClass, quotaStatsUpdaterClass);
        merged.label = defaultIfBlank(other.label, label);
        merged.descriptionLabel = defaultIfBlank(other.descriptionLabel, descriptionLabel);
        return merged;
    }
}
