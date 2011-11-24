/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
public class FilterContributionHandler extends
        ContributionFragmentRegistry<DefaultActionFilter> {

    protected ActionFilterRegistry filterReg;

    public FilterContributionHandler() {
        filterReg = new ActionFilterRegistry();
    }

    public ActionFilterRegistry getRegistry() {
        return filterReg;
    }

    @Override
    public DefaultActionFilter clone(DefaultActionFilter object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // cannot happens.
        }
    }

    @Override
    public void contributionUpdated(String id, DefaultActionFilter filter,
            DefaultActionFilter origContrib) {
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
            List<FilterRule> mergedRules = new ArrayList<FilterRule>();
            mergedRules.addAll(Arrays.asList(dst.getRules()));
            mergedRules.addAll(Arrays.asList(src.getRules()));
            dst.setRules(mergedRules.toArray(new FilterRule[mergedRules.size()]));
        } else {
            dst.setRules(src.getRules());
        }
    }

}
