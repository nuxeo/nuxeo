/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.BINARY_METADATA_MONITOR;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_MAPPING_EP;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_PROCESSORS_EP;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_RULES_EP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Binary metadata component which registers all binary metadata contributions.
 *
 * @since 7.1
 */
public class BinaryMetadataComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(BinaryMetadataComponent.class);

    protected BinaryMetadataService metadataService;

    @Override
    public void start(ComponentContext context) {
        BinaryMetadataService service = new BinaryMetadataServiceImpl(getExtensionPointRegistry(METADATA_MAPPING_EP),
                getExtensionPointRegistry(METADATA_PROCESSORS_EP), getExtensionPointRegistry(METADATA_RULES_EP));
        if (Boolean.parseBoolean(
                Framework.getProperty(BINARY_METADATA_MONITOR, Boolean.toString(log.isTraceEnabled())))) {
            service = MetricInvocationHandler.newProxy(service, BinaryMetadataService.class);
        }
        metadataService = service;
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        metadataService = null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(BinaryMetadataService.class)) {
            return adapter.cast(metadataService);
        }
        return null;
    }

}
