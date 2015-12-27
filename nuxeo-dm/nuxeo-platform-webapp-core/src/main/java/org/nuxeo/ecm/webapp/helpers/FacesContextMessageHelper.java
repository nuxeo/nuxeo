/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Use &lt;h:messages/&gt; to display them client side.
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
     * Appends a FacesMessage to the set of messages associated with the specified client identifier, if clientId is not
     * null.
     *
     * @param fctx the FacesContext
     * @param content the actual message content.
     */
    public static void addMessageToFctx(FacesContext fctx, String clientId, String content) {
        if (fctx != null && clientId != null) {
            fctx.addMessage(clientId, getFacesMessageFor(content));
        }
    }

}
