/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.login.deputy.management.tests;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManagementStorageService;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy( {
    "org.nuxeo.runtime.datasource",
	"org.nuxeo.ecm.directory",
	"org.nuxeo.ecm.directory.sql",
	"org.nuxeo.ecm.directory.types.contrib",
	"org.nuxeo.ecm.platform.login.deputy.management"
})
@LocalDeploy("org.nuxeo.ecm.platform.login.deputy.management:datasource-contrib.xml")
public class TestCanPersistDeputyMandates {

    @Inject DeputyManager dm;


    @Before public void initStorage() throws Exception {
        ((DeputyManagementStorageService) dm).resetDeputies();
    }

    @Test public void testAddDeputies() throws Exception {
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

    @Test public void testValidity() throws Exception {
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

    @Test public void testDuplicate() throws Exception {
        initStorage();

        dm.addMandate(dm.newMandate("adm", "titi"));
        dm.addMandate(dm.newMandate("adm", "titi"));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, notNullValue());
        assertThat(alternate, hasItems("adm"));
    }

    @Test public void testRollback() throws Exception {
        initStorage();

        dm.addMandate(dm.newMandate("adm", "titi"));

        List<String> alternate = dm.getPossiblesAlternateLogins("titi");
        assertThat(alternate, hasItems("adm")); // deputy is stored

        TransactionHelper.lookupUserTransaction().rollback();

        List<String> alternateAfterRollback = dm.getAvalaibleDeputyIds("titi");
        assertThat(alternateAfterRollback, not(hasItems("adm"))); // deputy is not stored
    }

}
