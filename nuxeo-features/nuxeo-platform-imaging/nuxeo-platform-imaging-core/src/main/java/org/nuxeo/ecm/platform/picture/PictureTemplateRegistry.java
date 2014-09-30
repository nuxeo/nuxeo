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
import org.nuxeo.ecm.platform.picture.api.PictureTemplate;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@link PictureTemplate} class (merge supported)
 *
 * @since 5.9.6
 */
public class PictureTemplateRegistry extends
        ContributionFragmentRegistry<PictureTemplate> {

    private static final Log log = LogFactory.getLog(PictureTemplateRegistry.class);

    protected final Map<String, PictureTemplate> pictureTemplates = new HashMap<>(
            7);

    /**
     * Collection of picture template which can't be disabled
     */
    protected final List<String> defaultPictureTemplates = Arrays.asList(
            "Small", "Medium", "Original", "Thumbnail", "OriginalJpeg");

    /**
     * @return unmodifiable picture templates titles list
     */
    public List<String> getDefaultPictureTemplates() {
        return Collections.unmodifiableList(defaultPictureTemplates);
    }

    /**
     * Check if the passed picture template is present by default (so it can't
     * be disabled)
     */
    public boolean isDefault(PictureTemplate pictureTemplate) {
        return defaultPictureTemplates.contains(pictureTemplate.getTitle());
    }

    @Override
    public PictureTemplate clone(PictureTemplate pictureTemplate) {
        return pictureTemplate.clone();
    }

    public PictureTemplate getById(String id) {
        return pictureTemplates.get(id);
    }

    /**
     * JIT sort
     *
     * FIXME- Try a different approch since this method will be call a few time
     * and each time a new list is created and sorted
     */
    public List<PictureTemplate> getPictureTemplates() {
        List<PictureTemplate> entries = new ArrayList<>(
                pictureTemplates.values());
        Collections.sort(entries);
        return entries;
    }

    @Override
    public void contributionRemoved(String id, PictureTemplate pictureTemplate) {
        pictureTemplates.remove(id);
    }

    @Override
    public void contributionUpdated(String id, PictureTemplate pictureTemplate,
            PictureTemplate oldPictureTemplate) {
        if (pictureTemplates.containsKey(id)) {
            contributionRemoved(id, pictureTemplate);
        }

        if (pictureTemplate.isEnabled()) {
            pictureTemplates.put(id, pictureTemplate);
        }
    }

    @Override
    public String getContributionId(PictureTemplate pictureTemplate) {
        String title = pictureTemplate.getTitle();
        if (StringUtils.isEmpty(title)) {
            throw new IllegalStateException(
                    "The 'title' property of a pictureTemplate mustn't be null or empty ("
                            + pictureTemplate + ")");
        }

        return title;
    }

    @Override
    public void merge(PictureTemplate pictureTemplate,
            PictureTemplate oldPictureTemplate) {

        boolean enabled = pictureTemplate.isEnabled();
        if (!enabled && isDefault(pictureTemplate)) {
            enabled = true;

            if (log.isWarnEnabled()) {
                log.warn("The picture template named "
                        + pictureTemplate.getTitle()
                        + " can't be disabled (it's present in the default picture template collection)");
            }
        }

        oldPictureTemplate.setEnabled(enabled);

        String chainId = pictureTemplate.getChainId();
        if (!StringUtils.isEmpty(chainId)) {
            oldPictureTemplate.setChainId(chainId);
        }

        String tag = pictureTemplate.getTag();
        if (!StringUtils.isEmpty(tag)) {
            oldPictureTemplate.setTag(tag);
        }

        String description = pictureTemplate.getDescription();
        if (!StringUtils.isEmpty(description)) {
            oldPictureTemplate.setDescription(description);
        }

        // TODO - Fix order always get updated even if it's zero so the default
        // value if nothing is specified
        oldPictureTemplate.setOrder(pictureTemplate.getOrder());

        Integer maxSize = pictureTemplate.getMaxSize();

        // Ignore if maxSize is not specified or negative
        if (maxSize != null && maxSize >= 0) {
            oldPictureTemplate.setMaxSize(maxSize);
        }
    }
}
