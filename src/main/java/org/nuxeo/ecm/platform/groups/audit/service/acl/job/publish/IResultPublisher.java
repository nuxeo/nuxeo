package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import org.nuxeo.ecm.core.api.ClientException;

public interface IResultPublisher {
    public void publish() throws ClientException;
}
