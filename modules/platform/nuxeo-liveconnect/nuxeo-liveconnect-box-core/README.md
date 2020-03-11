# Nuxeo Live Connect - Box

Nuxeo Live Connect connector for Box.

# Setting up OAuth with Box

## Step 1: Preparing your application accounts on the Box App console

 1. Go to https://app.box.com/developers/services
 2. Click on 'Create a Box Application'.
 3. Fill the name of your application and select Box Content.
 4. Click on 'Configure your application'.
 5. In 'redirect_uri' set the following URL, adapting the hostname and port to your case: https://localhost:8080/nuxeo/site/oauth2/box/callback
 6. Copy:
    - client_id
    - client_secret
 7. Save Application

## Step 2: Configuring the Nuxeo Platform
 1. In the Nuxeo Platform go to the Admin Center > Cloud Services.
 2. In the 'Service providers' tab, edit the 'box' service provider.
    - Paste there your client id and secret.
    - Make sure the 'Enabled' box is checked.
 3. Now go to the HOME tab and click 'Cloud Services' tab and click the 'Connect to Box' button or just create a new Box document.
