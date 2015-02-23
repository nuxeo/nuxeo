/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.wro.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import ro.isdc.wro.model.resource.locator.UriLocator;
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.util.provider.ConfigurableProviderSupport;

/**
 * Nuxeo contributor to wro processors and locators, registered as a service.
 *
 * @since 7.3
 */
public class NuxeoConfigurableProvider extends ConfigurableProviderSupport {

    private static final Log log = LogFactory.getLog(NuxeoConfigurableProvider.class);

    public static final String PRE_TYPE = "wroPre";

    public static final String POST_TYPE = "wroPost";

    @Override
    public Map<String, UriLocator> provideLocators() {
        Map<String, UriLocator> map = new HashMap<String, UriLocator>();
        map.put(NuxeoUriLocator.ALIAS, new NuxeoUriLocator());
        return map;
    }

    public Map<String, ResourcePreProcessor> providePreProcessors() {
        Map<String, ResourcePreProcessor> map = new HashMap<String, ResourcePreProcessor>();
        // extend with runtime service processors registration
        List<? extends Processor> processors = resolveProcessors(PRE_TYPE);
        for (Processor p : processors) {
            Class<?> klass = p.getTargetProcessorClass();
            if (klass == null) {
                // assume alias references a native wro processor
                continue;
            }
            ResourcePreProcessor proc;
            try {
                proc = (ResourcePreProcessor) klass.newInstance();
                map.put(p.getName(), proc);
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instanciating resource pre processor", e);
                continue;
            }
        }
        return map;
    }

    @Override
    public Map<String, ResourcePostProcessor> providePostProcessors() {
        Map<String, ResourcePostProcessor> map = new HashMap<String, ResourcePostProcessor>();
        // extend with runtime service processors registration
        List<Processor> processors = resolveProcessors(POST_TYPE);
        for (Processor p : processors) {
            Class<?> klass = p.getTargetProcessorClass();
            if (klass == null) {
                // assume alias references a native wro processor
                continue;
            }
            ResourcePostProcessor proc;
            try {
                proc = (ResourcePostProcessor) klass.newInstance();
                map.put(p.getName(), proc);
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instanciating resource post processor", e);
                continue;
            }
        }
        return map;
    }

    public static List<Processor> resolveProcessors(String type) {
        WebResourceManager service = Framework.getService(WebResourceManager.class);
        List<Processor> processors = service.getProcessors(type);
        if (processors != null) {
            return processors;
        }
        return Collections.emptyList();
    }

}
