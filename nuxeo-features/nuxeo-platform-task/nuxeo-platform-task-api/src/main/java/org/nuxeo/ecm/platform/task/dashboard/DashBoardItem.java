/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.task.dashboard;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Item holding information about a Document with a task attached to it.
 * <p>
 * Aimed at being used in Dashboard fragments.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @since 5.5
 */
public interface DashBoardItem extends Serializable {

    /**
     * Returns the identifier of the workflow task instance.
     *
     * @return the identifier of the dash board item.
     */
    String getId();

    /**
     * Returns the name of the DashBoardItem.
     *
     * @return the name of the DashBoardItem
     */
    String getName();

    /**
     * Returns the document reference on which the item is bound.
     *
     * @return a document reference instance
     */
    DocumentRef getDocRef();

    /**
     * Returns the description of the item.
     *
     * @return the description of the item
     */
    String getDescription();

    /**
     * Returns the associated item comment.
     *
     * @return the associated item comment
     */
    String getComment();

    /**
     * Returns the date when the task has been started.
     *
     * @return the date when the task has been started
     */
    Date getStartDate();

    /**
     * Returns the date at which the task needs to be closed.
     *
     * @return the date at which the task needs to be closed
     */
    Date getDueDate();

    /**
     * Returns the item associated directive.
     *
     * @return the item associated directive
     */
    String getDirective();

    /**
     * Returns the icon path for the doc ref.
     *
     * @return the icon path for the doc ref
     */
    DocumentModel getDocument();

    /**
     * Does the user reach the deadline?
     *
     * @return the expired flag.
     */
    boolean isExpired();

    /**
     * Returns the underneath task instance
     */
    Task getTask();

    /**
     * Defines the {@link Locale} that will be used to generate translations
     *
     * @param locale
     */
    void setLocale(Locale locale);

    /**
     * Get the Translated TaskName (Locale must be set)
     *
     * @return
     */
    String getI18nTaskName();

    /**
     * Get the Translated Task Directive (Locale must be set)
     *
     * @return
     */
    String getI18nDirective();

    /**
     * Returns JSON representation of the item as map
     */
    Map<String, Object> asMap();

    /**
     * Return the link url to the target Document
     *
     * @param includeWorkflowTab
     * @return
     */
    String getDocumentLink(boolean includeWorkflowTab);

}
