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
 * {@link ContributionFragmentRegistry} to register {@link VideoConversion}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoConversionContributionHandler extends
        ContributionFragmentRegistry<VideoConversion> {

    public final Map<String, VideoConversion> registry;

    public VideoConversionContributionHandler() {
        registry = new HashMap<String, VideoConversion>();
    }

    @Override
    public String getContributionId(VideoConversion contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, VideoConversion contrib,
            VideoConversion newOrigContrib) {
        if (contrib.isEnabled()) {
            registry.put(id, contrib);
        } else {
            registry.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, VideoConversion origContrib) {
        registry.remove(id);
    }

    @Override
    public VideoConversion clone(VideoConversion object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // cannot happens.
        }
    }

    @Override
    public void merge(VideoConversion src, VideoConversion dst) {
        dst.setConverter(src.getConverter());
        dst.setHeight(src.getHeight());
        dst.setEnabled(src.isEnabled());
    }

}
