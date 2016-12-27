/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
                log.warn(String.format("Missing 'id' for picture conversion %s, not registering it.", pictureConversion));
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
        if (source.isEnabledSet() && source.isEnabled() != dest.isEnabled()) {
            dest.setEnabled(source.isEnabled());
        }

        if (source.isDefaultSet() && source.isDefault() != dest.isDefault()) {
            dest.setDefault(source.isDefault());
        }

        // cannot disable default picture conversion
        if (!dest.isEnabled() && dest.isDefault()) {
            dest.setEnabled(true);
            if (log.isWarnEnabled()) {
                String message = String.format("The picture conversion '%s' is marked as default, enabling it.",
                        dest.getId());
                log.warn(message);
            }
        }

        String chainId = source.getChainId();
        if (!StringUtils.isBlank(chainId)) {
            dest.setChainId(chainId);
        }

        String tag = source.getTag();
        if (!StringUtils.isBlank(tag)) {
            dest.setTag(tag);
        }

        String description = source.getDescription();
        if (!StringUtils.isBlank(description)) {
            dest.setDescription(description);
        }

        Integer order = source.getOrder();
        if (order != null) {
            dest.setOrder(order);
        }

        Integer maxSize = source.getMaxSize();
        if (maxSize != null) {
            dest.setMaxSize(maxSize);
        }

        List<String> newFilterIds = new ArrayList<>();
        newFilterIds.addAll(dest.getFilterIds());
        newFilterIds.addAll(source.getFilterIds());
        dest.setFilterIds(newFilterIds);

        if (source.isRenditionSet()) {
            dest.setRendition(source.isRendition());
        }

        if (source.isRenditionVisibleSet()) {
            dest.setRenditionVisible(source.isRenditionVisible());
        }
    }
}
