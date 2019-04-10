/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants;

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
    public List<String> getActors() throws ClientException {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_ACTORS_PROPERTY_NAME);
    }

    @Override
    public String getName() throws ClientException {
        return doc.getTitle();
    }

    @Override
    public String getDirective() throws ClientException {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_DIRECTIVE_PROPERTY_NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getComments() throws ClientException {
        return (List<String>) getPropertyValue(RoutingTaskConstants.TASK_STEP_COMMENTS_PROPERTY_NAME);
    }

    @Override
    public Date getDueDate() throws ClientException {
        return getDatePropertyValue(RoutingTaskConstants.TASK_STEP_DUE_DATE_PROPERTY_NAME);
    }

    @Override
    public Boolean hasAutomaticValidation() throws ClientException {
        return getPropertyValue(RoutingTaskConstants.TASK_STEP_AUTOMATIC_VALIDATION_PROPERTY_NAME);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(String propertyName) {
        try {
            return (T) doc.getPropertyValue(propertyName);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Date getDatePropertyValue(String propertyName) {
        try {
            Calendar cal = (Calendar) doc.getPropertyValue(propertyName);
            if (cal != null) {
                return cal.getTime();
            }
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return null;
    }
}
