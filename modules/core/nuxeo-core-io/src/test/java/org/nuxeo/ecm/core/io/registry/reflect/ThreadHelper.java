/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io.registry.reflect;

import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableObject;
import org.nuxeo.ecm.core.io.registry.Marshaller;

/**
 * @since 2025.10
 */
class ThreadHelper {

    private ThreadHelper() {
        // helper class
    }

    public static <M extends Marshaller<Object>> M getFromAnotherThread(Supplier<M> supplier) throws Exception {
        var result = new MutableObject<M>();
        var err = new MutableObject<RuntimeException>();
        Thread subThread = new Thread(() -> {
            try {
                result.setValue(supplier.get());
            } catch (RuntimeException e) {
                err.setValue(e);
            }
        });
        subThread.start();
        subThread.join();
        if (err.get() != null) {
            throw err.get();
        }
        return result.get();
    }
}
