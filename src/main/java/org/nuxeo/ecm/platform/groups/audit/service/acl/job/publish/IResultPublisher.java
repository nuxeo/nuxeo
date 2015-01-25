package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

public interface IResultPublisher extends Serializable {

    public void publish(Blob fileBlob) throws ClientException;

}
