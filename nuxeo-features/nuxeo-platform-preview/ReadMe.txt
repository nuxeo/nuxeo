
About this addon :
------------------
This addons provides HTML preview for Nuxeo Document.
The preview is available :
 - as a Tab in the default JSF WebApp
 - as a DocumentModel adpater in the API

The system is pluggable to that each type of DocumentModel may have it's own type of preview.
Default implementation includes 2 types of adapter :
 - Tranformation based : calls transformation service to generate HTML preview
 - Preprocessing based : use specific fields in the document that are supposed to store a preprocessed HTML files

Installation :
--------------
Copy the jar to the plugins directory.
You should also have :
 - OpenOffice lauched in Listen mode
 - htmltopdf command line utility installed in /usr/bin

Todo :
------
 - better error handling : OK
 - temp file better clean up : OK
 - make cmd line transformers configurable : OK
 - include more html transformers
