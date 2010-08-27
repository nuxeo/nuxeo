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
 * UID Sequencer interface defines a method to retrieve next ids based on a
 * given key.
 *
 * @author <a href="mailto:dm@nuxeo.com>Dragos Mihalache</a>
 */
public interface UIDSequencer {

    /**
     * For the given key returns the incremented UID which is also stored in the
     * same sequence entry. This is a "one time use" function for a document.
     *
     * @param key
     * @return
     */
    int getNext(String key);

}
