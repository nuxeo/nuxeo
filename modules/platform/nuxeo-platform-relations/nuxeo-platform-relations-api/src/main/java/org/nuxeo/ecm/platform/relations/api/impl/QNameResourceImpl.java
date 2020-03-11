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
 * $Id: QNameResourceImpl.java 20796 2007-06-19 09:52:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.QNameResource;

/**
 * Prefixed resource.
 * <p>
 * New prefixed resources can be declared through extension points.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class QNameResourceImpl extends ResourceImpl implements QNameResource {

    private static final long serialVersionUID = 1L;

    protected String namespace = "";

    protected String localName;

    public QNameResourceImpl(String namespace, String localName) {
        super(namespace + localName);
        this.namespace = namespace;
        this.localName = localName;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.QNAMERESOURCE;
    }

    @Override
    public boolean isQNameResource() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s('{%s}%s')", getClass().getSimpleName(), namespace, localName);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public void setLocalName(String localName) {
        this.localName = localName;
    }

}
