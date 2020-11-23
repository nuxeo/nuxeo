/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
public class VideoConversionContributionHandler extends ContributionFragmentRegistry<VideoConversion> {

    public final Map<String, VideoConversion> registry;

    public VideoConversionContributionHandler() {
        registry = new HashMap<>();
    }

    @Override
    public String getContributionId(VideoConversion contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, VideoConversion contrib, VideoConversion newOrigContrib) {
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

        if (src.isRenditionSet()) {
            dst.setRendition(src.isRendition());
        }

        if (src.isRenditionVisibleSet()) {
            dst.setRenditionVisible(src.isRenditionVisible());
        }
    }

}
