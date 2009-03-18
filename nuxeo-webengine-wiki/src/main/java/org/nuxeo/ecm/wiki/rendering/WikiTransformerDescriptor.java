/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.wiki.rendering;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("rendering-extension")
public class WikiTransformerDescriptor extends RenderingExtensionDescriptor {

    @XNode("@name")
    protected void setName(String name) {
        this.name = name;
    }

    @XNode("@class")
    protected void setClassName(Class<?> klass) {
        this.klass = klass;
    }

    @XNode("@serializer")
    protected Class<?> serializerClass;

    @XNodeList(value="filter", type=ArrayList.class, componentType=WikiFilterDescriptor.class)
    protected List<WikiFilterDescriptor> filters;


    @Override
    public WikiTransformer newInstance() throws Exception {
        WikiTransformer tr;
        if (serializerClass == null) {
            tr = new WikiTransformer();
        } else {
            tr = new WikiTransformer((WikiSerializer) serializerClass.newInstance());
        }
        WikiSerializer serializer = tr.getSerializer();
        for (WikiFilterDescriptor wfd : filters) {
            if (wfd.clazz != null) {
                Class<?> clazz = Class.forName(wfd.clazz);
                WikiFilter filter = (WikiFilter) clazz.newInstance();
                serializer.addFilter(filter);
            } else {
                serializer.addFilter(new PatternFilter(wfd.pattern, wfd.replacement));
            }
        }
        return tr;
    }

}
