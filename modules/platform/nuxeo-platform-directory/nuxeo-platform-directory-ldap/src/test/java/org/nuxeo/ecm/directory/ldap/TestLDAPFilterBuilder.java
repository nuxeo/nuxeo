/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.ldap;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

public class TestLDAPFilterBuilder extends LDAPDirectoryTestCase {

    protected static final String USER_DIR = "userDirectory";

    @Inject
    protected DirectoryService directoryService;

    protected Session getSession() throws Exception {
        return directoryService.open(USER_DIR);
    }

    protected LDAPDirectory getDirectory() throws Exception {
        return (LDAPDirectory) directoryService.getDirectory(USER_DIR);
    }

    @Test
    public void testQueryBuilder() throws Exception {
        Calendar cal = new GregorianCalendar(2010, 0, 1, 12, 34, 56);
        try (Session session = getSession()) {
            QueryBuilder queryBuilder = //
                    new QueryBuilder().predicate(Predicates.not(Predicates.or(Predicates.eq("username", "user_1"),
                            Predicates.eq("username", "Administrator"))))
                                      .and(Predicates.gt("intField", Long.valueOf(123)))
                                      .and(Predicates.noteq("booleanField", Long.valueOf(1)))
                                      .and(Predicates.between("intField", Long.valueOf(123), Long.valueOf(456)))
                                      .and(Predicates.like("firstName", "jo%n"))
                                      .and(Predicates.notlike("firstName", "bob"))
                                      .and(Predicates.gte("dateField", cal))
                                      .and(Predicates.lt("doubleField", Double.valueOf(3.14)))
                                      .and(Predicates.in("company", "c1", "c2"))
                                      .and(Predicates.notin("company", "c3", "c4"))
                                      .and(Predicates.isnull("username"))
                                      .and(Predicates.ilike("firstName", "abc%")); // make ILIKE work as LIKE
            LDAPFilterBuilder builder = new LDAPFilterBuilder(getDirectory());
            builder.walk(queryBuilder.predicate());
            assertEquals("(&" //
                    + "(!(|(uid={0})(uid={1})))" //
                    + "(intField>{2})" //
                    + "(!(booleanField={3}))" //
                    + "(&(intField>={4})(intField<={5}))" //
                    + "(givenName={6}*{7})" //
                    + "(!(givenName={8}))" //
                    + "(dateField>={9})" //
                    + "(doubleField<{10})" //
                    + "(|(o={11})(o={12}))" //
                    + "(!(|(o={13})(o={14})))" //
                    + "(!(uid=*))" //
                    + "(givenName={15}*)" //
                    + ")", //
                    builder.filter.toString());
            assertEqualsNormalized(Arrays.asList( //
                    "user_1", "Administrator", //
                    Long.valueOf(123), //
                    Boolean.TRUE, //
                    Long.valueOf(123), Long.valueOf(456), //
                    "jo", "n", //
                    "bob", //
                    cal, //
                    Double.valueOf(3.14), //
                    "c1", "c2", //
                    "c3", "c4", //
                    "abc" //
            ), builder.params.stream().collect(toList()));
        }
    }

    protected void assertEqualsNormalized(List<Object> a, List<Object> b) {
        assertEquals(normalizeList(a), normalizeList(b));
    }

    protected List<Object> normalizeList(List<Object> list) {
        return list.stream().map(this::normalizeCalendar).collect(Collectors.toList());
    }

    protected Object normalizeCalendar(Object ob) {
        if (ob instanceof Calendar) {
            ob = Long.valueOf(((Calendar) ob).getTimeInMillis());
        }
        return ob;
    }

    @Test
    public void testQueryBuilderEmpty() throws Exception {
        try (Session session = getSession()) {
            QueryBuilder queryBuilder = new QueryBuilder();
            LDAPFilterBuilder builder = new LDAPFilterBuilder(getDirectory());
            builder.walk(queryBuilder.predicate());
            assertEquals("", builder.filter.toString());
            assertEquals(Collections.emptyList(), builder.params.stream().collect(toList()));
        }
    }

    // more test for LIKE pattern translations
    @Test
    public void testQueryBuilderLike() throws Exception {
        try (Session session = getSession()) {
            QueryBuilder queryBuilder = //
                    new QueryBuilder().predicate(Predicates.like("firstName", "foobar"))
                                      .and(Predicates.like("firstName", "foo%bar"))
                                      .and(Predicates.like("firstName", "foo_bar"))
                                      .and(Predicates.like("firstName", "foo\\%bar"))
                                      .and(Predicates.like("firstName", "foo\\_bar"))
                                      .and(Predicates.like("firstName", "foo\\\\bar"));
            LDAPFilterBuilder builder = new LDAPFilterBuilder(getDirectory());
            builder.walk(queryBuilder.predicate());
            assertEquals("(&" //
                    + "(givenName={0})" //
                    + "(givenName={1}*{2})" //
                    + "(givenName={3})" //
                    + "(givenName={4})" //
                    + "(givenName={5})" //
                    + "(givenName={6})" //
                    + ")", //
                    builder.filter.toString());
            assertEqualsNormalized(Arrays.asList( //
                    "foobar", //
                    "foo", "bar", //
                    "foo_bar", // bare underscore is not interpreted as a wildcard
                    "foo%bar",
                    "foo_bar",
                    "foo\\bar"
            ), builder.params.stream().collect(toList()));
        }
    }

}
