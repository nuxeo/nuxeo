package org.nuxeo.ecm.platform.publisher.synchonize.client;

import org.nuxeo.ecm.platform.publisher.api.PublicationTree;

import java.util.Calendar;

public interface ClientSynchonizedPublicationTree extends PublicationTree {

    Calendar getLastSynchronizationDate();

    boolean synchronize();

}
