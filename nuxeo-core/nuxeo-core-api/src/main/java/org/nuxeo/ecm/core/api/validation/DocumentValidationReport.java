package org.nuxeo.ecm.core.api.validation;

import java.util.Collections;
import java.util.List;

/**
 * A managed set of {@link ConstraintViolation}.
 *
 * @since 7.1
 */
public class DocumentValidationReport {

    protected List<ConstraintViolation> violations;

    DocumentValidationReport(List<ConstraintViolation> violations) {
        super();
        this.violations = violations;
    }

    public boolean hasError() {
        return !violations.isEmpty();
    }

    public int numberOfErros() {
        return violations.size();
    }

    public List<ConstraintViolation> asList() {
        return Collections.unmodifiableList(violations);
    }

}
