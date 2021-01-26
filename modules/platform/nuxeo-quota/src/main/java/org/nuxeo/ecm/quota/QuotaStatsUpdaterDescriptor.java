/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor object for registering {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("quotaStatsUpdater")
@XRegistry(enable = false)
public class QuotaStatsUpdaterDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNode("@class")
    protected Class<? extends QuotaStatsUpdater> quotaStatsUpdaterClass;

    @XNode("@label")
    protected String label;

    @XNode("@descriptionLabel")
    protected String descriptionLabel;

    public String getName() {
        return name;
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

}
