/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * {@link ContributionFragmentRegistry} to register
 * {@link AutomaticVideoConversion}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class AutomaticVideoConversionContributionHandler extends
        ContributionFragmentRegistry<AutomaticVideoConversion> {

    public final Map<String, AutomaticVideoConversion> registry;

    public AutomaticVideoConversionContributionHandler() {
        registry = new HashMap<String, AutomaticVideoConversion>();
    }

    @Override
    public String getContributionId(AutomaticVideoConversion contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id,
            AutomaticVideoConversion contrib,
            AutomaticVideoConversion newOrigContrib) {
        if (contrib.isEnabled()) {
            registry.put(id, contrib);
        } else {
            registry.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id,
            AutomaticVideoConversion origContrib) {
        registry.remove(id);
    }

    @Override
    public AutomaticVideoConversion clone(AutomaticVideoConversion object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // cannot happens.
        }
    }

    @Override
    public void merge(AutomaticVideoConversion src, AutomaticVideoConversion dst) {
        dst.setEnabled(src.isEnabled());
    }

}
