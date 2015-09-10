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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.uidgen;

/**
 * UID Sequencer interface defines a method to retrieve next ids based on a given key.
 *
 * @author <a href="mailto:dm@nuxeo.com>Dragos Mihalache</a>
 */
public interface UIDSequencer {

    /**
     * Gets the sequencer name.
     *
     * @since 7.4
     */
    String getName();

    /**
     * Sets the sequencer name.
     *
     * @since 7.4
     */
    void setName(String name);

    /**
     * Init Sequencer
     *
     * @since 7.3
     */
    void init();

    /**
     * Initializes the sequencer with the given key to at least the given id.
     * <p>
     * A sequence can only be incremented, so if its current id is greater than the given id the sequence won't be
     * decremented to reach the given id.
     *
     * @since 7.4
     */
    void initSequence(String key, int id);

    /**
     * For the given key returns the incremented UID which is also stored in the same sequence entry. This is a
     * "one time use" function for a document.
     *
     * @param key
     * @return
     */
    int getNext(String key);

    /**
     * Cleanup callback
     *
     * @since 7.3
     */
    void dispose();

}
