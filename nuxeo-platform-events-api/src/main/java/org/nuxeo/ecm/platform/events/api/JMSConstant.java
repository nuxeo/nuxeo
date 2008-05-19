/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.events.api;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public interface JMSConstant {
    String NUXEO_MESSAGE_TYPE = "NuxeoMessageType";
    String DOCUMENT_MESSAGE = "DocumentMessage";
    String NXCORE_EVENT = "NXCoreEvent";
    String EVENT_MESSAGE = "EventMessage";
    String NUXEO_EVENT_ID = "NuxeoEventId";
}
