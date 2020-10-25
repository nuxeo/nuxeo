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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ValidatorsRuleDescriptor.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publisher.rules;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Validators rule descriptor.
 * <p>
 * Mostly references the underlying <code>ValidatorRule</code> implementation. It might be useful in the future to
 * extend the members of this object to hold other information about the way we should apply this validator.
 * (placefulness for instance).
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("validatorsRule")
public class ValidatorsRuleDescriptor {

    @XNode("@class")
    protected Class<ValidatorsRule> klass;

    @XNode("@name")
    private String name;

    public Class<ValidatorsRule> getKlass() {
        return klass;
    }

    public String getName() {
        return name;
    }

}
