/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
        Map<String, UriLocator> map = new HashMap<>();
        map.put(NuxeoUriLocator.ALIAS, new NuxeoUriLocator());
        return map;
    }

    @Override
    public Map<String, ResourcePreProcessor> providePreProcessors() {
        Map<String, ResourcePreProcessor> map = new HashMap<>();
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
                proc = (ResourcePreProcessor) klass.getDeclaredConstructor().newInstance();
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
        Map<String, ResourcePostProcessor> map = new HashMap<>();
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
                proc = (ResourcePostProcessor) klass.getDeclaredConstructor().newInstance();
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
