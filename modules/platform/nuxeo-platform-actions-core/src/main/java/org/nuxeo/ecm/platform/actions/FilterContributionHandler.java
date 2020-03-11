/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FilterContributionHandler extends ContributionFragmentRegistry<DefaultActionFilter> {

    protected ActionFilterRegistry filterReg;

    public FilterContributionHandler() {
        filterReg = new ActionFilterRegistry();
    }

    public ActionFilterRegistry getRegistry() {
        return filterReg;
    }

    @Override
    public DefaultActionFilter clone(DefaultActionFilter object) {
        return object.clone();
    }

    @Override
    public void contributionUpdated(String id, DefaultActionFilter filter, DefaultActionFilter origContrib) {
        filterReg.addFilter(filter);
    }

    @Override
    public void contributionRemoved(String id, DefaultActionFilter origContrib) {
        filterReg.removeFilter(id);
    }

    @Override
    public String getContributionId(DefaultActionFilter contrib) {
        return contrib.getId();
    }

    @Override
    public void merge(DefaultActionFilter src, DefaultActionFilter dst) {
        if (src.getAppend()) {
            List<FilterRule> mergedRules = new ArrayList<>();
            mergedRules.addAll(Arrays.asList(dst.getRules()));
            mergedRules.addAll(Arrays.asList(src.getRules()));
            dst.setRules(mergedRules.toArray(new FilterRule[mergedRules.size()]));
        } else {
            dst.setRules(src.getRules());
        }
    }

}
