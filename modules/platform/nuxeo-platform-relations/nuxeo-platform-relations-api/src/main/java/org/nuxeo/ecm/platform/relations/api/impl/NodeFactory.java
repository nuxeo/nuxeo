/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
    private NodeFactory() {
    }

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

    public static QNameResource createQNameResource(String namespace, String localName) {
        return new QNameResourceImpl(namespace, localName);
    }
}
