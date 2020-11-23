/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */
package org.nuxeo.ecm.platform.video.action;

import static org.nuxeo.ecm.platform.video.computation.RecomputeTranscodedVideosComputation.PARAM_CONVERSION_NAMES;
import static org.nuxeo.ecm.platform.video.computation.RecomputeTranscodedVideosComputation.PARAM_XPATH;
import static org.nuxeo.ecm.platform.video.computation.RecomputeVideoInfoComputation.RECOMPUTE_ALL_VIDEO_INFO;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.5
 */
public class RecomputeVideoConversionsActionValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of(PARAM_XPATH, PARAM_CONVERSION_NAMES, RECOMPUTE_ALL_VIDEO_INFO);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        // Check generateVidInfoOnMissingOnly
        validateBoolean(RECOMPUTE_ALL_VIDEO_INFO, command);

        // Check XPath
        String xpath = command.getParam(PARAM_XPATH);
        validateXpath(PARAM_XPATH, xpath, command);

        // Check conversions list
        validateList(PARAM_CONVERSION_NAMES, command);
        List<String> conversionNames = new ArrayList<>(command.getParam(PARAM_CONVERSION_NAMES));
        // recompute all renditions
        if (conversionNames.isEmpty()) {
            return;
        }
        VideoService videoService = Framework.getService(VideoService.class);
        conversionNames.removeAll(videoService.getAvailableVideoConversionsNames());
        if (!conversionNames.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("The conversions: %s are not supported.", conversionNames));
        }
    }
}
