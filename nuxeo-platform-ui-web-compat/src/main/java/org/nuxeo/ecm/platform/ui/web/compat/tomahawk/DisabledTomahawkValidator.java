/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.compat.tomahawk;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * Validator issuing a validation error with a disabled message.
 *
 * @author Anahide Tchertchian
 */
public class DisabledTomahawkValidator implements Validator {

    public static final String DISABLED_VALIDATOR_MESSAGE = "This tomahawk validator is disabled. Please use another jsf library";

    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {
        throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, DISABLED_VALIDATOR_MESSAGE, null));
    }

}
