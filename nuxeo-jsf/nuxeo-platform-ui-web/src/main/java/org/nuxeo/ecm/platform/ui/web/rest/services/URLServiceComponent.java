/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: URLServiceComponent.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest.services;

import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.URLPatternDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component registering url policies and document view codecs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class URLServiceComponent extends DefaultComponent {

    public static final String NAME = URLServiceComponent.class.getName();

    public static final String URL_PATTERNS_EXTENSION_POINT = "urlpatterns";

    protected URLPolicyService urlPolicyService;

    @Override
    public void activate(ComponentContext context) {
        urlPolicyService = new URLPolicyServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        urlPolicyService.clear();
        urlPolicyService = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(URLPolicyService.class)) {
            return (T) urlPolicyService;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (URL_PATTERNS_EXTENSION_POINT.equals(extensionPoint)) {
            urlPolicyService.addPatternDescriptor((URLPatternDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (URL_PATTERNS_EXTENSION_POINT.equals(extensionPoint)) {
            urlPolicyService.removePatternDescriptor((URLPatternDescriptor) contribution);
        }
    }

}
