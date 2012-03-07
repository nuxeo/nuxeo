/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class VideoProviderContributionHandler extends
        ContributionFragmentRegistry<VideoProvider> {

    public final Map<String, VideoProvider> registry;

    public VideoProvider defaultVideoProvider;

    public VideoProviderContributionHandler() {
        registry = new HashMap<String, VideoProvider>();
    }

    @Override
    public String getContributionId(VideoProvider contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, VideoProvider contrib,
            VideoProvider newOrigContrib) {
        if (contrib.isEnabled()) {
            registry.put(id, contrib);
            if (defaultVideoProvider == null || contrib.isDefault()) {
                defaultVideoProvider = contrib;
            }
        } else {
            registry.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, VideoProvider origContrib) {
        registry.remove(id);
    }

    @Override
    public VideoProvider clone(VideoProvider object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // cannot happens.
        }
    }

    @Override
    public void merge(VideoProvider src, VideoProvider dst) {
        dst.setEnabled(src.isEnabled());
        dst.setDefault(src.isDefault());
        dst.setFacets(src.getFacets());
        dst.setVideoPlayerTemplate(src.getVideoPlayerTemplate());
        dst.setKeepOriginal(src.isKeepOriginal());
        dst.setParameters(src.getParameters());
        dst.setVideoProviderHandler(src.getVideoProviderHandler());
    }

}
