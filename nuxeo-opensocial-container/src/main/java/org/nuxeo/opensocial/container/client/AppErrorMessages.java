/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client;

import com.google.gwt.i18n.client.Messages;

/**
 * @author Stéphane Fourrier
 */
public interface AppErrorMessages extends Messages {
    String unitIsNotEmpty();

    String zoneIsNotEmpty();

    String noZoneCreated();

    String cannotLoadLayout();

    String cannotReachServer();

    String applicationNotCorrectlySet();

    String cannotUpdateLayout();

    String cannotUpdateFooter();

    String cannotCreateZone();

    String cannotUpdateZone();

    String cannotUpdateSideBar();

    String cannotUpdateHeader();

    String cannotDeleteZone();

    String cannotCreateWebContent();

    String cannotLoadWebContents();

    String cannotUpdateAllWebContents();

    String cannotUpdateWebContent();

    String cannotDeleteWebContent();

    String cannotLoadContainerBuilder();

    String cannotAddExternalWebContent(String type);

    String cannotFindWebContent();

    String preferenceDoesNotExist(String name);
}
