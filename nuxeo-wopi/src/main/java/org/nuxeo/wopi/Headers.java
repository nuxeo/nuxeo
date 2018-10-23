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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

/**
 * @since 10.3
 */
public class Headers {

    private Headers() {
        // constants class
    }

    public static final String ITEM_VERSION = "X-WOPI-ItemVersion";

    public static final String LOCK = "X-WOPI-Lock";

    public static final String MAX_EXPECTED_SIZE = "X-WOPI-MaxExpectedSize";

    public static final String OLD_LOCK = "X-WOPI-OldLock";

    public static final String OVERRIDE = "X-WOPI-Override";

    public static final String PROOF = "X-WOPI-Proof";

    public static final String PROOF_OLD = "X-WOPI-ProofOld";

    public static final String RELATIVE_TARGET = "X-WOPI-RelativeTarget";

    public static final String REQUESTED_NAME = "X-WOPI-RequestedName";

    public static final String SUGGESTED_TARGET = "X-WOPI-SuggestedTarget";

    public static final String URL_TYPE = "X-WOPI-UrlType";

    public static final String TIMESTAMP = "X-WOPI-TimeStamp";

}
