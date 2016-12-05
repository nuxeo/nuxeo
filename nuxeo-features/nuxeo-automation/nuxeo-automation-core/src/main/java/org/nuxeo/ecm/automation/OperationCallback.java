/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */

package org.nuxeo.ecm.automation;

import java.util.Map;

import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * @since 5.7.3
 */
public interface OperationCallback {

    void onChainEnter(OperationType chain);

    void onChainExit();

    void onOperationEnter(OperationContext context, OperationType type, InvokableMethod method, Map<String, Object> parms);

    void onOperationExit(Object output);

    OperationException onError(OperationException error);

}
