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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s, handling merge of registered
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdaterDescriptor} elements.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsUpdaterRegistry extends ContributionFragmentRegistry<QuotaStatsUpdaterDescriptor> {

    protected Map<String, QuotaStatsUpdater> quotaStatsUpdaters = new HashMap<>();

    public QuotaStatsUpdater getQuotaStatsUpdater(String name) {
        return quotaStatsUpdaters.get(name);
    }

    public List<QuotaStatsUpdater> getQuotaStatsUpdaters() {
        return new ArrayList<>(quotaStatsUpdaters.values());
    }

    @Override
    public String getContributionId(QuotaStatsUpdaterDescriptor descriptor) {
        return descriptor.getName();
    }

    @Override
    public void contributionUpdated(String id, QuotaStatsUpdaterDescriptor contrib,
            QuotaStatsUpdaterDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            try {
                QuotaStatsUpdater updater = contrib.getQuotaStatsUpdaterClass().getDeclaredConstructor().newInstance();
                updater.setName(contrib.getName());
                updater.setLabel(contrib.getLabel());
                updater.setDescriptionLabel(contrib.getDescriptionLabel());
                quotaStatsUpdaters.put(id, updater);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
        } else {
            quotaStatsUpdaters.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, QuotaStatsUpdaterDescriptor descriptor) {
        quotaStatsUpdaters.remove(id);
    }

    @Override
    public QuotaStatsUpdaterDescriptor clone(QuotaStatsUpdaterDescriptor descriptor) {
        return descriptor.clone();
    }

    @Override
    public void merge(QuotaStatsUpdaterDescriptor src, QuotaStatsUpdaterDescriptor dst) {
        dst.setQuotaStatsUpdaterClass(src.getQuotaStatsUpdaterClass());
        dst.setEnabled(src.isEnabled());
        dst.setLabel(src.getLabel());
        dst.setDescriptionLabel(src.getDescriptionLabel());
    }

}
