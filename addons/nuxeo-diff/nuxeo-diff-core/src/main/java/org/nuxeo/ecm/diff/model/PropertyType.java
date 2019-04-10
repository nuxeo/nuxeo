/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model;

/**
 * Property type constants: string, boolean, integer, scalarList, complex, complexList, ...
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class PropertyType {

    public static final String UNDEFINED = "undefined";

    public static final String STRING = "string";

    public static final String BOOLEAN = "boolean";

    public static final String DATE = "date";

    public static final String INTEGER = "integer";

    public static final String LONG = "long";

    public static final String DOUBLE = "double";

    public static final String CONTENT = "content";

    public static final String SCALAR_LIST = "scalarList";

    public static final String COMPLEX = "complex";

    public static final String COMPLEX_LIST = "complexList";

    public static final String CONTENT_LIST = "contentList";

    /**
     * Avoid instantiating a new property type.
     */
    private PropertyType() {
    }

    /**
     * Checks if is simple type.
     *
     * @param propertyType the property type
     * @return true, if is simple type
     */
    public static boolean isSimpleType(String propertyType) {

        return !isListType(propertyType) && !isComplexType(propertyType) && !isContentType(propertyType);
    }

    /**
     * Checks if is list type.
     *
     * @param propertyType the property type
     * @return true, if is list type
     */
    public static boolean isListType(String propertyType) {

        return SCALAR_LIST.equals(propertyType) || COMPLEX_LIST.equals(propertyType)
                || CONTENT_LIST.equals(propertyType);
    }

    /**
     * Checks if is scalar list type.
     *
     * @param propertyType the property type
     * @return true, if is scalar list type
     */
    public static boolean isScalarListType(String propertyType) {

        return SCALAR_LIST.equals(propertyType);
    }

    /**
     * Checks if is complex list type.
     *
     * @param propertyType the property type
     * @return true, if is complex list type
     */
    public static boolean isComplexListType(String propertyType) {

        return COMPLEX_LIST.equals(propertyType);
    }

    /**
     * Checks if is content list type.
     *
     * @param propertyType the property type
     * @return true, if is content list type
     */
    public static boolean isContentListType(String propertyType) {

        return CONTENT_LIST.equals(propertyType);
    }

    /**
     * Checks if is complex type.
     *
     * @param propertyType the property type
     * @return true, if is complex type
     */
    public static boolean isComplexType(String propertyType) {

        return COMPLEX.equals(propertyType);
    }

    /**
     * Checks if is content type.
     *
     * @param propertyType the property type
     * @return true, if is content type
     */
    public static boolean isContentType(String propertyType) {

        return CONTENT.equals(propertyType);
    }

}
