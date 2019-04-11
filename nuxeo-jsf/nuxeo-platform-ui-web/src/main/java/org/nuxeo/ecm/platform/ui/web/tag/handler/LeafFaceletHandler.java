/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;

/**
 * Leaf Facelet Handler (facelet handler that does nothing).
 * <p>
 * Used when there is no next handler to apply, as next handler can never be null.
 *
 * @since 7.4
 */
public class LeafFaceletHandler implements FaceletHandler {

    public LeafFaceletHandler() {
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
    }

    @Override
    public String toString() {
        return "FaceletHandler Tail";
    }

}
