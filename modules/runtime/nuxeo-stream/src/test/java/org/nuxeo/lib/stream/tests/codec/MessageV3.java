/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tests.codec;

import java.util.Objects;

import org.apache.avro.reflect.AvroName;

/**
 * @since 10.2
 */
@SuppressWarnings("CanBeFinal")
public class MessageV3 {
    @AvroName("intValue")
    public int myInt = 3;

    @AvroName("stringValue")
    public String myString = "v3";

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MessageV3 messageV3 = (MessageV3) o;
        return myInt == messageV3.myInt && Objects.equals(myString, messageV3.myString);
    }

    @Override
    public int hashCode() {

        return Objects.hash(myInt, myString);
    }
}
