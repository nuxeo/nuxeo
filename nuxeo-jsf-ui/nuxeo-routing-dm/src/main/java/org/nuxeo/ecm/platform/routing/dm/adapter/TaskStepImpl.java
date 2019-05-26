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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants;

/**
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class TaskStepImpl implements TaskStep {

    protected DocumentModel doc;

    public TaskStepImpl(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public String getId() {
        return doc.getId();
    }

    @Override
    public List<String> getActors() {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_ACTORS_PROPERTY_NAME);
    }

    @Override
    public String getName() {
        return doc.getTitle();
    }

    @Override
    public String getDirective() {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_DIRECTIVE_PROPERTY_NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getComments() {
        return (List<String>) getPropertyValue(RoutingTaskConstants.TASK_STEP_COMMENTS_PROPERTY_NAME);
    }

    @Override
    public Date getDueDate() {
        return getDatePropertyValue(RoutingTaskConstants.TASK_STEP_DUE_DATE_PROPERTY_NAME);
    }

    @Override
    public Boolean hasAutomaticValidation() {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_AUTOMATIC_VALIDATION_PROPERTY_NAME);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(String propertyName) {
        return (T) doc.getPropertyValue(propertyName);
    }

    protected Date getDatePropertyValue(String propertyName) {
        Calendar cal = (Calendar) doc.getPropertyValue(propertyName);
        if (cal != null) {
            return cal.getTime();
        }
        return null;
    }

}
