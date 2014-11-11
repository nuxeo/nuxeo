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

package org.nuxeo.ecm.core.repository.jcr;

import java.util.Collection;

import javax.jcr.Node;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * A node proxy is a typed wrapper to a JCR node.
 * <p>
 * The wrapped node may not exists but the proxy
 * must be able to create it if needed.
 * <p>
 * Each proxy have a type.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface JCRNodeProxy {

    /**
     * Gets the underlying node - if proxy is not already connected to a node
     * it will connect now and return the node.
     *
     * @return
     * @throws DocumentException
     */
    Node getNode() throws DocumentException;

    Node connect() throws DocumentException;

    boolean isConnected();

    Field getField(String name);

    ComplexType getSchema(String schema);

    Collection<Field> getFields();

    JCRDocument getDocument();

}
