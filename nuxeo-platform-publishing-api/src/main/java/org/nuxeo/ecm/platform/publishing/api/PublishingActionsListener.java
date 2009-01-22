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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingActionsListener.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.api;

import java.io.Serializable;

/**
 * Publishing actions listener base interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface PublishingActionsListener extends Serializable {

    /**
     * Publish the document.
     *
     * @return a jsf view id for the redirection.
     * @throws PublishingException
     */
    String publishDocument() throws PublishingException;

    /**
     * Reject the document.
     *
     * @return a jsf view id.
     * @throws PublishingException
     */
    String rejectDocument() throws PublishingException;

    /**
     * Can the current user manage the publishing.
     *
     * @return true if whether or not the current user can publish/reject the
     *         document.
     * @throws PublishingException
     */
    boolean canManagePublishing() throws PublishingException;

    /**
     * Is the document a proxy ?
     *
     * @return if whether or not the current document is a proxy.
     * @throws PublishingException
     */
    boolean isProxy() throws PublishingException;

    // Dummy getters / setters

    String getRejectPublishingComment();

    void setRejectPublishingComment(String rejectPublishingComment);

}
