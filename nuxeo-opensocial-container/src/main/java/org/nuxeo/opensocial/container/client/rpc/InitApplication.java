package org.nuxeo.opensocial.container.client.rpc;

/**
 * @author Stéphane Fourrier
 */
public class InitApplication extends AbstractAction<InitApplicationResult> {

    private static final long serialVersionUID = 1L;

    private String spaceProviderName;

    private String spaceName;

    @SuppressWarnings("unused")
    private InitApplication() {
        super();
    }

    public InitApplication(ContainerContext containerContext,
            String spaceProviderName, String spaceName) {
        super(containerContext);
        this.spaceProviderName = spaceProviderName;
        this.spaceName = spaceName;
    }

    public String getSpaceProviderName() {
        return spaceProviderName;
    }

    public String getSpaceName() {
        return spaceName;
    }

}
