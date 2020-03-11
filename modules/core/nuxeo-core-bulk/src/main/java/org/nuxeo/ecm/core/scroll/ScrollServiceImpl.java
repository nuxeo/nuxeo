/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.core.scroll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;

/**
 * @since 11.1
 */
public class ScrollServiceImpl implements ScrollService {

    protected static final String SEP_KEY = ":";

    protected static final String DEFAULT_NAME = "default";

    protected final Map<String, ScrollDescriptor> descriptors;

    public ScrollServiceImpl(List<ScrollDescriptor> scrollDescriptors) {
        descriptors = new HashMap<>(scrollDescriptors.size());
        scrollDescriptors.forEach(descriptor -> descriptors.put(getKey(descriptor), descriptor));
        scrollDescriptors.stream()
                         .filter(ScrollDescriptor::isDefault)
                         .forEach(descriptor -> descriptors.put(getDefaultKey(descriptor), descriptor));
    }

    protected String getKey(ScrollDescriptor descriptor) {
        return getKey(descriptor.getType(), descriptor.getName());
    }

    protected String getDefaultKey(ScrollDescriptor descriptor) {
        return getKey(descriptor.getType(), DEFAULT_NAME);
    }

    protected String getKey(ScrollRequest request) {
        return getKey(request.getType(), request.getName() == null ? DEFAULT_NAME : request.getName());
    }

    protected String getKey(String type, String name) {
        return type + SEP_KEY + name;
    }

    @Override
    public Scroll scroll(ScrollRequest request) {
        Objects.requireNonNull(request);
        ScrollDescriptor descriptor = descriptors.get(getKey(request));
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "Unknown Scroll " + getKey(request) + " implementation for request: " + request);
        }
        Scroll scroll = descriptor.newScrollInstance();
        scroll.init(request, descriptor.getOptions());
        return scroll;
    }

    @Override
    public boolean exists(ScrollRequest request) {
        if (request == null) {
            return false;
        }
        return descriptors.containsKey(getKey(request));
    }
}
