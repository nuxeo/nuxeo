/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ResourceAdapter.java 22854 2007-07-22 21:10:27Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api;


/**
 * Adapter to transform any java Object into a qualified name
 * resource and conversely.
 *
 * TODO should use Serializable instead of Object
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface ResourceAdapter {

    String getNamespace();

    void setNamespace(String namespace);

    /**
     * Transform an incoming object into a Resource
     *
     * @param object TODO Serializable
     * @return the resource
     */
    Resource getResource(Object object);

    /**
     * Resolve the resource to an applicative representation,
     * for instance a DocumentModel,
     *
     * @param resource
     * @return the representation TODO Serializable
     */
    Object getResourceRepresentation(Resource resource);

    /**
     * @return the class being adapted
     */
    Class<?> getKlass();
}
