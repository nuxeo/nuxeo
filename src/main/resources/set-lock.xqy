
(: the URI of the document to set lock :)
declare variable $uri as xs:string external;

(: an XML version of a lock :)
declare variable $lock-string as xs:string external;

(: parse the incoming XML :)
declare variable $lock as element() := xdmp:unquote($lock-string)/*;

(: lock the current document :)
xdmp:lock-acquire($uri)

let $document := fn:doc($uri)/*
let $oldOwner := ($document/*[fn:node-name(.) = 'ecm__lockOwner'])[1] (: assume there can only be one instance of a key :)

xdmp:set-response-content-type("text/xml")

if (fn:empty($oldOwner)) then
  (: document is not locked, lock it :)
  let $owner = ($lock/*[fn:node-name(.) = 'ecm__lockOwner'])[1]
  let $created = ($lock/*[fn:node-name(.) = 'ecm__lockCreated'])[1]

  xdmp:node-insert-child($document, $owner)
  xdmp:node-insert-child($document, $created)

  (: unlock the current document :)
  xdmp:lock-release($uri)

  <document />
else
  (: document is locked, return the current lock :)
  let $oldCreated := ($document/*[fn:node-name(.) = 'ecm__lockCreated'])[1]

  (: unlock the current document :)
  xdmp:lock-release($uri)

  return (
    <document>
      <ecm__lockOwner xsi:type="xs:string">$oldOwner</ecm__lockOwner>
      <ecm__lockOwner xsi:type="xs:string">$oldCreated</ecm__lockCreated>
    </document>
  )
