/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.error.web;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * JSF functions triggering errors
 *
 * @author Anahide Tchertchian
 */
public class JSFErrorFunctions {

    public static String triggerCheckedError() {
        throw new NuxeoException("JSF function triggering a checked error");
    }

    public static String triggerUncheckedError() {
        throw new NullPointerException("JSF function triggering an unchecked error");
    }

}
