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
 * $Id: ValidatorsRule.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publisher.rules;

import org.nuxeo.ecm.core.api.DocumentModel;

import java.io.Serializable;

/**
 * Validators rule API.
 * <p>
 * Object aiming at being responsible of computing the validators of a just
 * published document.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ValidatorsRule extends Serializable {

    /**
     * Computes the list of publishing validators given the document model of
     * the document just published.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been
     *            published)
     * @return a list of principal names.
     * @throws PublishingValidatorException TODO
     */
    String[] computesValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException;

}
