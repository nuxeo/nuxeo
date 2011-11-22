/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry of {@link PostContentCreationHandler}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class PostContentCreationHandlerRegistry extends
        ContributionFragmentRegistry<PostContentCreationHandlerDescriptor> {

    private static final Log log = LogFactory.getLog(PostContentCreationHandlerRegistry.class);

    protected Map<String, PostContentCreationHandlerDescriptor> postContentCreationHandlerDescriptors = new HashMap<String, PostContentCreationHandlerDescriptor>();

    @Override
    public String getContributionId(PostContentCreationHandlerDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id,
            PostContentCreationHandlerDescriptor contrib,
            PostContentCreationHandlerDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            postContentCreationHandlerDescriptors.put(id, contrib);
        } else {
            postContentCreationHandlerDescriptors.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id,
            PostContentCreationHandlerDescriptor contrib) {
        postContentCreationHandlerDescriptors.remove(id);
    }

    @Override
    public PostContentCreationHandlerDescriptor clone(
            PostContentCreationHandlerDescriptor postContentCreationHandler) {
        try {
            return (PostContentCreationHandlerDescriptor) postContentCreationHandler.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(PostContentCreationHandlerDescriptor src,
            PostContentCreationHandlerDescriptor dst) {
        if (src.getClazz() != null) {
            dst.setClazz(src.getClazz());
        }
        if (src.isEnabled() != dst.isEnabled()) {
            dst.setEnabled(src.isEnabled());
        }
        int order = src.getOrder();
        if (order > 0 && order != dst.getOrder()) {
            dst.setOrder(src.getOrder());
        }
    }

    public List<PostContentCreationHandler> getOrderedHandlers() {
        List<PostContentCreationHandlerDescriptor> descs = new ArrayList<PostContentCreationHandlerDescriptor>(
                postContentCreationHandlerDescriptors.values());
        Collections.sort(descs);

        List<PostContentCreationHandler> handlers = new ArrayList<PostContentCreationHandler>();
        for (PostContentCreationHandlerDescriptor desc : descs) {
            try {
                handlers.add(desc.getClazz().newInstance());
            } catch (Exception e) {
                log.error(
                        "Unable to instantiate class for handler: "
                                + desc.getName(), e);
            }
        }
        return handlers;
    }

}
