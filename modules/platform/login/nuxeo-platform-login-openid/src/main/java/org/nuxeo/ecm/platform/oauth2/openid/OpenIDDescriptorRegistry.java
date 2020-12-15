/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.oauth2.openid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Descriptor registry with custom logics to disable some providers.
 *
 * @since 11.5
 */
public class OpenIDDescriptorRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(OpenIDDescriptorRegistry.class);

    @Override
    protected void register(Context ctx, XAnnotatedObject xObject, Element element) {
        super.register(ctx, xObject, element);
        String id = (String) xObject.getRegistryId().getValue(ctx, element);
        OpenIDConnectProviderDescriptor provider = (OpenIDConnectProviderDescriptor) contributions.get(id);
        if (provider.isEnabled()) {
            if (provider.getClientId() == null || provider.getClientSecret() == null) {
                log.info("OpenId provider for {} is disabled because clientId and/or clientSecret are empty",
                        provider::getName);
                provider.setEnabled(false);
                disabled.add(id);
            }
        }
    }

}
