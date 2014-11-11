package org.nuxeo.ecm.webapp.security.policies;

import java.util.Comparator;
import java.util.List;

import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.webapp.security.SecurityData;
import org.nuxeo.ecm.webapp.security.SecurityDataPolicy;

import edu.emory.mathcs.backport.java.util.Collections;

public class SortedSecurityDataPolicy  implements SecurityDataPolicy {

    private static final long serialVersionUID = 1L;

    protected final Comparator<UserEntry> comparator;
    protected final DefaultSecurityDataPolicy support = new DefaultSecurityDataPolicy();

    protected SortedSecurityDataPolicy(Comparator<UserEntry> comparator) {
        this.comparator = comparator;
    }


    public List<UserEntry> compute(SecurityData securityData) {
        List<UserEntry> entries =  support.compute(securityData);
        Collections.sort(entries, comparator);
        return entries;
    }

}
