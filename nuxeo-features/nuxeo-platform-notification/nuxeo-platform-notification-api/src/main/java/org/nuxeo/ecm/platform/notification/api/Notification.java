/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    /**
     * @return the mvelExpr used to evaluate the mail template name
     * @since 5.6
     */
    String getTemplateExpr();

}
