/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.queue.api;


// TODO: Auto-generated Javadoc
/**
 * An atomic item can be in the following state:
 * <dl>
 * <dt>Handled</dt>
 * <dd>content is currently handled by system</dd>
 * <dt>Orphaned</dt>
 * <dd>content known by system but not handled</dd>
 * </dl>
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public enum QueueItemState {

    /** The Handled. */
    Handled, /** The Orphaned. */
    Orphaned

}
