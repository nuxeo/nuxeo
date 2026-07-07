/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.IgnoreNonPostgreSQL;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;
import org.nuxeo.ecm.directory.multi.MultiDirectoryDescriptor;
import org.nuxeo.runtime.mongodb.IgnoreNoMongoDB;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * Tests that {@link UserManager#getUserSearchPredicate(String)} and {@link UserManager#getGroupSearchPredicate(String)}
 * correctly resolve the effective {@link SubstringMatchType} when the user or group directory is a
 * {@link MultiDirectoryDescriptor multi-directory}.
 *
 * @since 2025.22
 */
@Features(LogCaptureFeature.class)
@Deploy("org.nuxeo.ecm.directory.multi")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-config.xml")
public class TestUserManagerMultiDirectory extends UserManagerTestCase {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected HotDeployer hotDeployer;

    /**
     * No {@code substringMatchType} declared anywhere: multi-directory default ({@code subinitial}) is used.
     */
    @Test
    public void testMultiDirectoryNoExplicit() {
        assertSubinitialUserPredicate();
        assertSubinitialGroupPredicate();
    }

    /**
     * {@code substringMatchType} is declared on the multi-directory itself (and, conflictingly, on a sub-directory):
     * the multi-level value wins (SQL variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNonPostgreSQL.class, cause = "SQL-only test variant")
    public void testMultiDirectoryMultiLevelWinsSQL() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-multi-level-sql.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * {@code substringMatchType} is declared on the multi-directory itself (and, conflictingly, on a sub-directory):
     * the multi-level value wins (MongoDB variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "MongoDB-only test variant")
    public void testMultiDirectoryMultiLevelWinsMongoDB() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-multi-level-mongodb.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * {@code substringMatchType=subany} is declared on only one of the two sub-directories: that value is picked up
     * (SQL variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNonPostgreSQL.class, cause = "SQL-only test variant")
    public void testMultiDirectorySingleSubExplicitSQL() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-single-sub-sql.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * {@code substringMatchType=subany} is declared on only one of the two sub-directories: that value is picked up
     * (MongoDB variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "MongoDB-only test variant")
    public void testMultiDirectorySingleSubExplicitMongoDB() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-single-sub-mongodb.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * The same {@code substringMatchType=subany} is declared on both sub-directories: that shared value is picked up
     * (SQL variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNonPostgreSQL.class, cause = "SQL-only test variant")
    public void testMultiDirectoryAgreeingSubsSQL() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-agreeing-subs-sql.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * The same {@code substringMatchType=subany} is declared on both sub-directories: that shared value is picked up
     * (MongoDB variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "MongoDB-only test variant")
    public void testMultiDirectoryAgreeingSubsMongoDB() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-agreeing-subs-mongodb.xml");
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
    }

    /**
     * The sub-directories declare conflicting values {@code subany} and {@code subfinal}: the most permissive value
     * ({@code subany}) wins, and a warning is logged (SQL variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNonPostgreSQL.class, cause = "SQL-only test variant")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = UserManagerImpl.class)
    public void testMultiDirectoryConflictingSubsSubanyWinsSQL() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-conflicting-subs-sql.xml");
        assertConflictingSubsSubanyWinsResults();
    }

    /**
     * The sub-directories declare conflicting values {@code subany} and {@code subfinal}: the most permissive value
     * ({@code subany}) wins, and a warning is logged (MongoDB variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "MongoDB-only test variant")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = UserManagerImpl.class)
    public void testMultiDirectoryConflictingSubsSubanyWinsMongoDB() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-conflicting-subs-mongodb.xml");
        assertConflictingSubsSubanyWinsResults();
    }

    /**
     * The sub-directories declare conflicting values {@code subinitial} and {@code subfinal} (no {@code subany}): the
     * most permissive value ({@code subfinal}) wins, and a warning is logged (SQL variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNonPostgreSQL.class, cause = "SQL-only test variant")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = UserManagerImpl.class)
    public void testMultiDirectoryConflictingSubsSubfinalWinsSQL() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-conflicting-subs-no-subany-sql.xml");
        assertConflictingSubsSubfinalWinsResults();
    }

    /**
     * The sub-directories declare conflicting values {@code subinitial} and {@code subfinal} (no {@code subany}): the
     * most permissive value ({@code subfinal}) wins, and a warning is logged (MongoDB variant).
     */
    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "MongoDB-only test variant")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = UserManagerImpl.class)
    public void testMultiDirectoryConflictingSubsSubfinalWinsMongoDB() throws Exception {
        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-multi-directory-substring-match-conflicting-subs-no-subany-mongodb.xml");
        assertConflictingSubsSubfinalWinsResults();
    }

    protected void assertConflictingSubsSubanyWinsResults() throws Exception {
        assertSubanyUserPredicate();
        assertSubanyGroupPredicate();
        List<String> messages = logCaptureResult.getCaughtEventMessages();
        // one warning per predicate build (getUserSearchPredicate + getGroupSearchPredicate)
        assertEquals(messages.toString(), 2, messages.size());
        assertTrue(messages.get(0),
                messages.get(0).contains("Multi-directory: 'multiUserDirectory' has conflicting substringMatchType"));
        assertTrue(messages.get(0), messages.get(0).contains("Using: 'subany' (the most permissive value)"));
        assertTrue(messages.get(1),
                messages.get(1).contains("Multi-directory: 'multiGroupDirectory' has conflicting substringMatchType"));
        assertTrue(messages.get(1), messages.get(1).contains("Using: 'subany' (the most permissive value)"));
    }

    protected void assertConflictingSubsSubfinalWinsResults() throws Exception {
        assertSubfinalUserPredicate();
        assertSubfinalGroupPredicate();
        List<String> messages = logCaptureResult.getCaughtEventMessages();
        assertEquals(messages.toString(), 2, messages.size());
        assertTrue(messages.get(0), messages.get(0).contains("Using: 'subfinal' (the most permissive value)"));
        assertTrue(messages.get(1), messages.get(1).contains("Using: 'subfinal' (the most permissive value)"));
    }

    protected void assertSubinitialUserPredicate() {
        String predicate = userManager.getUserSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("firstName ILIKE 'foo%'"));
        assertTrue(predicate, predicate.contains("lastName ILIKE 'foo%'"));
        assertTrue(predicate, predicate.contains("username ILIKE 'foo%'"));
    }

    protected void assertSubinitialGroupPredicate() {
        String predicate = userManager.getGroupSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("groupname ILIKE 'foo%'"));
        assertTrue(predicate, predicate.contains("grouplabel ILIKE 'foo%'"));
    }

    protected void assertSubanyUserPredicate() {
        String predicate = userManager.getUserSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("firstName ILIKE '%foo%'"));
        assertTrue(predicate, predicate.contains("lastName ILIKE '%foo%'"));
        assertTrue(predicate, predicate.contains("username ILIKE '%foo%'"));
    }

    protected void assertSubanyGroupPredicate() {
        String predicate = userManager.getGroupSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("groupname ILIKE '%foo%'"));
        assertTrue(predicate, predicate.contains("grouplabel ILIKE '%foo%'"));
    }

    protected void assertSubfinalUserPredicate() {
        String predicate = userManager.getUserSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("firstName ILIKE '%foo'"));
        assertTrue(predicate, predicate.contains("lastName ILIKE '%foo'"));
        assertTrue(predicate, predicate.contains("username ILIKE '%foo'"));
    }

    protected void assertSubfinalGroupPredicate() {
        String predicate = userManager.getGroupSearchPredicate("foo").toString();
        assertTrue(predicate, predicate.contains("groupname ILIKE '%foo'"));
        assertTrue(predicate, predicate.contains("grouplabel ILIKE '%foo'"));
    }

}
