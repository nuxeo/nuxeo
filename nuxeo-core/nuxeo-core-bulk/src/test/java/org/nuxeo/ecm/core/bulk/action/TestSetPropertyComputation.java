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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk.action;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_VERSIONING_OPTION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.SetPropertyComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkCommand.Builder;

/**
 * @since 10.3
 */
public class TestSetPropertyComputation {

    private static class TestableSetPropertyComputation extends SetPropertyComputation {
        private TestableSetPropertyComputation(BulkCommand command) {
            this.command = command;
        }
    }

    @Test
    public void auditAndVersioningFieldsShouldHaveDefaultValue() {
        Collection<Serializable> values = new ArrayList<>();
        values.add(null);
        values.add("");
        values.add("tutu");
        values.add("false");
        values.add("[false]");
        values.add("none");
        values.add(VersioningOption.MAJOR.toString());
        values.add(VersioningOption.MINOR.toString());
        values.add(Boolean.FALSE);
        values.add(new String[] { "true" });
        values.add(new ArrayList<>(singleton("tutu")));
        values.add(new ArrayList<>(singleton("true")));
        testParamParsing(PARAM_DISABLE_AUDIT, values, false, false);
        testParamParsing(PARAM_VERSIONING_OPTION, values, false, false);
    }

    @Test
    public void auditFieldShouldBeTrue() {
        Collection<Serializable> values = new ArrayList<>();
        values.add("true");
        values.add(Boolean.TRUE);
        testParamParsing(PARAM_DISABLE_AUDIT, values, true, false);
    }

    @Test
    public void versioningFieldShouldBeNone() {
        Collection<Serializable> values = new ArrayList<>();
        values.add("NONE");
        values.add(VersioningOption.NONE.toString());
        testParamParsing(PARAM_VERSIONING_OPTION, values, false, true);
    }

    protected void testParamParsing(String param, Collection<Serializable> values, boolean audit, boolean versioning) {
        for (Serializable value : values) {
            BulkCommand command = new Builder(ACTION_NAME, "useless").param(param, value).build();
            SetPropertyComputation computation = new TestableSetPropertyComputation(command);
            computation.startBucket(null);
            assertEquals(audit, computation.disableAudit);
            assertEquals(versioning, computation.disableVersioning);
        }
    }

}
