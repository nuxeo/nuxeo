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