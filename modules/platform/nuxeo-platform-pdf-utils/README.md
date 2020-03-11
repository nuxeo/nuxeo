# Nuxeo Platform PDF Utils

A set of utilities to deal with PDFs from a [nuxeo](http://nuxeo.com) application.

## Operations

These operations can be used in Studio after importing their JSON definitions to the Automation registry.

_A quick reminder: To get the JSON definition of an operation, you can install the plug-in, start nuxeo server then go to {server:port}/nuxeo/site/automation/doc. All available operations are listed, find the one you are looking for and follow the links to get its JSON definition._

* **`PDF: Add Page Numbers`** (id `PDF.AddPageNumbers`)
  * Accepts a Blob, returns a Blob
  * The input blob must be a PDF
  * The returned blob contains the page numbers, displayed using the parameters (position, font, ...)
    * Notice the input blob is _not_ modified, a copy (+ page numbers) is returned

  * The following parameters let you tune the operation:
    * `startAtPage` (default: 1)
    * `startAtNumber` (default: 1)
    * `position`
      * Can be Bottom right, Bottom center, Bottom left, Top right, Top center, or Top left
      * Default: Bottom Right

    * `fontName` (default: Helvetica)
    * `fontSize` (default: 16)
    * `hex255Color`
      * Expressed as either 0xrrggbb or #rrggbb (case insensitive)
      * Default value: 0xffffff

    * `password`: If the PDF is encrypted, the password that will allow modification.

* **`PDF: Extract Pages`** (id `PDF.ExtractPages`)
  * Accept either a blob or a document as input
  * Returns a blob built with the extracted pages
  * If the input is a document, the `xpath` parameter must be used (default: `file:content`)
  * The following parameters let you tune the operation:
    * `startPage`
      * If < 1 => realigned to 1
      * If > `endPage` or > number of pages, a blank PDF is returned

    * `endPage`
      * If > number of pages, it is realigned to the number of pages

    * `fileName`
      * If not used, the filename will be the original file name plus the page range. For example, if the original name was "mydoc.pdf" and you extract pages 10 to 25, the resulting PDF will have a file name of "mydoc-10-25.pdf".

    * `pdfTitle`
      * If not used, title is not set
      * Warning: This is not the `dc:title`. It is the title as stored in the metadata of the PDF.

    * `pdfSubject`
      * If not used, subject is not set

    * `pdfAuthor`
      * If not used, author is not set

    * `password`: If the PDF is encrypted, the password that will allow extraction.

* **`PDF: Merge with Blob(s)`** (id `PDF.MergeWithBlobs`)
  * This operation merges all the blobs in a specific order (see below) and returns the final, merged PDF. Some properties (subject, ...) can also be set at the same time (optional)
  * The order of the PDF is the following:
    * Input blob
    * Blob referenced by the Context variable whose name is `toAppendVarName`
    * Blobs referenced as a `BlobList` by the Context variable whose name is `toAppendListVarName`
    * Blobs stored in the documents whose IDs are referenced as a `String List` by the Context variable whose name is `toAppendDocIDsVarName`
      * The `xpath` parameter is used to get the blob in each document
      * Optional. Default value is `file:content`

    * **Important**: The operation expects the _Context variable names_, _not the values_ of the variables. For example in Studio, say you have a multivalued String field named `myschema:the_ids`. It stores IDs of documents (typically, filled by the user using a "Multiple Documents Suggestion Widget"). In an Automation Chain, to merge the PDF embedded in a these documents with an input blob you would write (see we use `listArticles`, not `@{listArticles}`):
    ```
    . . . previous operations . . .
    Set Context Variable
      name: listArticles
      value: @{Document["myschema:the_ids"]}
    . . .
    PDF: Merge with Blob(s)
      ..other parameters
      toAppendDocIDsVarName: listArticles
    ```
    * These parameters are optional. Still, you probably want to use at least one of them :-)

* **`PDF: PDF: Merge with Document(s)`** (id `PDF.MergeWithDocs`)
  * See the documentation of `PDF: Merge with Blob(s)`
  * The difference is that the input is a document. The operation extracts the blob from the `xpath` field. Notice that it is ok for this blob to be null, the operation will still merge all the other blobs referenced in the parameters

