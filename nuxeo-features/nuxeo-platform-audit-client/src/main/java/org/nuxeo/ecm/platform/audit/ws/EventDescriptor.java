package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;
import java.util.Date;

public class EventDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 987698679871L;

    private String eventId;
    private String eventDate;
    private String docPath;
    private String docUUID;

    public EventDescriptor(String eventId,Date eventDate, String docPath, String docUUID)
    {
      this.eventDate=eventDate.toString();
      this.eventId=eventId;
      this.docPath=docPath;
      this.docUUID=docUUID;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getDocPath() {
        return docPath;
    }

    public String getDocUUID() {
        return docUUID;
    }


}
