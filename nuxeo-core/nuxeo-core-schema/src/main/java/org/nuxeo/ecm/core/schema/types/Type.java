/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

import java.io.Serializable;

/**
 * A Type object is used to describe some ECM content.
 * <p>
 * There are two groups of content types:
 *
 * <ul>
 * <li> primitive types - these are builtin types used to describe simple values
 * like string, integers, dates etc
 * <li> custom types - these are used defined types based on the primitive types
 * </ul>
 *
 * Custom types are structured in two groups:
 *
 * <ul>
 * <li> simple types - constrained primitive types. Constraints are specific to
 * each primitive type. <br>
 * For example the "string" type may have constraints like maximum length,
 * regular expression pattern etc. <br>
 * So you can define a custom simple type as being a string that match the
 * regular expression <code>.+@.+</code>
 * <li> complex types - structured types that can be expressed as a tree like
 * structure of other primitive, simple or complex types.
 * </ul>
 *
 * The typing system is mainly inspired from XML schemas.
 * <p>
 * There is a root type called <code>ANY</code> type. <br>
 * Apart this special type, each type has a super type (a type from which it is
 * derived)
 * <p>
 * On top of this typing system there are two high level content types:
 *
 * <ul>
 * <li> schemas - a schema is a complex that can be used to form composite types
 * <br>
 * Because multiple schemas may live together in a composite type they must
 * provide a namespace to avoid name collisions inside a composite type
 * <li> composite types - a composite type is made of several schemas. <br>
 * You can see a composite type as a type derived from multiple complex super
 * types. <br>
 * Composite types are used to define ECM documents
 * </ul>
 *
 * Type names must not contains a <code>:</code> character. This character may
 * be used internally to prefix the type name so it must not be used in the type
 * name.
 */
public interface Type extends Serializable {

    AnyType ANY = AnyType.INSTANCE;

    /**
     * Gets the name of this type.
     *
     * @return the type name
     */
    String getName();

    /**
     * Gets the local name of this type.
     *
     * @return the local name
     */
    String getSchemaName();

    /**
     * Gets the schema defining this type.
     *
     * @return
     */
    Schema getSchema();

    /**
     * Gets the super type.
     *
     * @return the super type or null if this is a primitive type
     */
    Type getSuperType();

    /**
     * Gets the entire hierarchy of super-types.
     * <p>
     * The array is ordered as follows:
     * <ul>
     * <li>the direct super type is the first element,
     * <li>the super super type is the second element,
     * <li>and so on.
     * </ul>
     * <p>
     * The returned array is never null. An empty array is returned in the case
     * of <code>ANY</code> type.
     *
     * @return an array containing the supertypes of this type
     */
    Type[] getTypeHierarchy();

    /**
     * Tests whether the given type is derived from this type.
     *
     * @param type the type to test
     * @return true if the given type is derived from this type, false otherwise
     */
    boolean isSuperTypeOf(Type type);

    /**
     * Tests whether this type is a simple type.
     *
     * @return true if this type is a simple type, false otherwise
     */
    boolean isSimpleType();

    /**
     * Tests whether this type is a complex type.
     *
     * @return true if this type is a complex type, false otherwise
     */
    boolean isComplexType();

    /**
     * Tests whether this type is a list type.
     *
     * @return true if is a list type, false otherwise
     */
    boolean isListType();

    /**
     * Tests whether this type is the ANY type.
     *
     * @return true if it is the ANY type, false otherwise
     */
    boolean isAnyType();

    /**
     * Tests whether this is a composite type.
     *
     * @return true if this is a composite type, false otherwise
     */
    boolean isCompositeType();

    /**
     * Tests whether the given object is of this type.
     *
     * @param object the object to test
     * @return true if the given object if of this type, false otherwise
     * @throws TypeException if an error occurs trying to retrieve the
     *             supertypes
     */
    boolean validate(Object object) throws TypeException;

    // TODO this actually decodes from XSD types. XSD decoding should be moved
    // to a separate class and not kept inside the Type model.
    /**
     * Decodes the string representation into an object of this type.
     * <p>
     * Returns null if the string can not be decoded.
     *
     * @param string the string to decode
     * @return the converted object that can be use as a value for an object of
     *         this type or null if the given object cannot be converted
     */
    Object decode(String string);

    /**
     * Encodes the given object that is assumed to be of this type into a string
     * representation.
     * <p>
     * Null is returned if the object cannot be converted.
     *
     * @param object the object to convert
     * @return the string representation of the given object or null if object
     *         cannot be converted
     */
    String encode(Object object);

    /**
     * Creates a new instance according to this type and filled with default
     * values.
     *
     * @return
     */
    Object newInstance();

    /**
     * Converts the given value to an object compatible with the associated type.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws TypeException if the value to convert is not compatible with the associated type
     */
    Object convert(Object value) throws TypeException;

}
