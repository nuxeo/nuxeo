/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for content view contributions, handling accurate merge on hot reload.
 *
 * @since 5.6
 */
public class ContentViewRegistry extends ContributionFragmentRegistry<ContentViewDescriptor> {

    protected static final Log log = LogFactory.getLog(ContentViewRegistry.class);

    protected final Map<String, ContentViewDescriptor> contentViews = new HashMap<>();

    protected final Map<String, Set<String>> contentViewsByFlag = new HashMap<>();

    @Override
    public String getContributionId(ContentViewDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ContentViewDescriptor contrib, ContentViewDescriptor newOrigContrib) {
        String name = contrib.getName();
        if (name == null) {
            log.error("Cannot register content view without a name");
            return;
        }
        if (contentViews.containsKey(id)) {
            contentViews.remove(id);
            removeContentViewFlags(name);
        }
        if (contrib.isEnabled()) {
            contentViews.put(name, contrib);
            addContentViewFlags(contrib);
            log.info("Registering content view with name " + id);
        }
    }

    @Override
    public void contributionRemoved(String id, ContentViewDescriptor origContrib) {
        contentViews.remove(id);
        removeContentViewFlags(origContrib);
        log.info("Unregistering content view with name " + id);
    }

    protected void addContentViewFlags(ContentViewDescriptor desc) {
        String name = desc.getName();
        List<String> flags = desc.getFlags();
        if (flags != null) {
            for (String flag : flags) {
                Set<String> items = contentViewsByFlag.get(flag);
                if (items == null) {
                    items = new LinkedHashSet<>();
                }
                items.add(name);
                contentViewsByFlag.put(flag, items);
            }
        }
    }

    protected void removeContentViewFlags(String contentViewName) {
        for (Set<String> items : contentViewsByFlag.values()) {
            if (items != null) {
                items.remove(contentViewName);
            }
        }
    }

    protected void removeContentViewFlags(ContentViewDescriptor desc) {
        String name = desc.getName();
        List<String> flags = desc.getFlags();
        if (flags != null) {
            for (String flag : flags) {
                Set<String> items = contentViewsByFlag.get(flag);
                if (items != null) {
                    items.remove(name);
                }
            }
        }
    }

    @Override
    public ContentViewDescriptor clone(ContentViewDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(ContentViewDescriptor src, ContentViewDescriptor dst) {
        dst.merge(src);
    }

    // API

    public ContentViewDescriptor getContentView(String id) {
        return contentViews.get(id);
    }

    public boolean hasContentView(String id) {
        return contentViews.containsKey(id);
    }

    public Set<String> getContentViewsByFlag(String flag) {
        return contentViewsByFlag.get(flag);
    }

    public Set<String> getContentViewNames() {
        return contentViews.keySet();
    }

    public Collection<ContentViewDescriptor> getContentViews() {
        return contentViews.values();
    }

}
