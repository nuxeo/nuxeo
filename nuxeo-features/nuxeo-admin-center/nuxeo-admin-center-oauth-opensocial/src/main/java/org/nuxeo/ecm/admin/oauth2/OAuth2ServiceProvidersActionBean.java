package org.nuxeo.ecm.admin.oauth2;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistryImpl;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.oauth2.WEOAuthConstants;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

@Name("oauth2ServiceProvidersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ServiceProvidersActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = OAuth2ServiceProviderRegistryImpl.DIRECTORY_NAME;

    protected static final String SCHEMA = NuxeoOAuth2ServiceProvider.SCHEMA;

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

    public String getAuthorizationURL(String entryId) throws Exception {

        String url;

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            DocumentModel entry = session.getEntry(entryId);
            NuxeoOAuth2ServiceProvider serviceProvider = NuxeoOAuth2ServiceProvider.createFromDirectoryEntry(entry);

            HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

            JsonFactory JSON_FACTORY = new JacksonFactory();

            AuthorizationCodeFlow flow = serviceProvider.getAuthorizationCodeFlow(
                    HTTP_TRANSPORT, JSON_FACTORY);

            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            String redirectUrl = VirtualHostHelper.getServerURL(request)
                    + WEOAuthConstants.getInstalledAppCallbackURL(serviceProvider.getServiceName());

            // redirect to the authorization flow
            AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
            authorizationUrl.setRedirectUri(redirectUrl);

            url = authorizationUrl.build();

        } finally {
            session.close();
        }

        return url;
    }
}
