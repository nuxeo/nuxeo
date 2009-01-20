/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     alexandre
 */
package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.platform.publishing.api.PublishingException;

public interface ValidatorActionsService {

    String publishDocument() throws PublishingException;

    String rejectDocument() throws PublishingException;

    boolean isProxy();

    boolean canManagePublishing() throws PublishingException;

    String getRejectPublishingComment();

    void setRejectPublishingComment(String rejectPublishingComment);

}