* **`PDF: Info to Fields`** (id `PDF.InfoToFields`)
  * Extract the info of the PDF and put them in the fields referenced by the `properties` parameter, return the modified document. If there is no blob or if the blob is not a PDF, all the values referenced in `properties` are cleared (set to empty string, 0, ...)
  * Parameters:
    * `xpath`: The xpath of the blob to handle in the document. Default value is `file:content`
    * `save`: If true, the document is saved after its fields have been populated
    * `properties`
      * A `key=value` list (one key-value pair/line), where `key` is the xpath of the destination field and `value` is one of the following (case sensitive):

    ```
    File name
    File size
    PDF version
    Page count
    Page size
    Page width
    Page height
    Page layout
    Title
    Author
    Subject
    PDF producer
    Content creator
    Creation date
    Modification date
    Encrypted
    Keywords
    Media box width
    Media box height
    Crop box width
    Crop box height
    Can Print
    Can Modify
    Can Extract
    Can Modify Annotations
    Can Fill Forms
    Can Extract for Accessibility
    Can Assemble
    Can Print Degraded
    ```

    The  permission fields (starting with "Can ...") contain "true" or "false". Every field is "true" if the document is not encrypted or is opened with the _owner_ password.
      * For example, say you have an `InfoOfPDF` schema, prefix `iop`, with misc. fields. You could write:
    ```
    iop:pdf_version=PDF version
    iop:page_count=Page count
    iop:page_size=Page size
    ...etc...
    ```

* **`PDF: Watermark with Text`** (id `PDF.WatermarkWithText`)
  * Accepts a Blob, returns a Blob
  * Returns a _new_ blob combining the input PDF and the `watermark` text set on every pages, using the different `properties`.
  * If `watermark` is empty, a simple copy of the input blob is returned
  * `properties` is a `key=value` set where `key` can be one of the following. When not used, a default value applies:
    * `fontFamily` (default: "Helvetica")
    * `fontSize` (default: 36.0)
    * `rotation` (default: 0)
    * `hex255Color` (default: "#000000")
    * `alphaColor` (default: 0.5)
    * `xPosition` (default: 0)
    * `yPosition` (default: 0)
    * `invertY` (default: "false")

  * _More details about some `properties`_:
    * `xPosition` and `yPosition` start at the _bottom-left corner_ of each page
    * `alphaColor` is a float with any value between 0.0 and 1.0. Values < 0 or > 1 are reset to the default 0.5

* **`PDF: Watermark with Image`** (id `PDF.WatermarkWithImage`)
  * Accepts a Blob, returns a Blob
  * Returns a _new_ blob combining the input PDF and an image set on every page (using the `x`, `y`and `scale` parameters)
  * The image to use for the watermark can be one of the following:
    * `imageContextVarName`: A Context variable which references a Blob containing the image.
    * `imageDocRef`: The path or the ID of a document whose `file:content` field contains the image to use
      * _Notice_: If `imageDocRef`` is used, an `UnrestrictedSession` fetches its blob, so the PDF can be watermarked even if current user has not enough right to read the watermark itself.

    * _Notice_: The operation first checks for `imageContextVarName`.

  * `x` and `y` start at the bottom-left of the page
  * Dimensions of the image will be * by `scale` (so 1.0 means "Original size", 0.5 means half the size. 4 means four time the size, ...)

* **`PDF: Watermark with PDF`** (id `PDF.WatermarkWithPDF`)
  * Accepts a Blob, returns a Blob
  * Returns a _new_ blob combining the input pdf and an overlayed PDF on every page
  * The PDF to use for the watermark can be one of the following:
    * `pdfContextVarName`: A Context variable which references a Blob containing the PDF.
    * `pdfDocRef`: The path or the ID of a document whose `file:content` field contains the PDF to use
      * _Notice_: If `pdfDocRef`` is used, an`UnrestrictedSession` fetches its blob, so the PDF can be watermarked even if current user has not enough right to read the watermark itself.

    * _Notice_: The operation first checks for `pdfContextVarName`.

  * This operation uses `PDFBox` to overlay the PDF. The count of pages in each PDF can be different. Basically, the PDF to overlay will be repeated over the PDF to watermark. So for a final PDF of 10 pages:
    * If the overlay has one single page, this page is overlayed on the 10 pages
    * If the overlay has 3 pages, then the overlay will be made with pages 1 2 3 1 2 3 1 2 3 1

