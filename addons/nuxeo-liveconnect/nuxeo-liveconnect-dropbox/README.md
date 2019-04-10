# Nuxeo Live Connect - Dropbox

Nuxeo Live Connect connector for Dropbox.

# Setting OAuth with Dropbox

 1. Go to https://www.dropbox.com/developers/apps
 2. Click on 'App console'
 3. Select 'Dropbox API app'
    Select:
    - Can your app be limited to its own folder? -> No My app needs access to files already on Dropbox.
    - What type of files does your app need access to? -> All file types My app needs access to a user's full Dropbox.
    - Fill the name with 'XXX'.
    - Click 'Create app'
 4. From the 'XXX' App settings tab, copy:
    - App key
    - App secret
 
    and go to the Admin Center/OAuth of your running nuxeo server to edit the 'dropbox' service provider.
    - Paste there your key and secret.
    - Enable the provider

Now go to the HOME tab and click OAuth Menu and click the 'Add a new dropbox token' button. 
