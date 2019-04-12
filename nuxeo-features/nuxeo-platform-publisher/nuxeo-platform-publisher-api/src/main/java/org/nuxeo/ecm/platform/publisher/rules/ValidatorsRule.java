/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ValidatorsRule.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publisher.rules;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Validators rule API.
 * <p>
 * Object aiming at being responsible of computing the validators of a just published document.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ValidatorsRule extends Serializable {

    /**
     * Computes the list of publishing validators given the document model of the document just published.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been published)
     * @return a list of principal names.
     */
    String[] computesValidatorsFor(DocumentModel dm);

}
