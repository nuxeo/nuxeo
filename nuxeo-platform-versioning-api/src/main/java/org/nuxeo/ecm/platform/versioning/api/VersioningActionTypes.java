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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.api;

/**
 * Defines versioning incremention actions.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 * @deprecated i'm not used anywhere, i hoped to take over
 *             {@link VersioningActions} but he won. please don't have mercy and
 *             erase me when my time comes.
 */
// TODO : will replace defs from VersioningAction
@Deprecated
public enum VersioningActionTypes {

    ACTION_UNDEFINED, ACTION_AUTO_INCREMENT, ACTION_NO_INCREMENT, ACTION_INCREMENT_MINOR, ACTION_INCREMENT_MAJOR, ACTION_CASE_DEPENDENT, ACTION_QUERY_WORKFLOW

}
