/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 11.5, unused.
 */
@XObject("config")
@Deprecated
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
                    Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(klass);
                    xmap.register(cl);
                    config = xmap.load(element);
                } catch (ClassNotFoundException e) {
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
