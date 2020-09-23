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
import java.util.List;
import java.util.stream.Collectors;

/**
 * A managed set of {@link ValidationViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationReport {

    protected List<ValidationViolation> violations;

    public DocumentValidationReport(List<ValidationViolation> violations) {
        super();
        this.violations = violations;
    }

    public boolean hasError() {
        return !violations.isEmpty();
    }

    public int numberOfErrors() {
        return violations.size();
    }

    public List<ValidationViolation> asList() {
        return Collections.unmodifiableList(violations);
    }

    @Override
    public String toString() {
        if (violations != null) {
            return violations.stream().map(v -> v.getMessage(null)).collect(Collectors.joining("\n"));
        } else {
            return "no error";
        }
    }

}
