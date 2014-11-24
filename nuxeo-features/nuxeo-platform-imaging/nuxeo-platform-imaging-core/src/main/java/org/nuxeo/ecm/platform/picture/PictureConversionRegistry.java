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
import java.util.Arrays;
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
public class PictureConversionRegistry extends
        ContributionFragmentRegistry<PictureConversion> {

    private static final Log log = LogFactory.getLog(PictureConversionRegistry.class);

    protected final Map<String, PictureConversion> pictureConversions = new HashMap<>();

    /**
     * Collection of picture conversion which can't be disabled
     */
    protected final List<String> defaultPictureConversions = Arrays.asList(
            "Small", "Medium", "Original", "Thumbnail", "OriginalJpeg");

    /**
     * @return unmodifiable picture conversions titles list
     */
    public List<String> getDefaultPictureConversions() {
        return Collections.unmodifiableList(defaultPictureConversions);
    }

    /**
     * Check if the passed picture conversion is present by default (so it can't
     * be disabled)
     */
    public boolean isDefault(PictureConversion pictureConversion) {
        return defaultPictureConversions.contains(pictureConversion.getId());
    }

    @Override
    public PictureConversion clone(PictureConversion pictureConversion) {
        return pictureConversion.clone();
    }

    public PictureConversion getById(String id) {
        return pictureConversions.get(id);
    }

    /**
     * FIXME- Try a different logic. Like eagerly sort the map when a picture
     * conversion is registered
     *
     * @return picture conversion collection sorted by order
     */
    public List<PictureConversion> getPictureConversions() {
        List<PictureConversion> entries = new ArrayList<>(
                pictureConversions.values());
        Collections.sort(entries);
        return entries;
    }

    @Override
    public void contributionRemoved(String id, PictureConversion pictureConversion) {
        pictureConversions.remove(id);
    }

    @Override
    public void contributionUpdated(String id, PictureConversion pictureConversion,
            PictureConversion oldPictureConversion) {
        if (pictureConversions.containsKey(id)) {
            contributionRemoved(id, pictureConversion);
        }

        if (pictureConversion.isEnabled()) {
            check(pictureConversion);
            pictureConversions.put(id, pictureConversion);
        }
    }

    /**
     * Check if the passed picture conversion is valid.
     *
     * A valid picture conversion should not have a null or empty id.
     *
     * @throws IllegalStateException if the id is null or empty
     */
    protected void check(PictureConversion pictureConversion) {
        // Check if the title is null or empty
        if (StringUtils.isBlank(pictureConversion.getId())) {
            throw new IllegalStateException(
                    "The 'id' property of a picture conversion mustn't be null or empty ("
                            + pictureConversion + ")");
        }
    }

    @Override
    public String getContributionId(PictureConversion pictureConversion) {
        return pictureConversion.getId();
    }

    @Override
    public void merge(PictureConversion pictureConversion,
            PictureConversion oldPictureConversion) {

        boolean enabled = pictureConversion.isEnabled();
        if (!enabled && isDefault(pictureConversion)) {
            enabled = true;

            if (log.isWarnEnabled()) {
                log.warn("The picture conversion named "
                        + pictureConversion.getId()
                        + " can't be disabled (it's present in the default picture conversion collection)");
            }
        }

        oldPictureConversion.setEnabled(enabled);

        String chainId = pictureConversion.getChainId();
        if (!StringUtils.isEmpty(chainId)) {
            oldPictureConversion.setChainId(chainId);
        }

        String tag = pictureConversion.getTag();
        if (!StringUtils.isEmpty(tag)) {
            oldPictureConversion.setTag(tag);
        }

        String description = pictureConversion.getDescription();
        if (!StringUtils.isEmpty(description)) {
            oldPictureConversion.setDescription(description);
        }

        Integer order = pictureConversion.getOrder();
        if (order != null) {
            oldPictureConversion.setOrder(order);
        }

        Integer maxSize = pictureConversion.getMaxSize();
        if (maxSize != null) {
            oldPictureConversion.setMaxSize(maxSize);
        }
    }
}
