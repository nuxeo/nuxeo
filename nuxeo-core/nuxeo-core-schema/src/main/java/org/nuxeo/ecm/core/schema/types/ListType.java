/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types;

import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * A list of typed objects.
 * <p>
 * The List type can validate java <code>array</code> and/or java
 * <code>Collection</code>.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ListType extends Type {

    /**
     * Get the field describing the element type the list accept.
     *
     * @return the field describing the list element types
     */
    Type getFieldType();

    /**
     * The field name if any was specified.
     * <p>
     * This is used to more for outputting the list as XML and for
     * compatibility with XSD.
     *
     * @return the field name
     */
    String getFieldName();

    /**
     * Get the field defining the elements stored by this list.
     *
     * @return the field
     */
    Field getField();

    /**
     * Gets the required minimum count of elements in this list.
     *
     * @return the minimum count of required elements
     */
    int getMinCount();

    /**
     * Gets the required maximum count of allowed elements in this list.
     *
     * @return the maximum count of allowed elements
     */
    int getMaxCount();

    /**
     * Gets the default value of the list elements, if any.
     *
     * @return the default value or null if none
     */
    Object getDefaultValue();

    /**
     * Sets the default value encoded as a string.
     *
     * @param value
     */
    void setDefaultValue(String value);

    /**
     * Sets list limits.
     *
     * @param minOccurs
     * @param maxOccurs
     */
    void setLimits(int minOccurs, int maxOccurs);

    @Override
    TypeRef<ListType> getRef();

    /**
     * Whether the instances of this list are arrays.
     */
    boolean isArray();

    /**
     * This method is provided for compatibility. Existing code is mapping scalar lists to arrays
     * but this should be changed in order to map only explicit scalar list (those declared using xs:list)
     * to arrays and not all list that have scalar items.
     *
     * @return true if the list items are of a scalar type
     *
     * TODO FIXME XXX remove the method and use instead isArray
     */
    boolean isScalarList();

}
