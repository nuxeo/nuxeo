/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LinkRegistry extends AbstractContributionRegistry<String, LinkDescriptor> {

    // type to view bindings
    protected final Map<String, LinkDescriptor[]> links; // category to links mapping

    public LinkRegistry() {
        links = new ConcurrentHashMap<String, LinkDescriptor[]>();
    }

    public List<LinkDescriptor> getLinks(String category) {
        LinkDescriptor[] descriptors = links.get(category);
        if (descriptors != null && descriptors.length > 0) {
            return Arrays.asList(descriptors);
        }
        return new ArrayList<LinkDescriptor>();
    }

    public List<LinkDescriptor> getActiveLinks(Resource context, String category) {
        List<LinkDescriptor> result = new ArrayList<LinkDescriptor>();
        LinkDescriptor[] descriptors = links.get(category);
        if (descriptors != null && descriptors.length > 0) {
            for (LinkDescriptor descriptor : descriptors) {
                if (descriptor.isEnabled(context)) {
                    result.add(descriptor);
                }
            }
        }
        return result;
    }

    public synchronized void registerLink(LinkDescriptor td) {
        addFragment(td.getId(), td);
    }

    public void unregisterLink(LinkDescriptor td) {
        removeFragment(td.getId(), td);
    }

    @Override
    protected LinkDescriptor clone(LinkDescriptor object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Must never happens");
        }
    }

    @Override
    protected void applyFragment(LinkDescriptor object, LinkDescriptor fragment) {
        // a view fragment may be used to replace the view implementation class and optionally the guard
        // and/or to add new categories
        object.applyFragment(fragment);
    }

    @Override
    protected void applySuperFragment(LinkDescriptor object, LinkDescriptor superFragment) {
        // links are not using inheritance
    }

    @Override
    protected void installContribution(String key, LinkDescriptor object) {
        List<String> cats = object.getCategories();
        for (String cat : cats) {
            installLink(cat, object);
        }
    }

    @Override
    protected void updateContribution(String key, LinkDescriptor object, LinkDescriptor oldValue) {
        removeLink(oldValue);
        installContribution(key, object);
    }

    @Override
    protected void uninstallContribution(String key, LinkDescriptor object) {
        removeLink(object);
    }

    @Override
    protected boolean isMainFragment(LinkDescriptor object) {
        return !object.isFragment();
    }

    protected void installLink(String category, LinkDescriptor link) {
        LinkDescriptor[] descriptors = links.get(category);
        if (descriptors == null) {
            descriptors = new LinkDescriptor[] { link };
        } else {
            LinkDescriptor[] ar = new LinkDescriptor[descriptors.length + 1];
            System.arraycopy(descriptors, 0, ar, 0, descriptors.length);
            ar[descriptors.length] = link;
            descriptors = ar;
        }
        links.put(category, descriptors);
    }

    protected void removeLink(LinkDescriptor link) {
        List<String> cats = link.getCategories();
        for (String cat : cats) {
            removeLink(cat, link);
        }
    }

    protected void removeLink(String category, LinkDescriptor link) {
        LinkDescriptor[] descriptors = links.get(category);
        if (descriptors == null) {
            return;
        }
        for (int i = 0; i < descriptors.length; i++) {
            if (link.getId().equals(descriptors[i].getId())) {
                if (descriptors.length == 1 && i == 0) {
                    links.remove(category);
                    return;
                } else {
                    LinkDescriptor[] tmp = new LinkDescriptor[descriptors.length - 1];
                    if (i > 0) {
                        System.arraycopy(descriptors, 0, tmp, 0, i);
                    }
                    if (i < descriptors.length - 1) {
                        System.arraycopy(descriptors, i + 1, tmp, i, descriptors.length - i - 1);
                    }
                    links.put(category, tmp);
                    return;
                }
            }
        }
    }

}
