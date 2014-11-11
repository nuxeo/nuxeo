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
 *     arussel
 */
package org.nuxeo.ecm.webapp.dashboard;

import java.util.List;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.theme.negotiation.Scheme;

/**
 * Set the perspective to 'multiple domains' if the user has access to more than
 * one domain.
 *
 * @author arussel
 */
public class MultipleDomainsPerspectiveNegotiator implements Scheme {

    private static final String MULTIPLE_DOMAINS_PERSPECTIVE_NAME = "multiple_domains";

    @SuppressWarnings("unchecked")
    public String getOutcome(Object context) {
        List<DocumentModel> availableDomains = (List) Component.getInstance("availableDomains");
        if (availableDomains != null && availableDomains.size() > 1) {
            return MULTIPLE_DOMAINS_PERSPECTIVE_NAME;
        }
        return null;
    }

}
