/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.name.DefaultStringNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Helper class to parse and evaluate if a LDAP filter expression matches a
 * fetched LDAP entry.
 * <p>
 * This is done by recursively evaluating the abstract syntax tree of the
 * expression as parsed by an apache directory shared method.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class LDAPFilterMatcher {

    private final FilterParser parser;

    // lazily initialized normalizer for the substring match
    private Normalizer normalizer;

    LDAPFilterMatcher() {
        parser = new FilterParserImpl();
    }

    /**
     * Check whether a raw string filter expression matches on the given LDAP
     * entry.
     *
     * @param attributes the ldap entry to match
     * @param filter a raw string filter expression (eg.
     *            <tt>(!(&(attr1=*)(attr2=value2)(attr3=val*)))</tt> )
     * @return true if the ldap entry matches the filter
     * @throws DirectoryException if the filter is not a valid LDAP filter
     */
    public boolean match(Attributes attributes, String filter)
            throws DirectoryException {
        if (filter == null || "".equals(filter)) {
            return true;
        }
        try {
            ExprNode parsedFilter = parser.parse(filter);
            return recursiveMatch(attributes, parsedFilter);
        } catch (Exception e) {
            throw new DirectoryException("could not parse LDAP filter: "
                    + filter, e);
        }
    }

    private boolean recursiveMatch(Attributes attributes, ExprNode filterElement)
            throws DirectoryException {
        if (filterElement instanceof PresenceNode) {
            return presenceMatch(attributes, (PresenceNode) filterElement);
        } else if (filterElement instanceof SimpleNode) {
            return simpleMatch(attributes, (SimpleNode) filterElement);
        } else if (filterElement instanceof SubstringNode) {
            return substringMatch(attributes, (SubstringNode) filterElement);
        } else if (filterElement instanceof BranchNode) {
            return branchMatch(attributes, (BranchNode) filterElement);
        } else {
            throw new DirectoryException("unsupported filter element type: "
                    + filterElement);
        }
    }

    /**
     * Handle attribute presence check (eg: <tt>(attr1=*)</tt>)
     */
    private boolean presenceMatch(Attributes attributes,
            PresenceNode presenceElement) {
        return attributes.get(presenceElement.getAttribute()) != null;
    }

    /**
     * Handle simple equality test on any non-null value (eg:
     * <tt>(attr2=value2)</tt>).
     *
     * @return true if the equality holds
     */
    private static boolean simpleMatch(Attributes attributes,
            SimpleNode simpleElement) throws DirectoryException {
        Attribute attribute = attributes.get(simpleElement.getAttribute());
        if (attribute == null) {
            // null attribute cannot match any equality statement
            return false;
        }
        try {
            NamingEnumeration<?> rawValues = attribute.getAll();
            while (rawValues.hasMore()) {
                String rawValue = rawValues.next().toString();
                if (simpleElement.getValue().equals(rawValue)) {
                    return true;
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException(
                    "could not retrieve value for attribute: "
                            + simpleElement.getAttribute());
        }
        return false;
    }

    /**
     * Implement the substring match on any non-null value of a string attribute
     * (eg: <tt>(attr3=val*)</tt>).
     *
     * @return the result of the regex evaluation
     */
    private boolean substringMatch(Attributes attributes,
            SubstringNode substringElement) throws DirectoryException {
        try {
            Attribute attribute = attributes.get(substringElement.getAttribute());
            if (attribute == null) {
                // null attribute cannot match any regex
                return false;
            }
            NamingEnumeration<?> rawValues = attribute.getAll();
            while (rawValues.hasMore()) {
                String rawValue = rawValues.next().toString();
                Normalizer normalizer = getNormalizer();
                Pattern pattern;
                try {
                    pattern = substringElement.getRegex(normalizer);
                } catch (Exception e) {
                    throw new DirectoryException(
                            "could not build regexp for substring: "
                                    + substringElement.toString());
                }
                if (pattern.matcher(rawValue).matches()) {
                    return true;
                }
            }
            return false;
        } catch (NamingException e1) {
            throw new DirectoryException(
                    "could not retrieve value for attribute: "
                            + substringElement.getAttribute());
        }
    }

    private Normalizer getNormalizer() {
        if (normalizer == null) {
            normalizer = new DefaultStringNormalizer();
        }
        return normalizer;
    }

    /**
     * Handle conjunction, disjunction and negation nodes and recursively call
     * the generic matcher on children.
     *
     * @return the boolean value of the evaluation of the sub expression
     */
    private boolean branchMatch(Attributes attributes, BranchNode branchElement)
            throws DirectoryException {
        if (branchElement.isConjunction()) {
            for (ExprNode child : branchElement.getChildren()) {
                if (!recursiveMatch(attributes, child)) {
                    return false;
                }
            }
            return true;
        } else if (branchElement.isDisjunction()) {
            for (ExprNode child : branchElement.getChildren()) {
                if (recursiveMatch(attributes, child)) {
                    return true;
                }
            }
            return false;
        } else if (branchElement.isNegation()) {
            return !recursiveMatch(attributes, branchElement.getChild());
        } else {
            throw new DirectoryException(
                    "unsupported branching filter element type: "
                            + branchElement.toString());
        }
    }

}
