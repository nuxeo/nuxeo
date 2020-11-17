/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

/**
 * A list of typed objects.
 * <p>
 * The List type can validate java <code>array</code> and/or java <code>Collection</code>.
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
     * This is used to more for outputting the list as XML and for compatibility with XSD.
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
     */
    void setDefaultValue(String value);

    /**
     * Sets list limits.
     */
    void setLimits(int minOccurs, int maxOccurs);

    /**
     * Whether the instances of this list are arrays.
     */
    boolean isArray();

    /**
     * This method is provided for compatibility. Existing code is mapping scalar lists to arrays but this should be
     * changed in order to map only explicit scalar list (those declared using xs:list) to arrays and not all list that
     * have scalar items.
     *
     * @return true if the list items are of a scalar type TODO FIXME XXX remove the method and use instead isArray
     */
    boolean isScalarList();

}
