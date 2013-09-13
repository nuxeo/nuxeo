AdulactImporter
===============

Document importer using XML as source and  Xpath  + MVEL to configure mappings 


## MVEL Context 

 - `currentDocument` : last created DocumentModel
 - `currentElement` : last parsed DOM4J tag Element
 - `xml` : XML input document as parsed by DOM4J
 - `map` : Mapping between DOM4J ELements and associated created DocumentModel (Element object is the key)
 - `root` : root DocumentModel where the import was started
 - `docs` : list of imported DocumentModels
 - `session` : CoreSession
 - `Fn` : utility functions
 - `source` : source file (File object) or source input stream (InputStream object) being parsed.




