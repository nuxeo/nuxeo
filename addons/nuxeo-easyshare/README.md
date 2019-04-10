# Summary

The EasyShare package is an addon for the Nuxeo platform to enable sharing files
from the repository with external users not requiring a login. You can specify a
set of files to be accessed by a specific unique URL which allows anonymous
download of either files within the share or proxies from anywhere else in your
repository through a special folder listing page. You can also set an expiration
date for sharing availability and track all downloads and get notifications.

Note:
> The EasyShare plugin is not part of the supported list of marketplace
packages. It's been developed for internal use and has been made public for the
interest of the Nuxeo community. The reason why it is not supported as is is 
that it is highly probable that we add the "share via link" feature to the 
Collection module. Yet you can safely use it for your own sharing use cases!

# Features

- Create shareable EasyShare Folder which has it's own externally accessible permanent URL
- A special folder listing page for external users with comments for recipient and file lists
- Folder or individual files within folder can be perma-linked and sent directly to recipients
- Tracks external folder access and individual file download with IP in History tab logs
- Send a notification when files are downloaded to contact email for the share folder
- Can add files directly to share directory or link proxy documents from anywhere in CMS

# Usage
## Creating a share

1. Anywhere you create a folder you can create a new type called EasyShare Folder which will contain your shared files.
1. Name the share. This will be visible to your recipients in the EasyShare page
1. Share comment is for sending a message that your recipient will see on the EasyShare page
1. Expiration date determines until which day the share will be available. After this date, the share will still exist in Nuxeo but will show as expired when attempting external access.
1. Add files directly into the EasyShare folder as any other folder by creating a new document
1. Add proxies by filling your worklist with documents from from anywhere else in the system, navigate back to share and click on Add to EasyShare
1. Get the URL by clicking on the URL button within a share folder. This URL can be sent to anyone by copy and paste, using whatever method outside Nuxeo, email, IM, etc.

## Accessing a share

- The EasyShare URL accesses a particular EasyShare folder without having to login to Nuxeo.
- This can be bookmarked as it is permanent and files within this listing can be downloaded and also bookmarked.

# Future improvements

- Download of all files by a zip
- Sending by email from Nuxeo

# Links

Documentation: [USERDOC/Nuxeo+EasyShare](http://doc.nuxeo.com/x/rooZAQ)

Source code: [https://github.com/nuxeo/nuxeo-easyshare](https://github.com/nuxeo/nuxeo-easyshare)

[EasyShare Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/easyshare)
