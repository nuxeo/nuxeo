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

package org.nuxeo.ecm.core.api.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Document properties are instances of document schema fields.
 * <p>
 * You can say that a {@link Field} object is like a Java class and a Property
 * object like a class instance. Thus, schemas defines fields (or
 * elements) which have a name and a type, and each field of a document can be
 * instantiated (if the schema permits) as a Property object.
 * <p>
 * Properties are always bound to a schema field that provides the type and
 * constraints on the property values. An exception is the root property the
 * {@link DocumentPart} object which is not bound to a field but to a schema.
 * <p>
 * So properties are holding the actual values for each defined field.
 * <p>
 * The usual way of using properties is to get a document from the storage
 * server then modify document properties and send them back to the storage
 * server to that modifications are be stored.
 * <p>
 * Note that the storage server can be on a remote machine so when modifying
 * properties remotely they are serialized and sent through the network between
 * the two machines. This means properties must hold serializable values and
 * also they must store some state flags so that the storage can decide which
 * property was modified and how in order to correctly update the stored
 * versions.
 * <p>
 * As we have seen each property may hold a serializable value which we will
 * refer to as the <code>normalized</code> property value. For each schema
 * field type there is only one java serializable object representation that
 * will be used as the normalized value. The property API is giving you the
 * possibility to use different compatible objects when setting or getting
 * property values. Each property implementation will automatically convert the
 * given value into a normalized one; so internally only the normalized value is
 * stored.
 * <p>
 * For example, for date properties you may use either <code>Date</code>
 * or <code>Calendar</code> when setting or retrieving a property value, but
 * the normalized value will be the <code>Calendar</code> one.
 * <p>
 * As we have seen, properties keep some state flags. Property flags can be divided in
 * two groups:
 * <ul>
 * <li> Dirty Flags - that reflect the public status of the document
 * <li> Internal Flags - that reflect some internal state
 * </ul>
 * <p>
 * Property Types:
 * <p>
 * Before going deeper in property flags, we will talk first about property
 * types. There are several types of properties that are very closed on the type
 * of fields they are bound onto.
 * <ul>
 * <li> Root Property (or <code>DocumentPart</code>) - this is a special
 * property that is bound to a schema instead of a field And it is the root of
 * the property tree.
 * <li> Complex Properties - container properties that are bound to complex
 * field types that can be represented as java <code>Map</code> objects. These
 * properties contains a set of schema defined properties. You cannot add new
 * child properties. You can only modify existing child properties. Complex
 * property values are expressed as java <code>Map</code> objects.
 * <ul>
 * <li> Structured Properties - this is a special case of complex properties.
 * The difference is that structured property values are expressed as <i>scalar</i>
 * java objects instead of java maps. By scalar java objects we mean any well
 * structured object which is not a container like a <code>Map</code> or a
 * <code>Collection</code>. These objects are usually as scalar values - it
 * doesn't make sense for example to set only some parts of that objects without
 * creating the object completely. An example of usage are Blob properties that
 * use {@link Blob} values.
 * </ul>
 * <li> List Properties - container properties that are bound to list field
 * types.
 * <li> Scalar Properties - atomic properties that are bound to scalar field
 * types and that are using as values scalar or primitive java objects like
 * arrays, primitives, String, Date etc.
 * </ul>
 * <p>
 * As we've seen there are 2 categories of properties: container properties and
 * scalar properties Complex and list properties are container properties while
 * structured and scalar properties are scalar.
 * <p>
 * Dirty Flags:
 * <p>
 * Dirty flags are used to keep track of the dirty state of a property. The
 * following flags are supported:
 * <ul>
 * <li> <code>IS_PHANTOM</code> - whether the property is existing in the
 * storage (was explicitly set by the user) or it was dynamically generated
 * using the default value by the implementation to fulfill schema definition.
 * This applies to all property types
 * <li> <code>IS_MODIFIED</code> - whether the property value was modified.
 * This applies to all property types.
 * <li> <code>IS_NEW</code> - whether the property is a new property that was
 * added to a parent list property This applies only to properties that are
 * children of a list property.
 * <li> <code>IS_REMOVED</code> - whether a property was removed. A removed
 * property will be removed from the storage and the next time you access the
 * property it will be a <code>phantom</code> one. This applies only to
 * properties that are children of a complex property.
 * <li> <code>IS_MOVED</code> - whether the property was moved on another
 * position inside the container list. This applies only to properties that are
 * children of a list property.
 * </ul>
 * <p>
 * There are several constraints on how property flags may change. This is a
 * list of all changes that may occur over dirty flags:
 * <ul>
 * <li> NONE + MODIFIED =&gt; MODFIED
 * <li> NONE + REMOVED =&gt; REMOVED
 * <li> NONE + MOVED =&gt; MOVED
 * <li> PHANTOM + MODIFIED =&gt; MODIFIED
 * <li> NEW + MODIFIED =&gt; NEW | MODIFIED
 * <li> NEW + MOVED =&gt; NEW | MOVED
 * <li> MODIFIED + REMOVED =&gt; REMOVED
 * <li> MODIFIED + MOVED =&gt; MODIFIED | MOVED
 * <li> MODIFIED + MODIFIED =&gt; MODIFIED
 * </ul>
 * <p>
 * The combinations not listed above are not permitted.
 * <p>
 * In case of list items, the REMOVED flag is not used since the property will
 * be physically removed from the property tree.
 * <p>
 * Also when the dirty flag of a children property changes, its parent is
 * informed to update its MODIFIED flag if needed. This way a modification on a
 * children property is propagated to parents in the form of a MODIFIED flag.
 * <p>
 * Internal Flags:
 * <p>
 * Internal flags are used by the implementation to keep some internal state.
 * For these flags you should look into the implementation
 * <p>
 * Apart flags properties can also hold some random user data using
 * {@link Property#setData(Object)} and {@link Property#getData()} methods. This
 * can be used for example to keep a context attached to a property. But be
 * aware when using this you should provide serializable objects as the data you
 * are attaching otherwise if properties are serialized / unserialized this will
 * generate errors. The API is not forcing you to use serializable values since
 * you can also use this feature to store temporary context data that will not
 * be sent over the network.
 *
 * @see <code>TestPropertyModel</code> for usage of property API
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Property extends Cloneable, Serializable, Iterable<Property> {

    /**
     * No dirty flags set.
     */
    int NONE = 0;

    /**
     * Flag used to mark a property as new. Property was added to a list.
     */
    int IS_NEW = 1;

    /**
     * Flag used to mark a property as dirty. Property value was modified.
     */
    int IS_MODIFIED = 2;

    /**
     * Flag used to mark a property as dirty. Property was removed.
     */
    int IS_REMOVED = 4;

    /**
     * Flag used to mark a property as dirty. Property was moved to another
     * index.
     */
    int IS_MOVED = 8;

    /**
     * Flag used to mark a property as phantom.
     */
    int IS_PHANTOM = 16;

    /**
     * A mask for the first 4 flags: NEW, REMOVED, MODIFIED, MOVED.
     */
    int IS_DIRTY = IS_NEW | IS_REMOVED | IS_MOVED | IS_MODIFIED;

    /**
     * A mask for public flags.
     */
    int DIRTY_MASK = IS_PHANTOM | IS_DIRTY;

    /**
     * Tests if this property is new (just created but not yet stored).
     * <p>
     * A property is new when added to a collection. This is the typical state
     * for a new property added to a list
     *
     * @return true if this property is new, false otherwise
     */
    boolean isNew();

    /**
     * Tests if a property is flagged as removed. Removed properties are child
     * property that were removed from their container.
     *
     * @return if the property was removed, false otherwise
     */
    boolean isRemoved();

    /**
     * Tests if a property value was modified.
     *
     * @return if the property was removed, false otherwise
     */
    boolean isModified();

    /**
     * Tests if a property value was moved to another index in the parent list
     * if any.
     *
     * @return if the property was removed, false otherwise
     */
    boolean isMoved();

    /**
     * Tests if the property is a phantom. This means it doesn't exists yet in
     * the storage and it is not a new property. This is a placeholder for a
     * property that is defined by the schema but was not yet set.
     *
     * @return true if a phantom false otherwise
     */
    boolean isPhantom();

    /**
     * Tests whether a property is dirty.
     * <p>
     * This tests whether or not a dirty flag is set on the property.
     *
     * @return true if the property changed
     */
    boolean isDirty();

    /**
     * Get the dirty flags that are set on this property.
     *
     * @return the dirty flags mask
     */
    int getDirtyFlags();

    /**
     * Notify the property that its changes was stored so it can safely remove
     * dirty flags.
     * <p>
     * Dirty flags are removed according to the type of the modifications.
     * This way if the property was REMOVED it becomes a PHANTOM otherwise all
     * dirty flags are cleared.
     * <p>
     * This method should be used by storage implementors to notify the property
     * it should reset its dirty flags. Note that clearing dirty flags is not
     * propagated to the parent property or to children. You need to clear dirty
     * flags explicitly for each property.
     */
    void clearDirtyFlags();

    /**
     * Whether the property is read only.
     *
     * @return true if read only false otherwise
     */
    boolean isReadOnly();

    /**
     * Checks whether this property is validating values when set.
     *
     * @return true if validating false otherwise
     */
    boolean isValidating();

    /**
     * Sets the read only flag.
     *
     * @param value true to set this property read only false otherwise
     */
    void setReadOnly(boolean value);

    /**
     * Sets the validating flag.
     *
     * @param value true to put validating on false otherwise
     */
    void setValidating(boolean value);

    /**
     * Tests whether this property is of a map (complex) type.
     *
     * @return true if the property is of map type, false otherwise
     */
    boolean isComplex();

    /**
     * Tests whether this property is of a list type.
     *
     * @return true if the property is of list type, false otherwise
     */
    boolean isList();

    /**
     * Tests whether this property is of a scalar type.
     *
     * @return true if the property is of a scalar type, false otherwise
     */
    boolean isScalar();

    /**
     * Whether this property is a container - this means the property value is a
     * map or a list.
     * <p>
     * Container properties don't have a scalar values. Container values are
     * computed each time they are requested - by calling on of the
     * <code>getValue</code> methods - by collecting the values of the child
     * properties.
     *
     * @return true if scalar false otherwise
     */
    boolean isContainer();

    /**
     * Gets the property name.
     *
     * @return the property name
     */
    String getName();

    /**
     * Gets the path of this property relative to the owner document.
     * <p>
     * The path for top level properties is the same to the property name.
     *
     * @return the path
     */
    String getPath();

    /**
     * Get the type of the field corresponding to this property.
     *
     * @return the property type
     */
    Type getType();

    /**
     * Gets the field corresponding to this property.
     * <p>
     * The field is the object defining the property. You can see the field as a
     * java class and the property as a class instance
     *
     * @return
     */
    Field getField();

    /**
     * Gets the property parent.
     *
     * @return the property parent for sub properties or null for top level
     *         properties
     */
    Property getParent();

    /**
     * Gets the document schema defining the property tree from which the
     * property belongs.
     *
     * @return the document schema owning the field corresponding to the
     *         property
     */
    Schema getSchema();

    /**
     * Gets the root property.
     *
     * @return the root property
     */
    DocumentPart getRoot();

    /**
     * Initializes the property with the given normalized value.
     * <p>
     * The given value must be normalized - note that no check is done on that.
     * <p>
     * The phantom flag is unset by this operation.
     * <p>
     * This method should be used to initialize properties.
     *
     * @param value the normalized value to set
     *
     */
    void init(Serializable value) throws PropertyException;

    /**
     * Sets this property value. The value will be first normalized and then
     * set.
     * <p>
     * For complex or list properties the value will be set recursively (as a
     * map or list value).
     *
     * @param value the value to set
     *
     * @throws {@link InvalidPropertyValueException} if the given value type is
     *             not compatible with the expected value type
     */
    void setValue(Object value) throws PropertyException;

    /**
     * Gets the property normalized value.
     * <p>
     * Normalized values are of the java type that correspond to the field type.
     *
     * @return the property value, which may be null
     */
    Serializable getValue() throws PropertyException;

    /**
     * Gets the property normalized value for write.
     * <p>
     * Can be different fropm {@link #getValue()} in cases where the property
     * adapts the value it is given to store.
     *
     * @return the property value to use for write, which may be null
     * @since 5.2.1
     */
    Serializable getValueForWrite() throws PropertyException;

    /**
     * Gets the property value as the given type.
     * <p>
     * The value is converted using the registered converter to the given type.
     * <p>
     * If conversion is not supported a runtime exception will be triggered.
     *
     * @return the property value, which may be null
     */
    <T> T getValue(Class<T> type) throws PropertyException;

    /**
     * Removes this property from the tree.
     * <p>
     * This method marks the property as dirty and sets its value to null.
     *
     * @return the old property value
     */
    Serializable remove() throws PropertyException;

    /**
     * Gets the child property having the given name.
     * <p>
     * If the property is a scalar, this will return always null.
     * <p>
     * The given name should be the full name (i.e. prefixed name if any prefix
     * exists).
     * <p>
     * If a non prefixed name is given, the first child property having the
     * given local name will be returned.
     * <p>
     * Relative paths are not resolved. THis method is intended to lookup direct
     * children. For path lookups use {@link Property#resolvePath(String)}
     * instead.
     *
     * @param name the child property name (the full name including the prefix
     *            if any)
     * @return the child property if any null if no child property with that
     *         name is found or if the property is a scalar
     * @throws {@link UnsupportedOperationException} if the property is a scalar
     *             property (doesn't have children)
     * @throws {@link PropertyNotFoundException} if the child property is not
     *             found in the type definition
     */
    Property get(String name) throws PropertyNotFoundException;

    /**
     * Get the child property given it's index. This operation is mandatory for
     * List properties.
     * <p>
     * If this method is not supported an {@link UnsupportedOperationException}
     * must be thrown
     * <p>
     * Relative paths are not resolved. THis method is intended to lookup direct
     * chilren. For path lookups, use {@link Property#resolvePath(String)}
     * instead.
     *
     * @param index
     * @return the child property if any null if no child property with that
     *         name is found or if the property is a scalar
     * @throws {@link UnsupportedOperationException} if the property is a scalar
     *             property (doesn't have children)
     * @throws {@link PropertyNotFoundException} if the child property is not
     *             found in the type definition
     */
    Property get(int index) throws PropertyNotFoundException;

    /**
     * Sets a child property value given its index. This method is required only
     * for List properties.
     * <p>
     * If this method is not supported, an {@link UnsupportedOperationException}
     * must be thrown.
     * <p>
     * This method will mark the child value as dirty for existing values and in
     * the case of map properties it will mark phantom properties as new
     * properties.
     *
     * @param index
     * @param value the new value
     * @throws {@link UnsupportedOperationException} if the property is a scalar
     *             property (doesn't have children)
     * @throws {@link PropertyNotFoundException} if the child property is not
     *             found in the type definition
     */
    void setValue(int index, Object value) throws PropertyException;

    /**
     * Get a collection over the children properties. This includes all children
     * including phantom ones (those who are not yet set by the user).
     * <p>
     * The returned collection is ordered for list properties, and unordered for
     * complex properties
     * <p>
     * Be aware that this method is creating phantom child properties for all
     * schema fields that are not yet set.
     *
     * @return the children properties
     */
    Collection<Property> getChildren();

    /**
     * Get the count of the children properties. This includes phantom
     * properties. So the returned size will be equal to the one returned by the
     * property {@link ComplexType#getFieldsCount()}.
     *
     * @return the children properties count
     */
    int size();

    /**
     * Appends a new value to the list. A new property will be created to store
     * the given value and appended to the children list.
     * <p>
     * The created property will be marked as {@link Property#isNew()}.
     *
     * @param value
     * @return the added property
     */
    Property addValue(Object value) throws PropertyException;

    /**
     * Inserts at the given position a new value to the list. A new property
     * will be created to store the given value and appended to the children
     * list.
     * <p>
     * The created property will be marked as {@link Property#isNew()}.
     *
     * @param value
     * @param index the position to insert the value
     * @return the added property
     */
    Property addValue(int index, Object value) throws PropertyException;

    /**
     * Creates an empty child property and adds it as a property to the list
     * container.
     * <p>
     * This method is useful to construct lists.
     *
     * @return the created property
     * @throws PropertyException
     */
    Property addEmpty() throws PropertyException;

    /**
     * Moves a property position into the parent container list.
     * <p>
     * This method applies only for list item properties. The given index
     * includes removed properties.
     *
     * @param index the position in the parent container to move this property
     * @throws UnsupportedOperationException if the operation is not supported
     *             by the target property
     */
    void moveTo(int index);

    /**
     * Same as {@link Property#resolvePath(Path)} but with a string path as
     * argument. This is the same as calling <code>resolvePath(new Path(path))</code>.
     *
     * @param path the string path to resolve.
     * @return the resolved property
     * @throws PropertyNotFoundException if the path cannot be resolved
     */
    Property resolvePath(String path) throws PropertyNotFoundException;

    /**
     * Resolves the given path relative to the current property and return the
     * property if any is found otherwise throws an exception.
     * <p>
     * The path format is a subset of XPath. Thus, / is used as path element
     * separator, [n] for list element indexes. Attribute separator '@' are not
     * supported since all properties are assumed to be elements. Also you ..
     * and . can be used as element names.
     * <p>
     * Example of paths:
     * <ul>
     * <li><code>dc:title</code>
     * <li><code>attachments/item[2]/mimeType</code>
     * <li><code>../dc:title</code>
     * </ul>
     *
     * @param path the path to resolve.
     * @return the resolved property
     * @throws PropertyNotFoundException if the path cannot be resolved
     */
    Property resolvePath(Path path) throws PropertyNotFoundException;

    /**
     * Gets the value of the property resolved using the given path.
     * <p>
     * This method is a shortcut for: <code>resolvePath(path).getValue()</code>.
     *
     * @param path the path to the property
     * @return the property value
     */
    Serializable getValue(String path) throws PropertyException;

    /**
     * Gets the value of the property resolved using the given path.
     * <p>
     * The value will be converted to the given type if possible, otherwise an
     * exception will be thrown.
     * <p>
     * This method is a shortcut for:
     * <code>resolvePath(path).getValue(type)</code>.
     *
     * @param <T> The type of the value to return
     * @param type the class of the value
     * @param path the java path of the property value
     * @return the value
     * @throws PropertyException
     */
    <T> T getValue(Class<T> type, String path) throws PropertyException;

    /**
     * Sets the value of the property resolved using the given path.
     * <p>
     * This method is a shortcut for:
     * <code>resolvePath(path).setValue(value)</code>.
     *
     * @param path the property path
     * @param value the value
     * @throws PropertyException
     */
    void setValue(String path, Object value) throws PropertyException;

    /**
     * Normalizes the given value as dictated by the property type.
     * <p>
     * Normalized values are the ones that are used for transportation over the
     * net and that are given to the storage implementation to be stored in the
     * repository
     * <p>
     * Normalized values must be {@link Serializable}
     * <p>
     * If the given value is already normalized it will be returned back.
     *
     * @param value the value to normalize according to the property type
     * @return the normalized value
     */
    Serializable normalize(Object value) throws PropertyConversionException;

    /**
     * Checks if the given value is a normalized one. This means the value has a
     * type that is normalized.
     * <p>
     * Null values are considered as normalized.
     *
     * @param value the value to check
     * @return true if the value is normalized false otherwise
     */
    boolean isNormalized(Object value);

    /**
     * Converts the given normalized value to the given type.
     * <p>
     * If the value has already the given type it will be returned back.
     *
     * @param value the normalized value to convert
     * @param toType the conversion type
     * @return the converted value, which may be null
     *
     * @throws PropertyConversionException if the conversion cannot be made
     *             because of type incompatibilities
     */
    <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException;

    /**
     * Validates the given value type.
     * <p>
     * Tests if the given value type can be converted to a normalized type and
     * thus a value of this type can be set to that property.
     *
     * @param type the type to validate
     * @return true if the type is valid, false otherwise
     */
    boolean validateType(Class<?> type);

    /**
     * Validates the given normalized value.
     * <p>
     * Only normalized values can be validated.
     * <p>
     * If the value is not validated, returns false.
     *
     * @param value the value to validate
     * @return true if the value is valid, false otherwise
     *
     * @see Property#validateType(Class)
     */
    boolean validate(Serializable value);

    /**
     * Creates a new and empty instance of a normalized value.
     * <p>
     * Empty is used in the sense of a value that has not been initialized or
     * can be considered as an empty value. For example for the {@link String}
     * type the empty value will be the empty string ""
     *
     * @return the empty instance the empty instance, or null for some
     *         implementations
     */
    Object newInstance();

    /**
     * Method that implement the visitor pattern.
     * <p>
     * The visitor must return null to stop visiting children otherwise a
     * context object that will be passed as the arg argument to children
     *
     * @param visitor the visitor to accept
     * @param arg an argument passed to the visitor. This should be used by the
     *            visitor to carry on the visiting context.
     */
    void accept(PropertyVisitor visitor, Object arg) throws PropertyException;

    /**
     * Compare the two properties by content.
     *
     * @param property
     * @return true If the properties have a similar content, otherwise false
     * @throws PropertyException
     */
    boolean isSameAs(Property property) throws PropertyException;

    /**
     * Gets an iterator over the dirty children properties.
     *
     * @return the iterator
     */
    Iterator<Property> getDirtyChildren();

    /**
     * Sets the application-defined data to associated it with the receiver
     * property.
     * <p>
     * The property data is reserved for the implementation and you must not
     * directly set it. Data attached with properties must be
     * {@link Serializable} objects if you want to serialize them along with the
     * property
     *
     * @param value
     */
    void setData(Object value);

    /**
     * Sets the application defined data associated with the receiver under the
     * given key.
     * <p>
     * The property data is reserved for the implementation and you must not
     * directly set it. Data attached with properties must be
     * {@link Serializable} objects if you want to serialize them along with the
     * property.
     *
     * @param key
     * @param value
     */
    void setData(String key, Object value);

    /**
     * Returns the application defined data associated with the receiver, or
     * null if it has not been set.
     * <p>
     * The property data is reserved for the implementation and you must not
     * directly set it. Data attached with properties must be
     * {@link Serializable} objects if you want to serialize them along with the
     * property.
     */
    Object getData();

    /**
     * Returns the application defined data associated with the receiver under
     * the given key, or null if it has not been set.
     * <p>
     * You are free to set any data you want on properties but be aware to use
     * {@link Serializable} objects if you want to serialize them along with the
     * property.
     *
     * @param key
     */
    Object getData(String key);

}
