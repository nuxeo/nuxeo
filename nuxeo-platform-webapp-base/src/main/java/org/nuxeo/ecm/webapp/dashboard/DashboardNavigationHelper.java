/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DashBoardActionsBean.java 29574 2008-01-23 16:08:12Z gracinet $
 */
package org.nuxeo.ecm.webapp.dashboard;

/**
 * Encapsulate navigation to Dashboard in order to provide more flexibility
 *  - dashboard view may depend on User/Browser
 *  - dashbord may need pre-processing for lazy creation
 *
 * @author Thierry Delprat
 *
 */
public interface DashboardNavigationHelper {

    String navigateToDashboard();

}
