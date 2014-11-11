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
        return String.format("<%s '{%s}%s'>", getClass(), namespace,
                localName);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

}
