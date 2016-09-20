
(: the URI of the document to remove lock :)
declare variable $uri as xs:string external;

(: the owner wanting to remove lock :)
declare variable $owner as xs:string external;

let $document := fn:doc($uri)/*
let $oldOwner := ($document/*[fn:node-name(.) = 'ecm__lockOwner'])[1] (: assume there can only be one instance of a key :)
let $oldCreated := ($document/*[fn:node-name(.) = 'ecm__lockCreated'])[1] (: assume there can only be one instance of a key :)

if (fn:empty($owner) or $owner = "" or $owner = $oldOwner) then
  (: unconditional remove or owners match :)
  xdmp:node-delete($oldOwner)
  xdmp:node-delete($oldCreated)

  return (
    <document>
      <ecm__lockOwner xsi:type="xs:string">$oldOwner</ecm__lockOwner>
      <ecm__lockOwner xsi:type="xs:string">$oldCreated</ecm__lockCreated>
    </document>
  )
else if (fn:empty($oldOwner)) then
  return <document />
else
  (: owners don't match :)
  return (
      <document>
        <ecm__lockOwner xsi:type="xs:string">$oldOwner</ecm__lockOwner>
        <ecm__lockOwner xsi:type="xs:string">$oldCreated</ecm__lockCreated>
        <failed xsi:type="xs:boolean">true</failed>
      </document>
    )
