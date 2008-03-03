/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (URL_PATTERNS_EXTENSION_POINT.equals(extensionPoint)) {
            urlPolicyService.addPatternDescriptor((URLPatternDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (URL_PATTERNS_EXTENSION_POINT.equals(extensionPoint)) {
            urlPolicyService.removePatternDescriptor((URLPatternDescriptor) contribution);
        }
    }

}
