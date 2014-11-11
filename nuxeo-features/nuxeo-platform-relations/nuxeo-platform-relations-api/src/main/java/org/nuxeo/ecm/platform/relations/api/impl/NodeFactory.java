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
 * $Id: NodeFactory.java 20645 2007-06-17 13:16:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;

/**
 * Node factory to create default nodes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class NodeFactory {

    // Utility class.
    private NodeFactory() { }

    public static LiteralImpl createLiteral(String value) {
        return new LiteralImpl(value);
    }

    public static LiteralImpl createLiteral(String value, String language) {
        LiteralImpl lit = new LiteralImpl(value);
        lit.setLanguage(language);
        return lit;
    }

    public static LiteralImpl createTypedLiteral(String value, String type) {
        LiteralImpl lit = new LiteralImpl(value);
        lit.setType(type);
        return lit;
    }

    public static BlankImpl createBlank() {
        return new BlankImpl();
    }

    public static BlankImpl createBlank(String id) {
        return new BlankImpl(id);
    }

    public static Resource createResource(String uri) {
        return new ResourceImpl(uri);
    }

    public static QNameResource createQNameResource(String namespace,
            String localName) {
        return new QNameResourceImpl(namespace, localName);
    }
}
