/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.stream.tests;

import java.util.Objects;

/**
 * @since 11.4
 */
class TestMessage {
    protected String stringF1;

    protected Long longF2;

    protected boolean boolF3;

    protected TestMessage() {
        // Empty constructor required for deserialization
    }

    public TestMessage(String stringF1, Long longF2, boolean boolF3) {
        this.stringF1 = stringF1;
        this.longF2 = longF2;
        this.boolF3 = boolF3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestMessage that = (TestMessage) o;
        return boolF3 == that.boolF3 && Objects.equals(stringF1, that.stringF1) && Objects.equals(longF2, that.longF2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringF1, longF2, boolF3);
    }

    @Override
    public String toString() {
        return "TestMessage{" + "stringF1='" + stringF1 + '\'' + ", longF2=" + longF2 + ", boolF3=" + boolF3 + '}';
    }
}
