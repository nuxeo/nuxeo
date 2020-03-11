/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestJson {

    @Test
    public void canSerializeAndReadBack() throws IOException {
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            DistributionSnapshot.jsonWriter().writeValue(sink, RuntimeSnapshot.build());
            try (OutputStream file =
                    Files.newOutputStream(Paths.get("target/test.json"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                file.write(sink.toByteArray());
            }
            try (ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray())) {
                DistributionSnapshot snapshot = DistributionSnapshot.jsonReader().<DistributionSnapshot>readValue(source);
                Assertions.assertThat(snapshot).isNotNull();
                Assertions.assertThat(snapshot.getBundle("org.nuxeo.apidoc.repo")).isNotNull();
            }
        }
    }

}
