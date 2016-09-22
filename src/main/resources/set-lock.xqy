(: the URI of the document to set lock :)
declare variable $uri as xs:string external;

(: an XML version of a lock :)
declare variable $lock-string as xs:string external;

(: parse the incoming XML :)
declare variable $lock as element() := xdmp:unquote($lock-string)/*;

let $document := fn:doc($uri)/*
let $oldOwner := ($document/*[fn:node-name(.) = fn:QName("", "ecm__lockOwner")])[1] (: assume there can only be one instance of a key :)

return
if (fn:empty($document)) then
  fn:error(xs:QName("ERROR"), "Document not found")
else if (fn:empty($oldOwner)) then
  (: document is not locked, lock it :)
  let $owner := ($lock/*[fn:node-name(.) = fn:QName("", "ecm__lockOwner")])[1]
  let $created := ($lock/*[fn:node-name(.) = fn:QName("", "ecm__lockCreated")])[1]
  return (
    xdmp:node-insert-child($document, $owner),
    xdmp:node-insert-child($document, $created),

    <document />
  )
else
  (: document is locked, return the current lock :)
  let $oldCreated := ($document/*[fn:node-name(.) = fn:QName("", "ecm__lockCreated")])[1]
  return (
    <document>
      {$oldOwner}
      {$oldCreated}
    </document>
  )