* **`PDF: Encrypt Read Only`** (id `PDF.EncryptReadOnly`)
  * Accepts Blob, Blobs, Document, Document(s)
  * Encrypts the PDF in readonly mode: User can print, copy, print degraded, extract info for accessibility, but cannot assemble, modify, modify annotations.
  * Returns a _new_ blob, copy of the original one, but encrypted
  * Parameters:
    * `originalOwnerPwd`: If the PDF is already encrypted, the password to open and modify it
    * `owenrPwd`; The new password to use for encryption. An owner can do everything on the PDF. If not passed, we use `originalOwnerPwd`
    * `userPwd`: The password for users, who will have restriction (read only) on the PDF
    * `keyLength`: The length to use for the encryption key. Possible values are 40 and 128. If no value is passed, 128 is used
    * `xpath`: If the input is `Document`  or `Documents`, the field where to get the blob from (`file:content` by default)

* **`PDF: Encrypt`** (id `PDF.Encrypt`)
  * Accepts Blob, Blobs, Document, Document(s)
  * Encrypts the PDF with the permissions given in `permissions`
  * Returns a _new_ blob, copy of the original one, but encrypted
  * Parameters:
    * `originalOwnerPwd`: If the PDF is already encrypted, the password to open and modify it
    * `owenrPwd`; The new password to use for encryption. An owner can do everything on the PDF. If not passed, we use `originalOwnerPwd`
    * `userPwd`: The password for users, who will have restriction (read only) on the PDF
    * `keyLength`: The length to use for the encryption key. Possible values are 40 and 128. If no value is passed, 128 is used
    * `xpath`: If the input is `Document`  or `Documents`, the field where to get the blob from (`file:content` by default)
    * `permissions` is a `key=value` set where `key` can be one of the following. When not used, `false` is applied (permission to do the action is denied):
    * `print`
    * `modify`
    * `copy`
    * `modifyAnnot`
    * `fillForms`
    * `extractForAccessibility`
    * `assemble`
    * `printDegraded`

    So for example, if you pass...

    ```
    print=true
    copy=true
    ```

    ... the user will only be able to print and copy.

* **`PDF: Remove Encryption`** (id `PDF.RemoveEncryption`)
  * Accepts Blob, Blobs, Document, Document(s)
  * Remove the encryption
  * Returns a _new_ blob, copy of the original one, not encrypted at all.
  * If the PDF already was encrypted, the operation still returns a copy of it (not the orginal blob)
  * Parameters:
    * `wnerPwd`: Password to use to decrypt and remove the permissions
    * `xpath`: If the input is `Document`  or `Documents`, the field where to get the blob from (`file:content` by default)

* **`PDF: Get Links`** (id `PDF.GetLinks`)
  * Accepts Blob
  * Returns a JSON String containing an array of objects. Each object has the following fields:
    * `page`: The page number where the link is
    * `subType`: The subType of the field. This is PDFBox (underlying java tool used to handle the PDF) label. It can be one of the following: `Launch`,  `GoToR` or `URI`.
    * `text`: The text of the link
    * `link`: The link itself (depending on `subType`)

  * Parameters
    * `type`: One of the following (case insensitive):
      * `Launch`
      * `Remote Go To`
      * `URI`

    * `getAll`: If `true`, the operation returns a list of all the links (of category Launch, Remote Go To or URI)
    * Important: If `getAll` is `false`, `type` _must_ be filled (or an error will occur)

* **`Conversion > PDF: Convert to Pictures`** (id `PDF.PDFToPictures`)
    * Accepts a Document
    * Parameters:
        * `fileName` (default: PDF file name)
            * This name will be applied to each image as `fileName` + `page number` + `.png`. Optional
        * `xpath` (default: `file:content`)
            * The path to the PDF blob. Optional
        * `password`
            * Password to unlock the PDF if required. Optional
    * Returns a Blob List of each page of the PDF as PNG image
    * These images can be used for OCR with [Nuxeo Vision](http://www.nuxeo.com/nuxeo-vision/), for example


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).
