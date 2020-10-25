/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;

/**
 * Core Query page provider descriptor.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("coreQueryPageProvider")
public class CoreQueryPageProviderDescriptor extends BasePageProviderDescriptor implements PageProviderDefinition {

    @Override
    protected BasePageProviderDescriptor newInstance() {
        return new CoreQueryPageProviderDescriptor();
    }

    @Override
    public CoreQueryPageProviderDescriptor clone() {
        return (CoreQueryPageProviderDescriptor) super.cloneDescriptor();
    }

}
