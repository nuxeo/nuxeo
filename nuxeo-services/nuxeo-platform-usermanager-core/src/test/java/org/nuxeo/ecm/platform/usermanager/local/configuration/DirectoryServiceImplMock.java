package org.nuxeo.ecm.platform.usermanager.local.configuration;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfiguration;

public class DirectoryServiceImplMock extends DirectoryServiceImpl {

	private static final long serialVersionUID = 1L;
	
	@Override
	protected DirectoryConfiguration getDirectoryConfiguration(
			DocumentModel context) {
		
		if (context == null) {
			return null;
		}
		DirectoryConfiguration conf = new DirectoryConfiguration() {
			
			@Override
			public void save(CoreSession session) throws ClientException {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public DirectoryConfiguration merge(DirectoryConfiguration conf) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public DocumentRef getDocumentRef() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean canMerge() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public String getDirectorySuffix() {
				return "tenanta";
			}
		};
		
		return conf;
	}

}
