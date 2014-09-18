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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.picture.api.PictureTemplate;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@link PictureTemplate} class (merge supported)
 *
 * @since TODO
 */
public class PictureTemplateRegistry extends
        ContributionFragmentRegistry<PictureTemplate> {

    private final Map<String, PictureTemplate> pictureTemplates = new HashMap<>();

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

        Boolean enabled = pictureTemplate.isEnabled();
        if (enabled != null) {
            oldPictureTemplate.setEnabled(enabled);
        }

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
        if (maxSize != null) {
            oldPictureTemplate.setMaxSize(maxSize);
        }
    }
}
