/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client;

import com.google.gwt.i18n.client.Constants;

/**
 * Internationalization of gwt container ContainerConstants.properties
 * 
 * @author Guillaume Cusnieux
 */
public interface ContainerConstants extends Constants {
    String error();

    String loadingError();

    String savePreferencesError();

    String deleteConfirm();

    String colorChoice();

    String save();

    String cancel();

    String search();

    String addGadget();

    String add();

    String addGadgetError();

    String loadGadgetError();

    String modifyLayout();

    String appearance();

    String close();

    String deleteGadget();

    String deleteError();

    String noImageDisplay();

    String createdBy();

    String select();

    String previous();

    String next();

    String on();
}
