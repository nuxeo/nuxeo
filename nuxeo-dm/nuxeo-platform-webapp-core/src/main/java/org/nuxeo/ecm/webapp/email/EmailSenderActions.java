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

package org.nuxeo.ecm.webapp.email;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Remove;

import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Provides email related operations.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
@Local
public interface EmailSenderActions extends StatefulBaseLifeCycle {

    @Remove
    @PermitAll
    void destroy();

    void initialize();

    void send();

    String getMailSubject();

    void setMailSubject(String mailSubject);

    String getMailContent();

    void setMailContent(String mailContent);

}
