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
 * $Id: Statement.java 19480 2007-05-27 10:46:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Statement interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public interface Statement extends Comparable<Statement>, Serializable,
        Cloneable {

    Node getObject();

    void setObject(Node object);

    Resource getPredicate();

    void setPredicate(Node predicate);

    Subject getSubject();

    void setSubject(Node subject);

    // property (statement metadata) management

    Map<Resource, Node[]> getProperties();

    Map<String, Node[]> getStringProperties();

    Node getProperty(Resource property);

    Node[] getProperties(Resource property);

    void setProperties(Map<Resource, Node[]> properties);

    void setProperty(Resource property, Node value);

    void setProperties(Resource property, Node[] values);

    void deleteProperties();

    void deleteProperty(Resource property);

    void deleteProperty(Resource property, Node value);

    void deleteProperties(Resource property, Node[] values);

    void addProperties(Map<Resource, Node[]> properties);

    void addProperty(Resource property, Node value);

    void addProperties(Resource property, Node[] values);

    Object clone() throws CloneNotSupportedException;

}
