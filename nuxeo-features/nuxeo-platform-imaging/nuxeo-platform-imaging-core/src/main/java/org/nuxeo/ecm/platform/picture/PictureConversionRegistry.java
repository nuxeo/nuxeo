/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@link org.nuxeo.ecm.platform.picture.api.PictureConversion} class (merge supported)
 *
 * @since 7.1
 */
public class PictureConversionRegistry extends ContributionFragmentRegistry<PictureConversion> {

    private static final Log log = LogFactory.getLog(PictureConversionRegistry.class);

    protected final Map<String, PictureConversion> pictureConversions = new HashMap<>();

    public PictureConversion getPictureConversion(String id) {
        return pictureConversions.get(id);
    }

    /**
     * Returns picture conversion collection sorted by order.
     */
    public List<PictureConversion> getPictureConversions() {
        List<PictureConversion> entries = new ArrayList<>(pictureConversions.values());
        Collections.sort(entries);
        return entries;
    }

    @Override
    public String getContributionId(PictureConversion pictureConversion) {
        return pictureConversion.getId();
    }

    @Override
    public void contributionUpdated(String id, PictureConversion pictureConversion,
            PictureConversion oldPictureConversion) {
        if (pictureConversions.containsKey(id)) {
            contributionRemoved(id, pictureConversion);
        }

        if (pictureConversion.isEnabled()) {
            if (!StringUtils.isBlank(id)) {
                pictureConversions.put(id, pictureConversion);
            } else {
                log.warn(String.format("Missing 'id' for picture conversion %s, not registering it.",
                        pictureConversion));
            }

        }
    }

    @Override
    public void contributionRemoved(String id, PictureConversion pictureConversion) {
        pictureConversions.remove(id);
    }

    @Override
    public PictureConversion clone(PictureConversion pictureConversion) {
        return pictureConversion.clone();
    }

    @Override
    public void merge(PictureConversion source, PictureConversion dest) {
        dest.merge(source);
    }

}
