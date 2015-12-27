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

import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;

import ro.isdc.wro.cache.factory.CacheKeyFactory;
import ro.isdc.wro.model.factory.WroModelFactory;

/**
 * Manager generating processors configuration from contributions to {@link WebResourceManager}, and hooking up other
 * specific factories.
 *
 * @since 7.10
 */
public class NuxeoWroPageManagerFactory extends NuxeoWroManagerFactory {

    @Override
    protected WroModelFactory newModelFactory() {
        return new NuxeoWroPageModelFactory();
    }

    @Override
    protected CacheKeyFactory newCacheKeyFactory() {
        return new NuxeoWroPageCacheKeyFactory();
    }

}
