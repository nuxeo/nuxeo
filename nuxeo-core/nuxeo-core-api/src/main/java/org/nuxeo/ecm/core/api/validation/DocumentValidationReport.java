/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.validation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A managed set of {@link ConstraintViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationReport implements Iterable<ConstraintViolation> {

    protected List<ConstraintViolation> violations;

    public DocumentValidationReport(List<ConstraintViolation> violations) {
        super();
        this.violations = violations;
    }

    public boolean hasError() {
        return !violations.isEmpty();
    }

    public int numberOfErrors() {
        return violations.size();
    }

    public List<ConstraintViolation> asList() {
        return Collections.unmodifiableList(violations);
    }
    
	@Override
	public Iterator<ConstraintViolation> iterator() {
		return asList().iterator();
	}

    @Override
    public String toString() {
        if (violations != null) {
            StringBuilder res = new StringBuilder();
            for (ConstraintViolation violation : violations) {
                res.append(violation.getMessage(Locale.ENGLISH)).append('\n');
            }
            return res.toString();
        } else {
            return "no error";
        }
    }

}
