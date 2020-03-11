/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event;

/**
 * Post-commit listener that can decide synchronously whether it's worth calling.
 * <p>
 * This is useful if there are quick decisions that can be made synchronously, and to avoid creating useless thread
 * work.
 *
 * @since 5.6
 */
public interface PostCommitFilteringEventListener extends PostCommitEventListener {

    /**
     * Checks if this event is worth passing to the asynchronous {@link #handleEvent}.
     * <p>
     * Note that the event's documents are usually <strong>disconnected</strong> into
     * {@link org.nuxeo.ecm.core.event.impl.ShallowDocumentModel ShallowDocumentModel} instances, which means that this
     * method may not be able to get to all the information it would get from a standard DocumentModel implementation.
     * If there is not enough information in the ShallowDocumentModel to decide whether this event is of interest, then
     * this method should accept it an let the actual logic done in {@link #handleEvent} do the final filtering.
     *
     * @param event the event
     * @return {@code true} to accept it, or {@code false} to ignore it
     * @since 5.6
     */
    boolean acceptEvent(Event event);

}
