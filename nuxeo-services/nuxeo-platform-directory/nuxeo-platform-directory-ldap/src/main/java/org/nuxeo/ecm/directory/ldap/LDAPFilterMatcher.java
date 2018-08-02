/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
 * Helper class to parse and evaluate if a LDAP filter expression matches a fetched LDAP entry.
 * <p>
 * This is done by recursively evaluating the abstract syntax tree of the expression as parsed by an apache directory
 * shared method.
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
     * Check whether a raw string filter expression matches on the given LDAP entry.
     *
     * @param attributes the ldap entry to match
     * @param filter a raw string filter expression (eg. <tt>(!(&(attr1=*)(attr2=value2)(attr3=val*)))</tt> )
     * @return true if the ldap entry matches the filter
     */
    public boolean match(Attributes attributes, String filter) {
        if (filter == null || "".equals(filter)) {
            return true;
        }
        try {
            ExprNode parsedFilter = parser.parse(filter);
            return recursiveMatch(attributes, parsedFilter);
        } catch (DirectoryException | IOException | ParseException e) {
            throw new DirectoryException("could not parse LDAP filter: " + filter, e);
        }
    }

    private boolean recursiveMatch(Attributes attributes, ExprNode filterElement) {
        if (filterElement instanceof PresenceNode) {
            return presenceMatch(attributes, (PresenceNode) filterElement);
        } else if (filterElement instanceof SimpleNode) {
            return simpleMatch(attributes, (SimpleNode) filterElement);
        } else if (filterElement instanceof SubstringNode) {
            return substringMatch(attributes, (SubstringNode) filterElement);
        } else if (filterElement instanceof BranchNode) {
            return branchMatch(attributes, (BranchNode) filterElement);
        } else {
            throw new DirectoryException("unsupported filter element type: " + filterElement);
        }
    }

    /**
     * Handle attribute presence check (eg: <tt>(attr1=*)</tt>)
     */
    private boolean presenceMatch(Attributes attributes, PresenceNode presenceElement) {
        return attributes.get(presenceElement.getAttribute()) != null;
    }

    /**
     * Handle simple equality test on any non-null value (eg: <tt>(attr2=value2)</tt>).
     *
     * @return true if the equality holds
     */
    protected static boolean simpleMatch(Attributes attributes, SimpleNode simpleElement) {
        Attribute attribute = attributes.get(simpleElement.getAttribute());
        if (attribute == null) {
            // null attribute cannot match any equality statement
            return false;
        }
        boolean isCaseSensitive = isCaseSensitiveMatch(attribute);
        try {
            NamingEnumeration<?> rawValues = attribute.getAll();
            try {
                while (rawValues.hasMore()) {
                    String rawValue = rawValues.next().toString();
                    if (isCaseSensitive || !(simpleElement.getValue() instanceof String)) {
                        if (simpleElement.getValue().equals(rawValue)) {
                            return true;
                        }
                    } else {
                        String stringElementValue = (String) simpleElement.getValue();
                        if (stringElementValue.equalsIgnoreCase(rawValue)) {
                            return true;
                        }
                    }
                }
            } finally {
                rawValues.close();
            }
        } catch (NamingException e) {
            throw new DirectoryException("could not retrieve value for attribute: " + simpleElement.getAttribute());
        }
        return false;
    }

    protected static boolean isCaseSensitiveMatch(Attribute attribute) {
        // TODO: introspect the content of
        // attribute.getAttributeSyntaxDefinition() to know whether the
        // attribute is case sensitive for exact match and cache the results.
        // fallback to case in-sensitive if syntax definition is missing
        return false;
    }

    protected static boolean isCaseSensitiveSubstringMatch(Attribute attribute) {
        // TODO: introspect the content of
        // attribute.getAttributeSyntaxDefinition() to know whether the
        // attribute is case sensitive for substring match and cache the
        // results.
        // fallback to case in-sensitive if syntax definition is missing
        return false;
    }

    /**
     * Implement the substring match on any non-null value of a string attribute (eg: <tt>(attr3=val*)</tt>).
     *
     * @return the result of the regex evaluation
     */
    protected boolean substringMatch(Attributes attributes, SubstringNode substringElement) {
        try {

            Attribute attribute = attributes.get(substringElement.getAttribute());
            if (attribute == null) {
                // null attribute cannot match any regex
                return false;
            }
            NamingEnumeration<?> rawValues = attribute.getAll();
            try {
                while (rawValues.hasMore()) {
                    String rawValue = rawValues.next().toString();
                    getNormalizer();
                    StringBuffer sb = new StringBuffer();
                    String initial = substringElement.getInitial();
                    String finalSegment = substringElement.getFinal();
                    if (initial != null && !initial.isEmpty()) {
                        sb.append(Pattern.quote((String) normalizer.normalize(initial)));
                    }
                    sb.append(".*");
                    for (Object segment : substringElement.getAny()) {
                        if (segment instanceof String) {
                            sb.append(Pattern.quote((String) normalizer.normalize(segment)));
                            sb.append(".*");
                        }
                    }
                    if (finalSegment != null && !finalSegment.isEmpty()) {
                        sb.append(Pattern.quote((String) normalizer.normalize(finalSegment)));
                    }
                    Pattern pattern;
                    try {
                        if (isCaseSensitiveSubstringMatch(attribute)) {
                            pattern = Pattern.compile(sb.toString());
                        } else {
                            pattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
                        }
                    } catch (PatternSyntaxException e) {
                        throw new DirectoryException("could not build regexp for substring: "
                                + substringElement.toString());
                    }
                    if (pattern.matcher(rawValue).matches()) {
                        return true;
                    }
                }
            } finally {
                rawValues.close();
            }
            return false;
        } catch (NamingException e1) {
            throw new DirectoryException("could not retrieve value for attribute: " + substringElement.getAttribute());
        }
    }

    private Normalizer getNormalizer() {
        if (normalizer == null) {
            normalizer = new DefaultStringNormalizer();
        }
        return normalizer;
    }

    /**
     * Handle conjunction, disjunction and negation nodes and recursively call the generic matcher on children.
     *
     * @return the boolean value of the evaluation of the sub expression
     */
    private boolean branchMatch(Attributes attributes, BranchNode branchElement) {
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
            throw new DirectoryException("unsupported branching filter element type: " + branchElement.toString());
        }
    }

}
