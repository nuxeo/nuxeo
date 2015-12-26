/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io.extension;

import org.joda.time.DateTime;

import com.github.segmentio.models.BasePayload;
import com.github.segmentio.models.Context;
import com.github.segmentio.models.Traits;

public class Group extends BasePayload {

    @SuppressWarnings("unused")
    private String action = "group";

    private String groupId;

    private Traits traits;

    public Group(String userId, String groupId, Traits traits, DateTime timestamp,
            Context context) {
        super(userId, timestamp, context, null);
        this.groupId=groupId;
        this.traits = traits;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Traits getTraits() {
        return traits;
    }

    public void setTraits(Traits traits) {
        this.traits = traits;
    }

}
