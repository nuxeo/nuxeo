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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event;

/**
 * Post-commit listener that can decide synchronously whether it's worth
 * calling.
 * <p>
 * This is useful if there are quick decisions that can be made synchronously,
 * and to avoid creating useless thread work.
 *
 * @since 5.6
 */
public interface PostCommitFilteringEventListener extends
        PostCommitEventListener {

    /**
     * Checks if this event is worth passing to the asynchronous
     * {@link #handleEvent}.
     *
     * @param event the event
     * @return {@code true} to accept it, or {@code false} to ignore it
     *
     * @since 5.6
     */
    boolean acceptEvent(Event event);

}
