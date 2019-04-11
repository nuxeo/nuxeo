/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected Map<String, PostContentCreationHandlerDescriptor> postContentCreationHandlerDescriptors = new HashMap<>();

    @Override
    public String getContributionId(PostContentCreationHandlerDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, PostContentCreationHandlerDescriptor contrib,
            PostContentCreationHandlerDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            postContentCreationHandlerDescriptors.put(id, contrib);
        } else {
            postContentCreationHandlerDescriptors.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, PostContentCreationHandlerDescriptor contrib) {
        postContentCreationHandlerDescriptors.remove(id);
    }

    @Override
    public PostContentCreationHandlerDescriptor clone(PostContentCreationHandlerDescriptor postContentCreationHandler) {
        try {
            return (PostContentCreationHandlerDescriptor) postContentCreationHandler.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(PostContentCreationHandlerDescriptor src, PostContentCreationHandlerDescriptor dst) {
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
        List<PostContentCreationHandlerDescriptor> descs = new ArrayList<>(
                postContentCreationHandlerDescriptors.values());
        Collections.sort(descs);

        List<PostContentCreationHandler> handlers = new ArrayList<>();
        for (PostContentCreationHandlerDescriptor desc : descs) {
            try {
                handlers.add(desc.getClazz().getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instantiate class for handler: " + desc.getName(), e);
            }
        }
        return handlers;
    }

}
