package org.nuxeo.opensocial.container.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
/**
* @author Guillaume Cusnieux
*/
public class NuxeoGadgetFilter implements Filter {

	private static final long serialVersionUID = 1L;
	private static final Object PICTBOOK = "Picturebook";
	private static final Object GED = "Ged";
	private static final Object AGENDA = "Agenda";

	public boolean accept(DocumentModel doc) {

		return GED.equals(doc.getType()) || PICTBOOK.equals(doc.getType())
				|| AGENDA.equals(doc.getType());
	}

}
