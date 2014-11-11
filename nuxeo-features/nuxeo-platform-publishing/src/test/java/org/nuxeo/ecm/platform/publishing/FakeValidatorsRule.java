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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: FakeValidatorsRule.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.ecm.platform.publishing.api.ValidatorsRule;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class FakeValidatorsRule implements ValidatorsRule {

    private static final long serialVersionUID = 5124173341571522375L;

    public String[] computesValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        // TODO Auto-generated method stub
        return null;
    }

}
