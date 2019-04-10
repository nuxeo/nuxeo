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
