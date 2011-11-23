/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.SimpleStyle;

/**
 * Registry for theme style resources, handling merge of registered
 * {@link SimpleStyle} elements.
 *
 * @since 5.5
 */
public class StyleRegistry extends ContributionFragmentRegistry<SimpleStyle> {

    protected Map<String, SimpleStyle> themePageStyles = new HashMap<String, SimpleStyle>();

    @Override
    public String getContributionId(SimpleStyle contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, SimpleStyle contrib,
            SimpleStyle newOrigContrib) {
        themePageStyles.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, SimpleStyle origContrib) {
        themePageStyles.remove(id);
    }

    @Override
    public SimpleStyle clone(SimpleStyle orig) {
        SimpleStyle clone = new SimpleStyle();
        clone.setName(orig.getName());
        clone.setSrc(orig.getSrc());
        clone.setContent(orig.getContent());
        return clone;
    }

    @Override
    public void merge(SimpleStyle src, SimpleStyle dst) {
        // no merge => replace content
        dst.setSrc(src.getSrc());
        dst.setContent(src.getContent());
    }

    public SimpleStyle getStyle(String id) {
        return themePageStyles.get(id);
    }
}
