/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.nuxeo.common.persistence;

/**
 * Interface to a memento used for saving the important state of an object
 * in a form that can be persisted in the file system.
 * <p>
 * Mementos were designed with the following requirements in mind:
 * <ol>
 *  <li>Certain objects need to be saved and restored across platform sessions.
 *    </li>
 *  <li>When an object is restored, an appropriate class for an object might not
 *    be available. It must be possible to skip an object in this case.</li>
 *  <li>When an object is restored, the appropriate class for the object may be
 *    different from the one when the object was originally saved. If so, the
 *    new class should still be able to read the old form of the data.</li>
 * </ol>
 * <p>
 * Mementos meet these requirements by providing support for storing a
 * mapping of arbitrary string keys to primitive values, and by allowing
 * mementos to have other mementos as children (arranged into a tree).
 * A robust external storage format based on XML is used.
 * <p>
 * The key for an attribute may be any alpha numeric value.  However, the
 * value of <code>TAG_ID</code> is reserved for internal use.
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 *
 */
public interface Memento {

    /**
     * Special reserved key used to store the memento id
     * (value <code>"org.eclipse.ui.id"</code>).
     *
     * @see #getID()
     */
    String TAG_ID = "IMemento.internal.id"; //$NON-NLS-1$

    /**
     * Creates a new child of this memento with the given type.
     * <p>
     * The <code>getChild</code> and <code>getChildren</code> methods
     * are used to retrieve children of a given type.
     *
     * @param type the type
     * @return a new child memento
     * @see #getChild
     * @see #getChildren
     */
    Memento createChild(String type);

    /**
     * Creates a new child of this memento with the given type and id. The id is
     * stored in the child memento (using a special reserved key,
     * <code>TAG_ID</code>) and can be retrieved using <code>getId</code>.
     * <p>
     * The <code>getChild</code> and <code>getChildren</code> methods are
     * used to retrieve children of a given type.
     *
     * @param type the type
     * @param id the child id
     * @return a new child memento with the given type and id
     * @see #getID
     */
    Memento createChild(String type, String id);

    /**
     * Returns the first child with the given type id.
     *
     * @param type the type id
     * @return the first child with the given type
     */
    Memento getChild(String type);

    /**
     * Returns all children with the given type id.
     *
     * @param type the type id
     * @return an array of children with the given type
     */
    Memento[] getChildren(String type);

    /**
     * Returns the floating point value of the given key.
     *
     * @param key the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not a floating point number
     */
    Float getFloat(String key);

    /**
     * Returns the id for this memento.
     *
     * @return the memento id, or <code>null</code> if none
     * @see #createChild(String, String)
     */
    String getID();

    /**
     * Returns the integer value of the given key.
     *
     * @param key the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not an integer
     */
    Integer getInteger(String key);

    /**
     * Returns the boolean value of the given key.
     *
     * @param key the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not an integer
     */
    Boolean getBoolean(String key);

    /**
     * Returns the string value of the given key.
     *
     * @param key the key
     * @return the value, or <code>null</code> if the key was not found
     */
    String getString(String key);

    /**
     * Returns the data of the Text node of the memento. Each memento is allowed
     * only one Text node.
     *
     * @return the data of the Text node of the memento, or <code>null</code>
     *         if the memento has no Text node.
     * @since 2.0
     */
    String getTextData();

    /**
     * Sets the value of the given key to the given floating point number.
     *
     * @param key the key
     * @param value the value
     */
    void putFloat(String key, float value);

    /**
     * Sets the value of the given key to the given integer.
     *
     * @param key the key
     * @param value the value
     */
    void putInteger(String key, int value);

    /**
     * Copy the attributes and children from  <code>memento</code>
     * to the receiver.
     *
     * @param memento the Memento to be copied.
     */
    void putMemento(Memento memento);

    /**
     * Sets the value of the given key to the given string.
     *
     * @param key the key
     * @param value the value
     */
    void putString(String key, String value);

    /**
     * Sets the value of the given key to the given boolean.
     *
     * @param key the key
     * @param value the value
     */
    void putBoolean(String key, boolean value);

    /**
     * Sets the memento's Text node to contain the given data. Creates the Text node if
     * none exists. If a Text node does exist, it's current contents are replaced.
     * Each memento is allowed only one text node.
     *
     * @param data the data to be placed on the Text node
     * @since 2.0
     */
    void putTextData(String data);
}
