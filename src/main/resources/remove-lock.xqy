(: the URI of the document to remove lock :)
declare variable $uri as xs:string external;

(: the owner wanting to remove lock :)
declare variable $owner as xs:string external;

let $document := fn:doc($uri)/*
let $oldOwner := ($document/*[fn:node-name(.) = fn:QName("", "ecm__lockOwner")])[1] (: assume there can only be one instance of a key :)
let $oldCreated := ($document/*[fn:node-name(.) = fn:QName("", "ecm__lockCreated")])[1] (: assume there can only be one instance of a key :)

return
if (fn:empty($document)) then
  fn:error(xs:QName("ERROR"), "Document not found")
else if (fn:empty($owner) or $owner = "" or $owner = $oldOwner) then
  (
    (: unconditional remove or owners match :)
    xdmp:node-delete($oldOwner),
    xdmp:node-delete($oldCreated),

    <document>
      {$oldOwner}
      {$oldCreated}
    </document>
  )
else if (fn:empty($oldOwner)) then
  <document />
else
  (: owners dont match :)
  (
    <document>
      {$oldOwner}
      {$oldCreated}
      <failed xsi:type="xs:boolean">true</failed>
    </document>
  )
