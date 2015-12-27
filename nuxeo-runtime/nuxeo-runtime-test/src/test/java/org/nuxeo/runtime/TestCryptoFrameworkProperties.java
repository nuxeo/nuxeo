/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     jcarsique
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import org.nuxeo.common.Environment;

/**
 * @since 7.4
 */
public class TestCryptoFrameworkProperties extends TestFrameworkProperties {
    // aValue == "myValue"
    final String aCryptedValue = "{$$CTT3p7DKBpCgkmPDSkUFPg==}";

    // aValue == "myValue"
    final String aDESCryptedValue = "{$REVT$g14b3eWAgSU=}";

    // aDefaultValue == "myDefaultValue"
    final String aCryptedDefaultValue = "{$$WPdr+gWTfXknlZS53VhyJA==}";

    // aStrangeValue == "${\\my.strange/value}"
    final String aCryptedStrangeValue = "{$$mdKe++UN0LUHnw4DrHor952CmGYm+ZzGBM2oi0hLO9E=}";

    // aSystemValue == "mySystemValue"
    final String aCryptedSystemValue = "{$$L7kzxIJxOx3T6LWqRkxjOg==}";

    @Before
    public void setUp() throws Exception {
        runtime.setProperty(Environment.CRYPT_KEY, Base64.encodeBase64String("secret".getBytes()));
    }

    @Override
    @Test
    public void testExpandVars() {
        // RuntimeService.expandVars(String) does not decrypt raw content, only variable values
        assertEquals("<myProp>" + aCryptedValue + "</myProp>",
                runtime.expandVars("<myProp>" + aCryptedValue + "</myProp>"));

        List<String> testExpressions = new ArrayList<>();
        testExpressions.add("<myProp>" + aValue + "</myProp>");
        testExpressions.add("<myProp>${" + aParam + "}</myProp>");
        testExpressions.add("<myProp>${" + aParam + ":=" + aDefaultValue + "}</myProp>");
        testExpressions.add("<myProp>${" + aParam + ":=" + aCryptedDefaultValue + "}</myProp>");

        // property undefined
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>${" + aParam + "}</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aDefaultValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
        assertEquals("<myProp>" + aDefaultValue + "</myProp>", runtime.expandVars(testExpressions.get(3)));

        // system property
        System.setProperty(aParam, aCryptedSystemValue);
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(3)));

        // runtime property
        runtime.setProperty(aParam, aCryptedValue);
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(3)));

        runtime.setProperty(aParam, aDESCryptedValue);
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(3)));
    }

    @Override
    @Test
    public void testExpandVarsWithSpecialChars() {
        List<String> testExpressions = new ArrayList<>();
        testExpressions.add("<myProp>" + aStrangeValue + "</myProp>");
        testExpressions.add("<myProp>${" + aParamWithDot + "}</myProp>");
        testExpressions.add("<myProp>${" + aParamWithDot + ":=" + aDefaultValue + "}</myProp>");

        // property undefined
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>${" + aParamWithDot + "}</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aDefaultValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // system property
        System.setProperty(aParamWithDot, aCryptedSystemValue);
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // runtime property
        runtime.setProperty(aParamWithDot, aCryptedStrangeValue);
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
    }

}
