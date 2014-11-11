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
 * $Id: WMProcessInstance.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;
import java.util.Date;

/**
 * Process instance.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMProcessInstance extends Serializable {

    /**
     * Returns the process instance id.
     *
     * @return the process instance id
     */
    String getId();

    /**
     * Returns the name of the related process definition.
     *
     * @return the name of the related process definition
     */
    String getName();

    /**
     * Returns the process instance state.
     * <p>
     * :XXX: It should be a dedicated object instead of a string here.
     *
     * @return the process instance state as a String
     */
    String getState();

    /**
     * Sets the process instance state.
     */
    void setState(String state);

    /**
     * Returns the corresponding process definition.
     *
     * @return the corresponding process definition
     */
    WMProcessDefinition getProcessDefinition();

    /**
     * Returns the date when the process has been started.
     *
     * @return the date when the process has been started
     */
    Date getStartDate();

    /**
     * Returns the date when the process has been stopped.
     *
     * @return the date when the process has been stopped
     */
    Date getStopDate();

    /**
     * Return the participant who initiated this instance.
     *
     * @return a WMParticipant instance
     */
    WMParticipant getAuthor();

    /**
     * Returns the author name.
     *
     * @return the author name
     */
    String getAuthorName();

}
