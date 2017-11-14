/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *     Vladimir Pasquier
 */

package org.nuxeo.ecm.automation.core.trace;

import static org.nuxeo.ecm.automation.core.Constants.LF;

import java.io.BufferedWriter;
import java.io.IOException;

import org.nuxeo.ecm.automation.OperationType;

/**
 * @since 9.3
 */
public class LiteCall extends Call {

    public LiteCall(OperationType chain, OperationType op) {
        super(chain, op);
    }

    @Override
    public void print(BufferedWriter writer) throws IOException {
        writer.append(getType().getType().getName());
        writer.append(LF);
    }
}
