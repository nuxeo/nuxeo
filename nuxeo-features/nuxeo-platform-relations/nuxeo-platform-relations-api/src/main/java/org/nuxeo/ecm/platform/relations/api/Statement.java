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
 * $Id: Statement.java 19480 2007-05-27 10:46:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Statement interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface Statement extends Comparable<Statement>, Serializable, Cloneable {

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

    Object clone();

}
