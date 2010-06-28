package org.nuxeo.ecm.webapp.security.policies;

import java.util.Comparator;
import java.util.List;

import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.webapp.security.SecurityData;

import edu.emory.mathcs.backport.java.util.Collections;

public class SortedSecurityDataPolicy  extends DefaultSecurityDataPolicy {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    protected final Comparator<UserEntry> comparator;

    protected SortedSecurityDataPolicy(Comparator<UserEntry> comparator) {
        this.comparator = comparator;
    }


    @Override
    public List<UserEntry> compute(SecurityData securityData) {
        List<UserEntry> entries =  super.compute(securityData);
        Collections.sort(entries, comparator);
        return entries;
    }

}
