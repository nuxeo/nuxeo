/*
 * (C) Copyright 2010-2019 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;

/**
 * Page provider descriptor accepting a custom class name. The expected interface is {@link PageProvider}, all other
 * attributes are common to other page provider descriptors.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("genericPageProvider")
public class GenericPageProviderDescriptor extends BasePageProviderDescriptor implements PageProviderDefinition {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<PageProvider<?>> klass;

    public Class<PageProvider<?>> getPageProviderClass() {
        return klass;
    }

    @Override
    protected BasePageProviderDescriptor newInstance() {
        return new GenericPageProviderDescriptor();
    }

    @Override
    public GenericPageProviderDescriptor clone() {
        GenericPageProviderDescriptor clone = (GenericPageProviderDescriptor) super.cloneDescriptor();
        clone.klass = getPageProviderClass();
        return clone;
    }

}
