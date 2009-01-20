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
 * $Id: ValidatorsRuleDesc.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.rules;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publishing.api.ValidatorsRule;

/**
 * Validators rule descriptor.
 * <p>
 * Mostly references the underlying <code>ValidatorRule</code> implementation.
 * It might be useful in the future to extend the members of this object to hold
 * other informations about the way we should apply this validator.
 * (placefulness for instance).
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("publishingValidatorsRule")
public class ValidatorsRuleDesc implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<ValidatorsRule> klass;

    public Class<ValidatorsRule> getKlass() {
        return klass;
    }

}
