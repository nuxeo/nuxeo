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

    @Override
    public void contributionRemoved(String id, PictureTemplate pictureTemplate) {
        pictureTemplates.remove(id);
    }

    @Override
    public void contributionUpdated(String id,
            PictureTemplate oldPictureTemplate,
            PictureTemplate newPictureTemplate) {

        if (pictureTemplates.containsKey(id)) {
            pictureTemplates.remove(id);
        }

        if (oldPictureTemplate.isEnabled()) {
            pictureTemplates.put(id, oldPictureTemplate);
        }
    }

    @Override
    public String getContributionId(PictureTemplate pictureTemplate) {
        return pictureTemplate.getTitle();
    }

    @Override
    public void merge(PictureTemplate oldPictureTemplate,
            PictureTemplate newPictureTemplate) {

        oldPictureTemplate.setEnabled(newPictureTemplate.isEnabled());

        String chainId = newPictureTemplate.getChainId();
        if (!StringUtils.isEmpty(chainId)) {
            oldPictureTemplate.setChainId(chainId);
        }

        String tag = newPictureTemplate.getTag();
        if (!StringUtils.isEmpty(tag)) {
            oldPictureTemplate.setTag(tag);
        }

        String description = newPictureTemplate.getDescription();
        if (!StringUtils.isEmpty(description)) {
            oldPictureTemplate.setTag(description);
        }

        int maxSize = newPictureTemplate.getMaxSize();
        if (maxSize >= 0) {
            oldPictureTemplate.setMaxSize(maxSize);
        }
    }
}
