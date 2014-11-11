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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

/**
 * Object method for a message action.
 *
 * @author Alexandre Russel
 */
public interface MessageAction {

    /**
     * Executes one action.
     * <p>
     * If it returns false, the following actions from the pipe are not executed.
     *
     * @see ExecutionContext
     */
    boolean execute(ExecutionContext context) throws Exception;

    void reset(ExecutionContext context) throws Exception;

}
