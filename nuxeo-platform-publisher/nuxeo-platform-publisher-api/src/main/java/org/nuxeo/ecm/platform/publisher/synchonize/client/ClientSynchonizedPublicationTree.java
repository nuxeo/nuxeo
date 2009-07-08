package org.nuxeo.ecm.platform.publisher.synchonize.client;

import java.util.Calendar;

import org.nuxeo.ecm.platform.publisher.api.PublicationTree;

public interface ClientSynchonizedPublicationTree extends PublicationTree {

    Calendar getLastSynchronizationDate();

    boolean synchronize();

}
