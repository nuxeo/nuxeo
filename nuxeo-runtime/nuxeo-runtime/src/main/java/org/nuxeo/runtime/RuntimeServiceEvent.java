/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

/**
 * An event in the Nuxeo Runtime life cycle.
 * <p>
 * The following event types are defined:
 * <ul>
 * <li> <code>RUNTIME_ABOUT_TO_START</code> Sent before starting the runtime
 * <li> <code>RUNTIME_STARTED</code> Sent after the runtime was started
 * <li> <code>RUNTIME_ABOUT_TO_STOP</code> Sent before stopping the runtime
 * <li> <code>RUNTIME_STOPPED</code> Sent after the runtime stopped
 * </ul>
 * Note: these events are not supposed to leave the runtime, hence are not declared serializable.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeServiceEvent {

    // the event IDs
    public static final int RUNTIME_ABOUT_TO_START = 0;

    public static final int RUNTIME_STARTED = 1;

    public static final int RUNTIME_ABOUT_TO_STOP = 2;

    public static final int RUNTIME_STOPPED = 3;

    /** The event id. */
    public final int id;

    public final RuntimeService runtime;

    public RuntimeServiceEvent(int id, RuntimeService runtime) {
        this.id = id;
        this.runtime = runtime;
    }

    /**
     * Gets the event name as a string.
     *
     * @return the event name
     */
    public final String getEventName() {
        switch (id) {
        case RUNTIME_STARTED:
            return "RUNTIME_STARTED";
        case RUNTIME_STOPPED:
            return "RUNTIME_STOPPED";
        case RUNTIME_ABOUT_TO_STOP:
            return "RUNTIME_ABOUT_TO_STOP";
        case RUNTIME_ABOUT_TO_START:
            return "RUNTIME_ABOUT_TO_START";
        }
        return "UNKNOWN";
    }

    @Override
    public String toString() {
        return getEventName();
    }

    // FIXME: review this, this looks suspicious (doesn't check on this.id).
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj.getClass().equals(this.getClass());
    }

}
