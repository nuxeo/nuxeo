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
package org.nuxeo.ecm.web.resources.wro.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.wro.provider.NuxeoConfigurableProvider;

import ro.isdc.wro.cache.factory.CacheKeyFactory;
import ro.isdc.wro.manager.factory.ConfigurableWroManagerFactory;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.resource.processor.factory.ConfigurableProcessorsFactory;

/**
 * Manager generating processors configuration from contributions to {@link WebResourceManager}, and hooking up other
 * specific factories.
 *
 * @since 7.3
 */
public class NuxeoWroManagerFactory extends ConfigurableWroManagerFactory {

    private static final Log log = LogFactory.getLog(NuxeoWroManagerFactory.class);

    @Override
    protected Properties newConfigProperties() {
        final Properties props = new Properties();
        // automatically setup runtime service processors
        addAliases(props, ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, NuxeoConfigurableProvider.PRE_TYPE);
        addAliases(props, ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, NuxeoConfigurableProvider.POST_TYPE);
        if (log.isDebugEnabled()) {
            log.debug("Built new conf, properties=" + props);
        }
        return props;
    }

    protected List<String> resolveProcessorNames(String type) {
        List<String> res = new ArrayList<String>();
        List<Processor> procs = NuxeoConfigurableProvider.resolveProcessors(type);
        for (Processor proc : procs) {
            res.add(proc.getName());
        }
        return res;
    }

    protected void addAliases(Properties props, String propName, String type) {
        final String SEP = ",";
        List<String> procs = resolveProcessorNames(type);
        String propValue = props.getProperty(propName);
        if (!StringUtils.isBlank(propValue)) {
            String[] existing = StringUtils.split(propValue, SEP);
            if (existing != null) {
                for (String s : existing) {
                    if (s != null) {
                        procs.add(s.trim());
                    }
                }
            }
        }
        props.put(propName, StringUtils.join(procs, SEP));
    }

    @Override
    protected WroModelFactory newModelFactory() {
        return new NuxeoWroModelFactory();
    }

    @Override
    protected CacheKeyFactory newCacheKeyFactory() {
        return new NuxeoWroCacheKeyFactory();
    }

}
