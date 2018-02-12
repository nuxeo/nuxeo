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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management.tests;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManagementStorageService;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.login.deputy.management")
@Deploy("org.nuxeo.ecm.platform.login.deputy.management:datasource-contrib.xml")
public class TestCanPersistDeputyMandates {

    @Inject
    DeputyManager dm;

    @Before
    public void initStorage() throws Exception {
        ((DeputyManagementStorageService) dm).resetDeputies();
    }

    @Test
    public void testAddDeputies() throws Exception {
        // titi has 2 deputies
        dm.addMandate(dm.newMandate("titi", "toto"));
        dm.addMandate(dm.newMandate("titi", "tata"));

        // titi is deputy for 3 users
        dm.addMandate(dm.newMandate("adm", "titi"));
        dm.addMandate(dm.newMandate("adm2", "titi"));
        dm.addMandate(dm.newMandate("adm3", "titi"));

        List<String> deputies = dm.getAvalaibleDeputyIds("titi");

        assertThat(deputies, notNullValue());
        assertThat(deputies, hasItems("toto", "tata"));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, notNullValue());
        assertThat(alternate, hasItems("adm", "adm2", "adm3"));
    }

    @Test
    public void testValidity() throws Exception {
        initStorage();

        Calendar notStarted = new GregorianCalendar();
        notStarted.add(Calendar.DAY_OF_MONTH, 1);
        Calendar started = new GregorianCalendar();
        started.add(Calendar.DAY_OF_MONTH, -2);

        Calendar notEnded = new GregorianCalendar();
        notEnded.add(Calendar.DAY_OF_MONTH, 2);
        Calendar ended = new GregorianCalendar();
        notEnded.add(Calendar.DAY_OF_MONTH, -1);

        // titi is deputy for 3 users
        dm.addMandate(dm.newMandate("adm", "titi", started, notEnded));
        dm.addMandate(dm.newMandate("adm2", "titi", started, ended));
        dm.addMandate(dm.newMandate("adm3", "titi", notStarted, notEnded));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, notNullValue());
        assertThat(alternate, hasItems("adm"));

    }

    @Test
    public void testDuplicate() throws Exception {
        initStorage();

        dm.addMandate(dm.newMandate("adm", "titi"));
        dm.addMandate(dm.newMandate("adm", "titi"));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, notNullValue());
        assertThat(alternate, hasItems("adm"));
    }

    @Test
    public void testRollback() throws Exception {
        initStorage();

        dm.addMandate(dm.newMandate("adm", "titi"));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, hasItems("adm")); // deputy is stored

        TransactionHelper.lookupUserTransaction().rollback();

        List<String> alternateAfterRollback = dm.getAvalaibleDeputyIds("titi");
        assertThat(alternateAfterRollback, not(hasItems("adm"))); // deputy is not stored

        // needed for session cleanup
        TransactionHelper.startTransaction();
    }

}
