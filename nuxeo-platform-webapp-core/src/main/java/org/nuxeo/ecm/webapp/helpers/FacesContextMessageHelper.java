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
 * $Id: FacesContextMessageHelper.java 19451 2007-05-27 09:03:18Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Helper to generate JSF context messages.
 * <p>
 * Use  &lt;h:messages/&gt; to display them client side.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Deprecated
// TODO: remove (not used)
public final class FacesContextMessageHelper {

    // Utility class
    private FacesContextMessageHelper() {
    }

    /**
     * Returns a FacesMessage instance.
     *
     * @param content the actual message content.
     * @return a FacesMessage instance
     */
    public static FacesMessage getFacesMessageFor(String content) {
        return new FacesMessage(content);
    }

    /**
     * Appends a FacesMessage to the set of messages associated with the
     * specified client identifier, if clientId is not null.
     *
     * @param fctx
     *            the FacesContext
     * @param content
     *            the actual message content.
     */
    public static void addMessageToFctx(FacesContext fctx, String clientId, String content) {
        if (fctx != null && clientId != null) {
           fctx.addMessage(clientId, getFacesMessageFor(content));
        }
    }

}
