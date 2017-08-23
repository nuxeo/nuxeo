/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.common.exception;

import java.nio.channels.ClosedByInterruptException;

/**
 * DON'T USE THIS CLASS - INTERNAL API.
 * <p />
 * This helper class is used to detect if an exception is caused by an {@link InterruptedExceptions} or something
 * equivalent (for example {@link ClosedByInterruptException}. This is a temporary class, we should rely on the
 * {@link Thread#isInterrupted()} status in the future.
 *
 * @since 9.3
 */
public class InterruptedExceptions {

    public static boolean hasInterruptedCause(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (isInterrupted(t)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    public static boolean isInterrupted(Throwable t) {
        return t instanceof InterruptedException || t instanceof ClosedByInterruptException;
    }

}
