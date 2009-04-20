package org.nuxeo.ecm.core.event.impl;

import java.util.Comparator;

public class EventListenerDescriptorComparator  implements Comparator<EventListenerDescriptor> {

	public int compare(EventListenerDescriptor o1, EventListenerDescriptor o2) {
		return o1.getPriority() - o2.getPriority();
	}

}
