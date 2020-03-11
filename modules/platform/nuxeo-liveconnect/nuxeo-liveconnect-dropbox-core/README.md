# Nuxeo Live Connect - Dropbox

Nuxeo Live Connect connector for Dropbox.

# Setting up OAuth with Dropbox

## Step 1:  Preparing your application accounts on the Dropbox App console

 1. Go to https://www.dropbox.com/developers/apps.
 2. Click on 'App console'.
 3. Click on the button 'Create app'.
 4. Select 'Dropbox API app' and then:
    Select:
    - 'Can your app be limited to its own folder?' -> No
    - 'What type of files does your app need access to?' -> All file types
    - Fill the name of your application.
    - Click 'Create app'.
 5. From the Settings tab, copy:
    - App key
    - App secret
 6. In the 'OAuth 2' > Redirect URIs set the following URL, adapting the hostname and port to your case: http://localhost:8080/nuxeo/site/oauth2/dropbox/callback.

## Step 2: Configuring the Nuxeo Platform
 1. In the Nuxeo Platform go to the Admin Center > Cloud Services.
 2. In the 'Service providers' tab, edit the 'dropbox' service provider.
    - Paste there your key and secret.
    - Make sure the 'Enabled' box is checked.
 3. Now go to the HOME tab and click 'Cloud Services' tab and click the 'Connect to Dropbox' button or just create a new Dropbox document.
