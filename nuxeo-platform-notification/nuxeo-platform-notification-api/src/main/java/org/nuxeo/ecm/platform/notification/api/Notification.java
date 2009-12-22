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
 * $Id$
 */

package org.nuxeo.ecm.platform.notification.api;

import java.io.Serializable;

public interface Notification extends Serializable {

    String EMAIL_NOTIFICATION = "email";

    /**
     * @return the name.
     */
    String getName();

    /**
     * @return the channel.
     */
    String getChannel();

    /**
     * @return the template.
     */
    String getTemplate();

    /**
     * @return the autoSubscribed.
     */
    boolean getAutoSubscribed();

    /**
     * @return the subject.
     */
    String getSubject();

    /**
     * @return the subject template.
     */
    String getSubjectTemplate();

    /**
     * @return the availableIn.
     */
    String getAvailableIn();

    String getLabel();

    boolean getEnabled();

}
