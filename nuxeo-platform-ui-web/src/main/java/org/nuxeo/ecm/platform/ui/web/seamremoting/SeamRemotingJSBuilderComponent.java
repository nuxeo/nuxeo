package org.nuxeo.ecm.platform.ui.web.seamremoting;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

@Name("SeamRemotingJSBuilderService")
@Scope(ScopeType.STATELESS)
public class SeamRemotingJSBuilderComponent extends DefaultComponent implements SeamRemotingJSBuilderService {

    public static String EP_REMOTABLE_SEAMBEANS = "remotableSeamBeans";

    protected static List<String> beanNames = new ArrayList<String>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_REMOTABLE_SEAMBEANS)) {
            RemotableSeamBeansDescriptor descriptor = (RemotableSeamBeansDescriptor) contribution;
            beanNames.addAll(descriptor.getBeanNames());
        }
    }

    public List<String> getRemotableBeanNames() {
        return beanNames;
    }

    @Factory(value="SeamRemotingBeanNames", scope=ScopeType.APPLICATION)
    public String getSeamRemotingJavaScriptURLParameters() {

        StringBuffer sb = new StringBuffer();

        int idx = 0;
        for (String beanName : beanNames) {
            sb.append(beanName);
            idx++;
            if (idx < beanNames.size()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

}
