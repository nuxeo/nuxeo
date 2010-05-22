The request API is very simple - this is due to limitations on some platform (like GWT)
It only lets you set headers and content as a string.
To upload or download files you need to provide specific API depending on the platform.

Through Resource class you can refer to already uploaded files (through the specific api)
Resource means an URL -> either an uploaded file either a file located by the URL. Uploaded files
are specified using upload:/ URI.

