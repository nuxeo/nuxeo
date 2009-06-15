package org.nuxeo.ecm.platform.ui.web.seamremoting;

import java.util.List;

public interface SeamRemotingJSBuilderService {

	public List<String> getRemotableBeanNames();

	public String getSeamRemotingJavaScriptURLParameters();

}