/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.web.resources.jsf.negotiators;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.styling.negotiation.AbstractNegotiator;

public final class RequestAttribute extends AbstractNegotiator {

    @Override
    public String getResult(String target, Object context) {
        FacesContext faces = null;
        if (context instanceof FacesContext) {
            faces = (FacesContext) context;
        } else {
            return null;
        }
        final Map<String, Object> parameters = faces.getExternalContext().getRequestMap();
        return (String) parameters.get(getProperty("param"));
    }

}
