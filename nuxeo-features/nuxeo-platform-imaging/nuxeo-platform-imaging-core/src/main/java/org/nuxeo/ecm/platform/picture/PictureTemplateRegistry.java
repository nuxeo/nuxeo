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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.picture.api.PictureTemplate;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for the {@link PictureTemplate} class (merge supported)
 *
 * @since 5.9.6
 */
public class PictureTemplateRegistry extends
        ContributionFragmentRegistry<PictureTemplate> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, PictureTemplate> pictureTemplates = new TreeMap<>();

    /**
     * Collection of picture template which can't be disabled
     */
    protected final Set<String> defaultPictureTemplates = new HashSet<String>(5);

    public PictureTemplateRegistry() {
        super();
        loadDefaultPictureTemplates();
    }

    /**
     * Unmodifiable picture templates titles
     *
     * @return
     */
    public Set<String> getDefaultPictureTemplates() {
        return Collections.unmodifiableSet(defaultPictureTemplates);
    }

    /**
     * Check if the passed picture template is present as default (so it can't
     * be disabled)
     *
     * @param pictureTemplate
     * @return
     */
    public boolean isDefault(PictureTemplate pictureTemplate) {
        return defaultPictureTemplates.contains(pictureTemplate.getTitle());
    }

    private void loadDefaultPictureTemplates() {
        defaultPictureTemplates.add("Small");
        defaultPictureTemplates.add("Medium");
        defaultPictureTemplates.add("Original");
        defaultPictureTemplates.add("Thumbnail");
        defaultPictureTemplates.add("OriginalJpeg");
    }

    @Override
    public PictureTemplate clone(PictureTemplate pictureTemplate) {
        return pictureTemplate.clone();
    }

    public PictureTemplate getById(String id) {
        return pictureTemplates.get(id);
    }

    public Collection<PictureTemplate> getPictureTemplates() {
        return pictureTemplates.values();
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

            if (logger.isWarnEnabled()) {
                logger.warn(
                        "The picture template named '{}' can't be disabled (it's present in the default picture template collection)",
                        pictureTemplate.getTitle());
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

        Integer maxSize = pictureTemplate.getMaxSize();

        // Ignore if maxSize is not setted or negative
        if (maxSize != null && maxSize >= 0) {
            oldPictureTemplate.setMaxSize(maxSize);
        }
    }
}
