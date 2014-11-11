Sample Oracle Setup (Thin client)
=================================
You need ::
 - the appropriate thin client driver for your version of Oracle database, 
   which you can downlod from Oracle's web pages. Put in in the lib/ 
   subdirectory if you intend to use the ant deployment script
 - a valid user that can create tables, sequences and views

First, edit the connection parameters in batch::

 $ python dbsetup.py --url jdbc:oracle:thin:@localhost:1521:SID
                     --user username --password myPr3c1ous

(In Oracle XE there is but one SID value: XE)

Note that you need lxml (python-lxml under debian/ubuntu) installed for this
script to work. See also http://codespeak.net/lxml/

Second, use "ant copy-oracle" in the top level build.xml file of a 
Nuxeo checkout for the right version to deploy that configuration 
to your instance.
