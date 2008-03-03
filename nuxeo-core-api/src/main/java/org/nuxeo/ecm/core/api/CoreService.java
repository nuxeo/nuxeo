/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreService extends DefaultComponent {

    public static final String ID = "org.nuxeo.ecm.core.api.factories";
    public static final String TAG_FACTORY = "factory";
    public static final String ATTR_CLASS = "class";

    private static final Log log = LogFactory.getLog(CoreService.class);

    private ComponentContext context;

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }


    @Override
    public void registerExtension(Extension extension) {
        NodeList nodes = extension.getElement().getElementsByTagName(TAG_FACTORY);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0).getAttributes().getNamedItem(ATTR_CLASS);
            if (node != null) {
                String klass = node.getNodeValue();
                try {
                    CoreSessionFactory factory = (CoreSessionFactory) context.getRuntimeContext()
                        .loadClass(klass).newInstance();
                    CoreInstance.getInstance().initialize(factory);
                } catch (Exception e) {
                    log.error("Failed to instantiate server connector: " + klass, e);
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {

    }

}
