package org.nuxeo.opensocial.container.server.handler.webcontent;

import java.util.Locale;
import java.util.Map;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.CreateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContentHandler extends
        AbstractActionHandler<CreateWebContent, CreateWebContentResult> {

    public static final String GENERATE_TITLE_PARAMETER_NAME = "generateTitle";

    protected CreateWebContentResult doExecute(CreateWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        String spaceId = action.getSpaceId();
        Space space = getSpaceFromId(spaceId, session);
        WebContentData data = action.getData();
        generateTitle(data, action.getParameters(), action.getUserLanguage());

        data = space.createWebContent(data);
        Map<String, Boolean> permissions = space.getPermissions(spaceId);
        return new CreateWebContentResult(data, permissions);
    }

    protected void generateTitle(WebContentData data,
            Map<String, String> parameters, String userLanguage) {
        String shouldGenerateTitle = parameters.get(GENERATE_TITLE_PARAMETER_NAME);
        if (!"false".equals(shouldGenerateTitle)) {
            String name = data instanceof OpenSocialData ? ((OpenSocialData) data).getGadgetName()
                    : data.getName();
            String labelKey = "label.gadget." + name;

            Locale locale = userLanguage != null ? new Locale(userLanguage)
                    : Locale.getDefault();
            String i18nTitle = I18NUtils.getMessageString("messages", labelKey,
                    null, locale);
            if (!i18nTitle.equals(labelKey)) {
                // we found a match
                data.setTitle(i18nTitle);
            } else {
                data.setTitle(name);
            }
        }
    }

    public Class<CreateWebContent> getActionType() {
        return CreateWebContent.class;
    }

}
