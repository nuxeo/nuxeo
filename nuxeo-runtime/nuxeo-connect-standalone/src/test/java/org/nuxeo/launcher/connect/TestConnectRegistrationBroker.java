/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mincong HUANG
 *
 */
package org.nuxeo.launcher.connect;

import org.junit.Test;
import org.nuxeo.launcher.connect.ConnectRegistrationBroker.TrialField;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestConnectRegistrationBroker {

    @Test
    public void validFirstName() throws Exception {
        List<String> firstNames = Arrays.asList("One-two Three", "First Name", "Prenom");
        Predicate<String> validator = TrialField.FIRST_NAME.getPredicate();
        firstNames.forEach(v -> assertTrue("First name '" + v + "' should be valid.", validator.test(v)));
    }

    @Test
    public void invalidFirstName() throws Exception {
        List<String> firstNames = Arrays.asList("", "toto@my.corp", "First_Name", "Prénom", "中文");
        Predicate<String> validator = TrialField.FIRST_NAME.getPredicate();
        firstNames.forEach(v -> assertFalse("First name '" + v + "' should be invalid.", validator.test(v)));
    }

    @Test
    public void validLastName() throws Exception {
        List<String> lastNames = Arrays.asList("One-two Three", "LastName", "Nom tres long");
        Predicate<String> validator = TrialField.LAST_NAME.getPredicate();
        lastNames.forEach(v -> assertTrue("Last name '" + v + "' should be valid.", validator.test(v)));
    }

    @Test
    public void invalidLastName() throws Exception {
        List<String> firstNames = Arrays.asList("", "toto@my.corp", "Last_Name", "Nom très long", "中文");
        Predicate<String> validator = TrialField.FIRST_NAME.getPredicate();
        firstNames.forEach(v -> assertFalse("Last name '" + v + "' should be invalid.", validator.test(v)));
    }

    @Test
    public void validCompanyName() throws Exception {
        List<String> companyNames = Arrays.asList("MyCorp", "My Corp", "My corp", "my-corp", "my corp 01");
        Predicate<String> validator = TrialField.COMPANY.getPredicate();
        companyNames.forEach(v -> assertTrue("Company name '" + v + "' should be valid.", validator.test(v)));
    }

    @Test
    public void invalidCompanyName() throws Exception {
        List<String> companyNames = Arrays.asList("", "My_Corp", "Ma Société");
        Predicate<String> validator = TrialField.COMPANY.getPredicate();
        companyNames.forEach(v -> assertFalse("Company name '" + v + "' should be invalid.", validator.test(v)));
    }

}
