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
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("config")
public class ConfigurationDescriptorImpl {

    private static final Log log = LogFactory.getLog(ConfigurationDescriptorImpl.class);

    private static final Object NULL = new Object();

    @XNode
    public Element element;

    private Object config;

    public Element getElement() {
        return element;
    }

    public DocumentFragment getFragment() {
        element.normalize();
        Node node = element.getFirstChild();
        if (node == null) {
            return null;
        }
        Range range = ((DocumentRange) element.getOwnerDocument()).createRange();
        range.setStartBefore(node);
        range.setEndAfter(element.getLastChild());
        return range.cloneContents();
    }

    public Object getConfiguration() {
        if (config == null) {
            XMap xmap = new XMap();
            String klass = element.getAttribute("class");
            if (klass != null) {
                try {
                    Class cl = Thread.currentThread().getContextClassLoader().loadClass(klass);
                    xmap.register(cl);
                    config = xmap.load(element);
                } catch (Exception e) {
                    config = NULL;
                    log.error(e, e);
                }
            } else {
                config = NULL;
            }
        }
        return config;
    }

}
