/* ***** BEGIN LICENSE BLOCK *****
 * Licensed under Version: MPL 1.1/GPL 2.0/LGPL 2.1
 * Full Terms at http://mozile.mozdev.org/0.8/LICENSE
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Jorgen Horstink, David Kingma and James A. Overton's code.
 *
 * The Initial Developers of the Original Code are Jorgen Horstink and David Kingma.
 * Portions created by the Initial Developer are Copyright (C) 2005-2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *	James A. Overton <james@overton.ca>
 *
 * ***** END LICENSE BLOCK ***** */

/**
 * @fileoverview Provides a minimal W3C Range implementation under Internet Explorer.
 * <p>History: The original code was written by Jorgen Horstink (http://jorgenhorstink.nl/2006/03/11/w3c-range-in-internet-explorer/).
 * It was extensively modified by David Kingma.</p>
 * <p>This version has been adapted from the work of James A. Overton for the Mozile project
 * (http://mozile.mozdev.org/0.8/doc/jsdoc/overview-summary-InternetExplorerRange.js.html). See http://mozile.mozdev.org</p>
 * <p>Some code was changed and a lot was removed to only keep what we need in the Nuxeo Annotations module.</p>
 *
 * @author Thomas Roger <troger@nuxeo.com>
 */

if (!window.Node) var Node =
    {
      ELEMENT_NODE                :  1,
      ATTRIBUTE_NODE              :  2,
      TEXT_NODE                   :  3,
      CDATA_SECTION_NODE          :  4,
      ENTITY_REFERENCE_NODE       :  5,
      ENTITY_NODE                 :  6,
      PROCESSING_INSTRUCTION_NODE :  7,
      COMMENT_NODE                :  8,
      DOCUMENT_NODE               :  9,
      DOCUMENT_TYPE_NODE          : 10,
      DOCUMENT_FRAGMENT_NODE      : 11,
      NOTATION_NODE               : 12
    }


/**
 * Range object, see http://www.w3.org/TR/DOM-Level-2-Traversal-Range/ranges.html
 * @constructor
 */
InternetExplorerRange = function(range) {
  /**
   * A reference to an IE native TextRange object.
   * @private
   * @type TextRange
   */
  this._range = null;

  if(range)
    this._range = range;

  /**
   * A boolean indicating whether the range's start and end points are at the same position.
   * @type Boolean
   */
  this.collapsed = null;

  /**
   * The deepest Node that contains the startContainer and endContainer Nodes.
   * @type Node
   */
  this.commonAncestorContainer = null;

  /**
   * The Node within which the Range starts.
   * @type Node
   */
  this.startContainer = null;

  /**
   * A number representing where in the endContainer the Range starts.
   * @type Integer
   */
  this.startOffset = null;

  /**
   * The Node within which the Range ends.
   * @type Node
   */
  this.endContainer = null;

  /**
   * A number representing where in the endContainer the Range ends.
   * @type Integer
   */
  this.endOffset = null;
}


/**
 * Initializes the properties of this range according to the internal
 * IE range (_range).
 * @private
 * @type Void
 */
InternetExplorerRange.prototype._init = function () {

  //startPoint
  var beginRange = this._range.duplicate();
  beginRange.collapse(true);
  var position = this._getPosition(beginRange);
  this.startContainer = position.node;
  this.startOffset = position.offset;

  //endPoint
  var endRange = this._range.duplicate();
  endRange.collapse(false);
  position = this._getPosition(endRange);
  this.endContainer = position.node;
  this.endOffset = position.offset;
}


/**
 * Takes an Internet Explorer TextRange object and returns a W3C node and offset pair.
 * <p>The basic method is as follows:
 * <ul><li>Create a new range with its start at the beginning of the element and its end at the target position. Set the rangeLength to the length of the range's text.
 * <li>Starting with the first child, for each child:
 * <ul><li>If the child is a text node, and its length is less than the rangeLength, then move the range's start by the text node's length.
 * <li>If the child is a text node and its length is less than the rangeLength then we've found the target. Return the node and use the remaining rangeLength as the offset.
 * <li>If the child is an element, move the range's start by the length of the element's innerText.
 * </ul></ul>
 * <p>This algorithm works fastest when the target is close to the beginning of the parent element.
 * The current implementation is smart enough pick the closest end point of the parent element (i.e. the start or the end), and work forward or backward from there.
 * @private
 * @param {TextRange} textRange A TextRange object. Its start position will be found.
 * @type Object
 * @return An object with "node" and "offset" properties.
 */
InternetExplorerRange.prototype._getPosition = function(textRange) {
  var element = textRange.parentElement();
  var range = element.ownerDocument.body.createTextRange();
  range.moveToElementText(element);
  range.setEndPoint("EndToStart", textRange);
  var rangeLength = range.text.length;

  // Choose Direction
  if(rangeLength < element.innerText.length / 2) {
    var direction = 1;
    var node = element.firstChild;
  }
  else {
    direction = -1;
    node = element.lastChild;
    range.moveToElementText(element);
    range.setEndPoint("StartToStart", textRange);
    rangeLength = range.text.length;
  }

  // Loop through child nodes
  while(node) {
    switch(node.nodeType) {
      case Node.TEXT_NODE:
        nodeLength = node.data.length;
        if(nodeLength < rangeLength) {
          var difference = rangeLength - nodeLength;
          if(direction == 1) range.moveStart("character", difference);
          else range.moveEnd("character", -difference);
          rangeLength = difference;
        }
        else {
          if(direction == 1) return {node: node, offset: rangeLength};
          else return {node: node, offset: nodeLength - rangeLength};
        }
        break;

      case Node.ELEMENT_NODE:
        nodeLength = node.innerText.length;
        if(direction == 1) range.moveStart("character", nodeLength);
        else range.moveEnd("character", -nodeLength);
        rangeLength = rangeLength - nodeLength;
        break;
    }

    if(direction == 1) node = node.nextSibling;
    else node = node.previousSibling;
  }


  // TODO: This should throw a warning.
  //throw("Error in InternetExplorerRange._getPosition: Ran out of child nodes before the range '"+ textRange.text +"' inside '"+ mozile.xpath.getXPath(element) +"' was found.");

  // The TextRange was not found. Return a reasonable value instead.
  return {node: element, offset: 0};

}

/**
 * Collapses the Range to one of its boundary points.
 * @param {Boolean} toStart When true the Range is collapsed to the start position, when false to the end position.
 * @type Void
 */
InternetExplorerRange.prototype.collapse = function (toStart) {
  this._range.collapse(toStart);

  //update the properties
  if(toStart) {
    this.endContainer = this.startContainer;
    this.endOffset = this.startOffset;
  } else {
    this.startContainer = this.endContainer;
    this.startOffset = this.endOffset;
  }
  this._collapsed();
};

/**
 * Returns the text of the Range.
 * @type String
 */
InternetExplorerRange.prototype.toString = function () {
  return this._range.text;
};
