package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;

public interface IResultPublisher extends Serializable {

    public void publish(Blob fileBlob);

}
