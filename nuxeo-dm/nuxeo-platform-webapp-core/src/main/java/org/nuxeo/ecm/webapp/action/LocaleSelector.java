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
package org.nuxeo.ecm.webapp.action;


import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

import javax.faces.application.FacesMessage;

@org.jboss.seam.annotations.Name("org.jboss.seam.international.localeSelector")
@org.jboss.seam.annotations.Install(precedence = Install.APPLICATION)
public class LocaleSelector extends org.jboss.seam.international.LocaleSelector {

    @In(required=false) protected FacesMessages facesMessages;

    @In(required=false) protected NavigationContext navigationContext;

    @In(required=false) protected ResourcesAccessor resourcesAccessor;

    @Override public void select() {
        if (navigationContext.isCreationEntered()) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR, "Cannot select language, creation flow entered, NXP-XXXX");
            return;
        }
        super.select();
    }
}
