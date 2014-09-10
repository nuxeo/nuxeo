/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;

/**
 * Page provider descriptor accepting a custom class name. The expected
 * interface is {@link ContentViewPageProvider}, all other attributes are
 * common to other page provider descriptors.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("genericPageProvider")
public class GenericPageProviderDescriptor extends BasePageProviderDescriptor
        implements PageProviderDefinition {

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

    public GenericPageProviderDescriptor clone() {
        GenericPageProviderDescriptor clone = (GenericPageProviderDescriptor) super.cloneDescriptor();
        clone.klass = getPageProviderClass();
        return clone;
    }

}
