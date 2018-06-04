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

import org.apache.avro.reflect.AvroDefault;
import org.apache.avro.reflect.AvroIgnore;
import org.apache.avro.reflect.Nullable;

/**
 * @since 10.2
 */
@SuppressWarnings("CanBeFinal")
public class MessageV2 extends MessageV1 {

    // the new field must be annotated as Nullable or AvroDefault
    // note that avro default can be different than java
    @AvroDefault("\"unknown\"")
    public String newString = "new";

    @Nullable
    public String anotherNewString = "foo";

    @AvroIgnore
    public String doNotCare = "bar";
}
