/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.work;

import java.util.UUID;

/**
 * @since 11.3
 */
public class SlowWork extends AbstractWork {
    private static final long serialVersionUID = 2L;

    protected final long durationMs;

    public SlowWork(long durationMs) {
        super(UUID.randomUUID().toString());
        this.durationMs = durationMs;
    }

    @Override
    public void work() {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTitle() {
        return "SlowWork " + durationMs + " ms";
    }
}
