/*  WysiHat - WYSIWYG JavaScript framework, version 0.2
 *  (c) 2008-2009 Joshua Peek
 *
 *  WysiHat is freely distributable under the terms of an MIT-style license.
 *--------------------------------------------------------------------------*/


var WysiHat = {};

WysiHat.Editor = {
  attach: function(textarea, options, block) {
    options = $H(options);
    textarea = $(textarea);
    textarea.hide();

    var model = options.get('model') || WysiHat.iFrame;
    var initializer = block;

    return model.create(textarea, function(editArea) {
      var document = editArea.getDocument();
      var window = editArea.getWindow();

      editArea.load();

      Event.observe(window, 'focus', function(event) { editArea.focus(); });
      Event.observe(window, 'blur', function(event) { editArea.blur(); });

      editArea._observeEvents();

      if (Prototype.Browser.Gecko) {
        editArea.execCommand('undo', false, null);
      }

      if (initializer)
        initializer(editArea);

      editArea.focus();
    });
  },

  include: function(module) {
    this.includedModules = this.includedModules || $A([]);
    this.includedModules.push(module);
  },

  extend: function(object) {
    var modules = this.includedModules || $A([]);
    modules.each(function(module) {
      Object.extend(object, module);
    });
  }
};

WysiHat.Commands = (function() {
  function boldSelection() {
    this.execCommand('bold', false, null);
  }

  function boldSelected() {
    return this.queryCommandState('bold');
  }

  function underlineSelection() {
    this.execCommand('underline', false, null);
  }

  function underlineSelected() {
    return this.queryCommandState('underline');
  }

  function italicSelection() {
    this.execCommand('italic', false, null);
  }

  function italicSelected() {
    return this.queryCommandState('italic');
  }

  function strikethroughSelection() {
    this.execCommand('strikethrough', false, null);
  }

  function blockquoteSelection() {
    this.execCommand('blockquote', false, null);
  }

  function colorSelection(color) {
    this.execCommand('forecolor', false, color);
  }

  function linkSelection(url) {
    this.execCommand('createLink', false, url);
  }

  function unlinkSelection() {
    var node = this.selection.getNode();
    if (this.linkSelected())
      this.selection.selectNode(node);

    this.execCommand('unlink', false, null);
  }

  function linkSelected() {
    var node = this.selection.getNode();
    return node ? node.tagName.toUpperCase() == 'A' : false;
  }

  function insertOrderedList() {
    this.execCommand('insertorderedlist', false, null);
  }

  function insertUnorderedList() {
    this.execCommand('insertunorderedlist', false, null);
  }

  function insertImage(url) {
    this.execCommand('insertImage', false, url);
  }

  function insertHTML(html) {
    if (Prototype.Browser.IE) {
      var range = this._selection.getRange();
      range.pasteHTML(html);
      range.collapse(false);
      range.select();
    } else {
      this.execCommand('insertHTML', false, html);
    }
  }

  function execCommand(command, ui, value) {
    var document = this.getDocument();

    var handler = this.commands.get(command)
    if (handler)
      handler.bind(this)(value);
    else
      document.execCommand(command, ui, value);
  }

  function queryCommandState(state) {
    var document = this.getDocument();

    var handler = this.queryCommands.get(state)
    if (handler)
      return handler.bind(this)();
    else
      return document.queryCommandState(state);
  }

  return {
    boldSelection:          boldSelection,
    boldSelected:           boldSelected,
    underlineSelection:     underlineSelection,
    underlineSelected:      underlineSelected,
    italicSelection:        italicSelection,
    italicSelected:         italicSelected,
    strikethroughSelection: strikethroughSelection,
    blockquoteSelection:    blockquoteSelection,
    colorSelection:         colorSelection,
    linkSelection:          linkSelection,
    unlinkSelection:        unlinkSelection,
    linkSelected:           linkSelected,
    insertOrderedList:      insertOrderedList,
    insertUnorderedList:    insertUnorderedList,
    insertImage:            insertImage,
    insertHTML:             insertHTML,
    execCommand:            execCommand,
    queryCommandState:      queryCommandState,

    commands: $H({}),

    queryCommands: $H({
      link: linkSelected
    })
  };
})();

WysiHat.Editor.include(WysiHat.Commands);
WysiHat.Events = (function() {
  var eventsToFoward = [
    'click',
    'dblclick',
    'mousedown',
    'mouseup',
    'mouseover',
    'mousemove',
    'mouseout',
    'keypress',
    'keydown',
    'keyup'
  ];

  function forwardEvents(document, editor) {
    eventsToFoward.each(function(event) {
      Event.observe(document, event, function(e) {
        editor.fire('wysihat:' + event);
      });
    });
  }

  function observePasteEvent(window, document, editor) {
    Event.observe(document, 'keydown', function(event) {
      if (event.keyCode == 86)
        editor.fire("wysihat:paste");
    });

    Event.observe(window, 'paste', function(event) {
      editor.fire("wysihat:paste");
    });
  }

  function observeFocus(window, editor) {
    Event.observe(window, 'focus', function(event) {
      editor.fire("wysihat:focus");
    });

    Event.observe(window, 'blur', function(event) {
      editor.fire("wysihat:blur");
    });
  }

  function observeSelections(document, editor) {
    Event.observe(document, 'mouseup', function(event) {
      var range = editor.selection.getRange();
      if (!range.collapsed)
        editor.fire("wysihat:select");
    });
  }

  function observeChanges(document, editor) {
    var previousContents = editor.rawContent();
    Event.observe(document, 'keyup', function(event) {
      var contents = editor.rawContent();
      if (previousContents != contents) {
        editor.fire("wysihat:change");
        previousContents = contents;
      }
    });
  }

  function observeCursorMovements(document, editor) {
    var previousRange = editor.selection.getRange();
    var handler = function(event) {
      var range = editor.selection.getRange();
      if (previousRange != range) {
        editor.fire("wysihat:cursormove");
        editor.previousRange = range;
      }
    };

    Event.observe(document, 'keyup', handler);
    Event.observe(document, 'mouseup', handler);
  }

  function observeEvents() {
    if (this._observers_setup)
      return;

    var document = this.getDocument();
    var window = this.getWindow();

    forwardEvents(document, this);
    observePasteEvent(window, document, this);
    observeFocus(window, this);
    observeSelections(document, this);
    observeChanges(document, this);
    observeCursorMovements(document, this);

    this._observers_setup = true;
  }

  return {
    _observeEvents: observeEvents
  };
})();

WysiHat.Editor.include(WysiHat.Events);
WysiHat.Persistence = (function() {
  function outputFilter(text) {
    return text.formatHTMLOutput();
  }

  function inputFilter(text) {
    return text.formatHTMLInput();
  }

  function content() {
    return this.outputFilter(this.rawContent());
  }

  function setContent(text) {
    this.setRawContent(this.inputFilter(text));
  }

  function save() {
    this.textarea.value = this.content();
  }

  function load() {
    this.setContent(this.textarea.value);
  }

  function reload() {
    this.selection.setBookmark();
    this.save();
    this.load();
    this.selection.moveToBookmark();
  }

  return {
    outputFilter: outputFilter,
    inputFilter:  inputFilter,
    content:      content,
    setContent:   setContent,
    save:         save,
    load:         load,
    reload:       reload
  };
})();

WysiHat.Editor.include(WysiHat.Persistence);
WysiHat.Window = (function() {
  function getDocument() {
    return this.contentDocument || this.contentWindow.document;
  }

  function getWindow() {
    if (this.contentDocument)
      return this.contentDocument.defaultView;
    else if (this.contentWindow.document)
      return this.contentWindow;
    else
      return null;
  }

  function focus() {
    this.getWindow().focus();

    if (this.hasFocus)
      return;

    this.hasFocus = true;
  }

  function blur() {
    this.hasFocus = false;
  }

  return {
    getDocument: getDocument,
    getWindow: getWindow,
    focus: focus,
    blur: blur
  };
})();

WysiHat.Editor.include(WysiHat.Window);
WysiHat.iFrame = {
  create: function(textarea, callback) {
    var editArea = new Element('iframe', { 'id': textarea.id + '_editor', 'class': 'editor' });

    Object.extend(editArea, WysiHat.iFrame.Methods);
    WysiHat.Editor.extend(editArea);

    editArea.attach(textarea, callback);
    textarea.insert({before: editArea});

    return editArea;
  }
};

WysiHat.iFrame.Methods = {
  attach: function(element, callback) {
    this.textarea = element;

    this.observe('load', function() {
      try {
        var document = this.getDocument();
      } catch(e) { return; } // No iframe, just stop

      this.selection = new WysiHat.Selection(this);

      if (this.ready && document.designMode == 'on')
        return;

      this.setStyle({});
      document.designMode = 'on';
      callback(this);
      this.ready = true;
      this.fire('wysihat:ready');
    });
  },

  whenReady: function(callback) {
    if (this.ready) {
      callback(this);
    } else {
      var editor = this;
      editor.observe('wysihat:ready', function() { callback(editor); });
    }
    return this;
  },

  setStyle: function(styles) {
    var document = this.getDocument();

    var element = this;
    if (!this.ready)
      return setTimeout(function() { element.setStyle(styles); }, 1);

    if (Prototype.Browser.IE) {
      var style = document.createStyleSheet();
      style.addRule("body", "border: 0");
      style.addRule("p", "margin: 0");

      $H(styles).each(function(pair) {
        var value = pair.first().underscore().dasherize() + ": " + pair.last();
        style.addRule("body", value);
      });
    } else if (Prototype.Browser.Opera) {
      var style = Element('style').update("p { margin: 0; }");
      var head = document.getElementsByTagName('head')[0];
      head.appendChild(style);
    } else {
      Element.setStyle(document.body, styles);
    }

    return this;
  },

  rawContent: function() {
    var document = this.getDocument();

    if (document.body)
      return document.body.innerHTML;
    else
      return "";
  },

  setRawContent: function(text) {
    var document = this.getDocument();
    if (document.body)
      document.body.innerHTML = text;
  }
};
WysiHat.Editable = {
  create: function(textarea, callback) {
    var editArea = new Element('div', {
      'id': textarea.id + '_editor',
      'class': 'editor',
      'contenteditable': 'true'
    });
    editArea.textarea = textarea;

    WysiHat.Editor.extend(editArea);
    Object.extend(editArea, WysiHat.Editable.Methods);

    callback(editArea);

    textarea.insert({before: editArea});

    return editArea;
  }
};

WysiHat.Editable.Methods = {
  getDocument: function() {
    return document;
  },

  getWindow: function() {
    return window;
  },

  rawContent: function() {
    return this.innerHTML;
  },

  setRawContent: function(text) {
    this.innerHTML = text;
  }
};

Object.extend(String.prototype, (function() {
  function formatHTMLOutput() {
    var text = String(this);
    text = text.tidyXHTML();

    if (Prototype.Browser.WebKit) {
      text = text.replace(/(<div>)+/g, "\n");
      text = text.replace(/(<\/div>)+/g, "");

      text = text.replace(/<p>\s*<\/p>/g, "");

      text = text.replace(/<br \/>(\n)*/g, "\n");
    } else if (Prototype.Browser.Gecko) {
      text = text.replace(/<p>/g, "");
      text = text.replace(/<\/p>(\n)?/g, "\n");

      text = text.replace(/<br \/>(\n)*/g, "\n");
    } else if (Prototype.Browser.IE || Prototype.Browser.Opera) {
      text = text.replace(/<p>(&nbsp;|&#160;|\s)<\/p>/g, "<p></p>");

      text = text.replace(/<br \/>/g, "");

      text = text.replace(/<p>/g, '');

      text = text.replace(/&nbsp;/g, '');

      text = text.replace(/<\/p>(\n)?/g, "\n");

      text = text.gsub(/^<p>/, '');
      text = text.gsub(/<\/p>$/, '');
    }

    text = text.gsub(/<b>/, "<strong>");
    text = text.gsub(/<\/b>/, "</strong>");

    text = text.gsub(/<i>/, "<em>");
    text = text.gsub(/<\/i>/, "</em>");

    text = text.replace(/\n\n+/g, "</p>\n\n<p>");

    text = text.gsub(/(([^\n])(\n))(?=([^\n]))/, "#{2}<br />\n");

    text = '<p>' + text + '</p>';

    text = text.replace(/<p>\s*/g, "<p>");
    text = text.replace(/\s*<\/p>/g, "</p>");

    var element = Element("body");
    element.innerHTML = text;

    if (Prototype.Browser.WebKit || Prototype.Browser.Gecko) {
      var replaced;
      do {
        replaced = false;
        element.select('span').each(function(span) {
          if (span.hasClassName('Apple-style-span')) {
            span.removeClassName('Apple-style-span');
            if (span.className == '')
              span.removeAttribute('class');
            replaced = true;
          } else if (span.getStyle('fontWeight') == 'bold') {
            span.setStyle({fontWeight: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<strong>' + span.innerHTML + '</strong>');
            replaced = true;
          } else if (span.getStyle('fontStyle') == 'italic') {
            span.setStyle({fontStyle: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<em>' + span.innerHTML + '</em>');
            replaced = true;
          } else if (span.getStyle('textDecoration') == 'underline') {
            span.setStyle({textDecoration: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<u>' + span.innerHTML + '</u>');
            replaced = true;
          } else if (span.attributes.length == 0) {
            span.replace(span.innerHTML);
            replaced = true;
          }
        });
      } while (replaced);

    }

    var acceptableBlankTags = $A(['BR', 'IMG']);

    for (var i = 0; i < element.descendants().length; i++) {
      var node = element.descendants()[i];
      if (node.innerHTML.blank() && !acceptableBlankTags.include(node.nodeName) && node.id != 'bookmark')
        node.remove();
    }

    text = element.innerHTML;
    text = text.tidyXHTML();

    text = text.replace(/<br \/>(\n)*/g, "<br />\n");
    text = text.replace(/<\/p>\n<p>/g, "</p>\n\n<p>");

    text = text.replace(/<p>\s*<\/p>/g, "");

    text = text.replace(/\s*$/g, "");

    return text;
  }

  function formatHTMLInput() {
    var text = String(this);

    var element = Element("body");
    element.innerHTML = text;

    if (Prototype.Browser.Gecko || Prototype.Browser.WebKit) {
      element.select('strong').each(function(element) {
        element.replace('<span style="font-weight: bold;">' + element.innerHTML + '</span>');
      });
      element.select('em').each(function(element) {
        element.replace('<span style="font-style: italic;">' + element.innerHTML + '</span>');
      });
      element.select('u').each(function(element) {
        element.replace('<span style="text-decoration: underline;">' + element.innerHTML + '</span>');
      });
    }

    if (Prototype.Browser.WebKit)
      element.select('span').each(function(span) {
        if (span.getStyle('fontWeight') == 'bold')
          span.addClassName('Apple-style-span');

        if (span.getStyle('fontStyle') == 'italic')
          span.addClassName('Apple-style-span');

        if (span.getStyle('textDecoration') == 'underline')
          span.addClassName('Apple-style-span');
      });

    text = element.innerHTML;
    text = text.tidyXHTML();

    text = text.replace(/<\/p>(\n)*<p>/g, "\n\n");

    text = text.replace(/(\n)?<br( \/)?>(\n)?/g, "\n");

    text = text.replace(/^<p>/g, '');
    text = text.replace(/<\/p>$/g, '');

    if (Prototype.Browser.Gecko) {
      text = text.replace(/\n/g, "<br>");
      text = text + '<br>';
    } else if (Prototype.Browser.WebKit) {
      text = text.replace(/\n/g, "</div><div>");
      text = '<div>' + text + '</div>';
      text = text.replace(/<div><\/div>/g, "<div><br></div>");
    } else if (Prototype.Browser.IE || Prototype.Browser.Opera) {
      text = text.replace(/\n/g, "</p>\n<p>");
      text = '<p>' + text + '</p>';
      text = text.replace(/<p><\/p>/g, "<p>&nbsp;</p>");
      text = text.replace(/(<p>&nbsp;<\/p>)+$/g, "");
    }

    return text;
  }

  function tidyXHTML() {
    var text = String(this);

    text = text.gsub(/\r\n?/, "\n");

    text = text.gsub(/<([A-Z]+)([^>]*)>/, function(match) {
      return '<' + match[1].toLowerCase() + match[2] + '>';
    });

    text = text.gsub(/<\/([A-Z]+)>/, function(match) {
      return '</' + match[1].toLowerCase() + '>';
    });

    text = text.replace(/<br>/g, "<br />");

    return text;
  }

  return {
    formatHTMLOutput: formatHTMLOutput,
    formatHTMLInput:  formatHTMLInput,
    tidyXHTML:        tidyXHTML
  };
})());
Object.extend(String.prototype, {
  sanitize: function(options) {
    return Element("div").update(this).sanitize(options).innerHTML.tidyXHTML();
  }
});

Element.addMethods({
  sanitize: function(element, options) {
    element = $(element);
    options = $H(options);
    var allowed_tags = $A(options.get('tags') || []);
    var allowed_attributes = $A(options.get('attributes') || []);
    var sanitized = Element(element.nodeName);

    $A(element.childNodes).each(function(child) {
      if (child.nodeType == 1) {
        var children = $(child).sanitize(options).childNodes;

        if (allowed_tags.include(child.nodeName.toLowerCase())) {
          var new_child = Element(child.nodeName);
          allowed_attributes.each(function(attribute) {
            if ((value = child.readAttribute(attribute)))
              new_child.writeAttribute(attribute, value);
          });
          sanitized.appendChild(new_child);

          $A(children).each(function(grandchild) { new_child.appendChild(grandchild); });
        } else {
          $A(children).each(function(grandchild) { sanitized.appendChild(grandchild); });
        }
      } else if (child.nodeType == 3) {
        sanitized.appendChild(child);
      }
    });
    return sanitized;
  }
});


if (typeof Range == 'undefined') {
  Range = function(ownerDocument) {
    this.ownerDocument = ownerDocument;

    this.startContainer = this.ownerDocument.documentElement;
    this.startOffset    = 0;
    this.endContainer   = this.ownerDocument.documentElement;
    this.endOffset      = 0;

    this.collapsed = true;
    this.commonAncestorContainer = this._commonAncestorContainer(this.startContainer, this.endContainer);

    this.detached = false;

    this.START_TO_START = 0;
    this.START_TO_END   = 1;
    this.END_TO_END     = 2;
    this.END_TO_START   = 3;
  }

  Range.CLONE_CONTENTS   = 0;
  Range.DELETE_CONTENTS  = 1;
  Range.EXTRACT_CONTENTS = 2;

  if (!document.createRange) {
    document.createRange = function() {
      return new Range(this);
    };
  }

  Object.extend(Range.prototype, (function() {
    function cloneContents() {
      return _processContents(this, Range.CLONE_CONTENTS);
    }

    function cloneRange() {
      try {
        var clone = new Range(this.ownerDocument);
        clone.startContainer          = this.startContainer;
        clone.startOffset             = this.startOffset;
        clone.endContainer            = this.endContainer;
        clone.endOffset               = this.endOffset;
        clone.collapsed               = this.collapsed;
        clone.commonAncestorContainer = this.commonAncestorContainer;
        clone.detached                = this.detached;

        return clone;

      } catch (e) {
        return null;
      };
    }

    function collapse(toStart) {
      if (toStart) {
        this.endContainer = this.startContainer;
        this.endOffset    = this.startOffset;
        this.collapsed    = true;
      } else {
        this.startContainer = this.endContainer;
        this.startOffset    = this.endOffset;
        this.collapsed      = true;
      }
    }

    function compareBoundaryPoints(compareHow, sourceRange) {
      try {
        var cmnSelf, cmnSource, rootSelf, rootSource;

        cmnSelf   = this.commonAncestorContainer;
        cmnSource = sourceRange.commonAncestorContainer;

        rootSelf = cmnSelf;
        while (rootSelf.parentNode) {
          rootSelf = rootSelf.parentNode;
        }

        rootSource = cmnSource;
        while (rootSource.parentNode) {
          rootSource = rootSource.parentNode;
        }

        switch (compareHow) {
          case this.START_TO_START:
            return _compareBoundaryPoints(this, this.startContainer, this.startOffset, sourceRange.startContainer, sourceRange.startOffset);
            break;
          case this.START_TO_END:
            return _compareBoundaryPoints(this, this.startContainer, this.startOffset, sourceRange.endContainer, sourceRange.endOffset);
            break;
          case this.END_TO_END:
            return _compareBoundaryPoints(this, this.endContainer, this.endOffset, sourceRange.endContainer, sourceRange.endOffset);
            break;
          case this.END_TO_START:
            return _compareBoundaryPoints(this, this.endContainer, this.endOffset, sourceRange.startContainer, sourceRange.startOffset);
            break;
        }
      } catch (e) {};

      return null;
    }

    function deleteContents() {
      try {
        _processContents(this, Range.DELETE_CONTENTS);
      } catch (e) {}
    }

    function detach() {
      this.detached = true;
    }

    function extractContents() {
      try {
        return _processContents(this, Range.EXTRACT_CONTENTS);
      } catch (e) {
        return null;
      };
    }

    function insertNode(newNode) {
      try {
        var n, newText, offset;

        switch (this.startContainer.nodeType) {
          case Node.CDATA_SECTION_NODE:
          case Node.TEXT_NODE:
            newText = this.startContainer.splitText(this.startOffset);
            this.startContainer.parentNode.insertBefore(newNode, newText);
            break;
          default:
            if (this.startContainer.childNodes.length == 0) {
              offset = null;
            } else {
              offset = this.startContainer.childNodes(this.startOffset);
            }
            this.startContainer.insertBefore(newNode, offset);
        }
      } catch (e) {}
    }

    function selectNode(refNode) {
      this.setStartBefore(refNode);
      this.setEndAfter(refNode);
    }

    function selectNodeContents(refNode) {
      this.setStart(refNode, 0);
      this.setEnd(refNode, refNode.childNodes.length);
    }

    function setStart(refNode, offset) {
      try {
        var endRootContainer, startRootContainer;

        this.startContainer = refNode;
        this.startOffset    = offset;

        endRootContainer = this.endContainer;
        while (endRootContainer.parentNode) {
          endRootContainer = endRootContainer.parentNode;
        }
        startRootContainer = this.startContainer;
        while (startRootContainer.parentNode) {
          startRootContainer = startRootContainer.parentNode;
        }
        if (startRootContainer != endRootContainer) {
          this.collapse(true);
        } else {
          if (_compareBoundaryPoints(this, this.startContainer, this.startOffset, this.endContainer, this.endOffset) > 0) {
            this.collapse(true);
          }
        }

        this.collapsed = _isCollapsed(this);

        this.commonAncestorContainer = _commonAncestorContainer(this.startContainer, this.endContainer);
      } catch (e) {}
    }

    function setStartAfter(refNode) {
      this.setStart(refNode.parentNode, _nodeIndex(refNode) + 1);
    }

    function setStartBefore(refNode) {
      this.setStart(refNode.parentNode, _nodeIndex(refNode));
    }

    function setEnd(refNode, offset) {
      try {
        this.endContainer = refNode;
        this.endOffset    = offset;

        endRootContainer = this.endContainer;
        while (endRootContainer.parentNode) {
          endRootContainer = endRootContainer.parentNode;
        }
        startRootContainer = this.startContainer;
        while (startRootContainer.parentNode) {
          startRootContainer = startRootContainer.parentNode;
        }
        if (startRootContainer != endRootContainer) {
          this.collapse(false);
        } else {
          if (_compareBoundaryPoints(this, this.startContainer, this.startOffset, this.endContainer, this.endOffset) > 0) {
            this.collapse(false);
          }
        }

        this.collapsed = _isCollapsed(this);

        this.commonAncestorContainer = _commonAncestorContainer(this.startContainer, this.endContainer);

      } catch (e) {}
    }

    function setEndAfter(refNode) {
      this.setEnd(refNode.parentNode, _nodeIndex(refNode) + 1);
    }

    function setEndBefore(refNode) {
      this.setEnd(refNode.parentNode, _nodeIndex(refNode));
    }

    function surroundContents(newParent) {
      try {
        var n, fragment;

        while (newParent.firstChild) {
          newParent.removeChild(newParent.firstChild);
        }

        fragment = this.extractContents();
        this.insertNode(newParent);
        newParent.appendChild(fragment);
        this.selectNode(newParent);
      } catch (e) {}
    }

    function _compareBoundaryPoints(range, containerA, offsetA, containerB, offsetB) {
      var c, offsetC, n, cmnRoot, childA;
      if (containerA == containerB) {
        if (offsetA == offsetB) {
          return 0; // equal
        } else if (offsetA < offsetB) {
          return -1; // before
        } else {
          return 1; // after
        }
      }

      c = containerB;
      while (c && c.parentNode != containerA) {
        c = c.parentNode;
      }
      if (c) {
        offsetC = 0;
        n = containerA.firstChild;
        while (n != c && offsetC < offsetA) {
          offsetC++;
          n = n.nextSibling;
        }
        if (offsetA <= offsetC) {
          return -1; // before
        } else {
          return 1; // after
        }
      }

      c = containerA;
      while (c && c.parentNode != containerB) {
        c = c.parentNode;
      }
      if (c) {
        offsetC = 0;
        n = containerB.firstChild;
        while (n != c && offsetC < offsetB) {
          offsetC++;
          n = n.nextSibling;
        }
        if (offsetC < offsetB) {
          return -1; // before
        } else {
          return 1; // after
        }
      }

      cmnRoot = range._commonAncestorContainer(containerA, containerB);
      childA = containerA;
      while (childA && childA.parentNode != cmnRoot) {
        childA = childA.parentNode;
      }
      if (!childA) {
        childA = cmnRoot;
      }
      childB = containerB;
      while (childB && childB.parentNode != cmnRoot) {
        childB = childB.parentNode;
      }
      if (!childB) {
        childB = cmnRoot;
      }

      if (childA == childB) {
        return 0; // equal
      }

      n = cmnRoot.firstChild;
      while (n) {
        if (n == childA) {
          return -1; // before
        }
        if (n == childB) {
          return 1; // after
        }
        n = n.nextSibling;
      }

      return null;
    }

    function _commonAncestorContainer(containerA, containerB) {
      var parentStart = containerA, parentEnd;
      while (parentStart) {
        parentEnd = containerB;
        while (parentEnd && parentStart != parentEnd) {
          parentEnd = parentEnd.parentNode;
        }
        if (parentStart == parentEnd) {
          break;
        }
        parentStart = parentStart.parentNode;
      }

      if (!parentStart && containerA.ownerDocument) {
        return containerA.ownerDocument.documentElement;
      }

      return parentStart;
    }

    function _isCollapsed(range) {
      return (range.startContainer == range.endContainer && range.startOffset == range.endOffset);
    }

    function _offsetInCharacters(node) {
      switch (node.nodeType) {
        case Node.CDATA_SECTION_NODE:
        case Node.COMMENT_NODE:
        case Node.ELEMENT_NODE:
        case Node.PROCESSING_INSTRUCTION_NODE:
          return true;
        default:
          return false;
      }
    }

    function _processContents(range, action) {
      try {

        var cmnRoot, partialStart = null, partialEnd = null, fragment, n, c, i;
        var leftContents, leftParent, leftContentsParent;
        var rightContents, rightParent, rightContentsParent;
        var next, prev;
        var processStart, processEnd;
        if (range.collapsed) {
          return null;
        }

        cmnRoot = range.commonAncestorContainer;

        if (range.startContainer != cmnRoot) {
          partialStart = range.startContainer;
          while (partialStart.parentNode != cmnRoot) {
            partialStart = partialStart.parentNode;
          }
        }

        if (range.endContainer != cmnRoot) {
          partialEnd = range.endContainer;
          while (partialEnd.parentNode != cmnRoot) {
            partialEnd = partialEnd.parentNode;
          }
        }

        if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
          fragment = range.ownerDocument.createDocumentFragment();
        }

        if (range.startContainer == range.endContainer) {
          switch (range.startContainer.nodeType) {
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.TEXT_NODE:
              if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
                c = range.startContainer.cloneNode();
                c.deleteData(range.endOffset, range.startContainer.data.length - range.endOffset);
                c.deleteData(0, range.startOffset);
                fragment.appendChild(c);
              }
              if (action == Range.EXTRACT_CONTENTS || action == Range.DELETE_CONTENTS) {
                range.startContainer.deleteData(range.startOffset, range.endOffset - range.startOffset);
              }
              break;
            case Node.PROCESSING_INSTRUCTION_NODE:
              break;
            default:
              n = range.startContainer.firstChild;
              for (i = 0; i < range.startOffset; i++) {
                n = n.nextSibling;
              }
              while (n && i < range.endOffset) {
                next = n.nextSibling;
                if (action == Range.EXTRACT_CONTENTS) {
                  fragment.appendChild(n);
                } else if (action == Range.CLONE_CONTENTS) {
                  fragment.appendChild(n.cloneNode());
                } else {
                  range.startContainer.removeChild(n);
                }
                n = next;
                i++;
              }
          }
          range.collapse(true);
          return fragment;
        }


        if (range.startContainer != cmnRoot) {
          switch (range.startContainer.nodeType) {
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.TEXT_NODE:
              if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
                c = range.startContainer.cloneNode(true);
                c.deleteData(0, range.startOffset);
                leftContents = c;
              }
              if (action == Range.EXTRACT_CONTENTS || action == Range.DELETE_CONTENTS) {
                range.startContainer.deleteData(range.startOffset, range.startContainer.data.length - range.startOffset);
              }
              break;
            case Node.PROCESSING_INSTRUCTION_NODE:
              break;
            default:
              if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
                leftContents = range.startContainer.cloneNode(false);
              }
              n = range.startContainer.firstChild;
              for (i = 0; i < range.startOffset; i++) {
                n = n.nextSibling;
              }
              while (n && i < range.endOffset) {
                next = n.nextSibling;
                if (action == Range.EXTRACT_CONTENTS) {
                  fragment.appendChild(n);
                } else if (action == Range.CLONE_CONTENTS) {
                  fragment.appendChild(n.cloneNode());
                } else {
                  range.startContainer.removeChild(n);
                }
                n = next;
                i++;
              }
          }

          leftParent = range.startContainer.parentNode;
          n = range.startContainer.nextSibling;
          for(; leftParent != cmnRoot; leftParent = leftParent.parentNode) {
            if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
              leftContentsParent = leftParent.cloneNode(false);
              leftContentsParent.appendChild(leftContents);
              leftContents = leftContentsParent;
            }

            for (; n; n = next) {
              next = n.nextSibling;
              if (action == Range.EXTRACT_CONTENTS) {
                leftContents.appendChild(n);
              } else if (action == Range.CLONE_CONTENTS) {
                leftContents.appendChild(n.cloneNode(true));
              } else {
                leftParent.removeChild(n);
              }
            }
            n = leftParent.nextSibling;
          }
        }

        if (range.endContainer != cmnRoot) {
          switch (range.endContainer.nodeType) {
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.TEXT_NODE:
              if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
                c = range.endContainer.cloneNode(true);
                c.deleteData(range.endOffset, range.endContainer.data.length - range.endOffset);
                rightContents = c;
              }
              if (action == Range.EXTRACT_CONTENTS || action == Range.DELETE_CONTENTS) {
                range.endContainer.deleteData(0, range.endOffset);
              }
              break;
            case Node.PROCESSING_INSTRUCTION_NODE:
              break;
            default:
              if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
                rightContents = range.endContainer.cloneNode(false);
              }
              n = range.endContainer.firstChild;
              if (n && range.endOffset) {
                for (i = 0; i+1 < range.endOffset; i++) {
                  next = n.nextSibling;
                  if (!next) {
                    break;
                  }
                  n = next;
                }
                for (; n; n = prev) {
                  prev = n.previousSibling;
                  if (action == Range.EXTRACT_CONTENTS) {
                    rightContents.insertBefore(n, rightContents.firstChild);
                  } else if (action == Range.CLONE_CONTENTS) {
                    rightContents.insertBefore(n.cloneNode(True), rightContents.firstChild);
                  } else {
                    range.endContainer.removeChild(n);
                  }
                }
              }
          }

          rightParent = range.endContainer.parentNode;
          n = range.endContainer.previousSibling;
          for(; rightParent != cmnRoot; rightParent = rightParent.parentNode) {
            if (action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) {
              rightContentsParent = rightContents.cloneNode(false);
              rightContentsParent.appendChild(rightContents);
              rightContents = rightContentsParent;
            }

            for (; n; n = prev) {
              prev = n.previousSibling;
              if (action == Range.EXTRACT_CONTENTS) {
                rightContents.insertBefore(n, rightContents.firstChild);
              } else if (action == Range.CLONE_CONTENTS) {
                rightContents.appendChild(n.cloneNode(true), rightContents.firstChild);
              } else {
                rightParent.removeChild(n);
              }
            }
            n = rightParent.previousSibling;
          }
        }

        if (range.startContainer == cmnRoot) {
          processStart = range.startContainer.firstChild;
          for (i = 0; i < range.startOffset; i++) {
            processStart = processStart.nextSibling;
          }
        } else {
          processStart = range.startContainer;
          while (processStart.parentNode != cmnRoot) {
            processStart = processStart.parentNode;
          }
          processStart = processStart.nextSibling;
        }
        if (range.endContainer == cmnRoot) {
          processEnd = range.endContainer.firstChild;
          for (i = 0; i < range.endOffset; i++) {
            processEnd = processEnd.nextSibling;
          }
        } else {
          processEnd = range.endContainer;
          while (processEnd.parentNode != cmnRoot) {
            processEnd = processEnd.parentNode;
          }
        }

        if ((action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) && leftContents) {
          fragment.appendChild(leftContents);
        }

        if (processStart) {
          for (n = processStart; n && n != processEnd; n = next) {
            next = n.nextSibling;
            if (action == Range.EXTRACT_CONTENTS) {
              fragment.appendChild(n);
            } else if (action == Range.CLONE_CONTENTS) {
              fragment.appendChild(n.cloneNode(true));
            } else {
              cmnRoot.removeChild(n);
            }
          }
        }

        if ((action == Range.EXTRACT_CONTENTS || action == Range.CLONE_CONTENTS) && rightContents) {
          fragment.appendChild(rightContents);
        }

        if (action == Range.EXTRACT_CONTENTS || action == Range.DELETE_CONTENTS) {
          if (!partialStart && !partialEnd) {
            range.collapse(true);
          } else if (partialStart) {
            range.startContainer = partialStart.parentNode;
            range.endContainer = partialStart.parentNode;
            range.startOffset = range.endOffset = range._nodeIndex(partialStart) + 1;
          } else if (partialEnd) {
            range.startContainer = partialEnd.parentNode;
            range.endContainer = partialEnd.parentNode;
            range.startOffset = range.endOffset = range._nodeIndex(partialEnd);
          }
        }

        return fragment;

      } catch (e) {
        return null;
      };
    }

    function _nodeIndex(refNode) {
      var nodeIndex = 0;
      while (refNode.previousSibling) {
        nodeIndex++;
        refNode = refNode.previousSibling;
      }
      return nodeIndex;
    }

    return {
      setStart:       setStart,
      setEnd:         setEnd,
      setStartBefore: setStartBefore,
      setStartAfter:  setStartAfter,
      setEndBefore:   setEndBefore,
      setEndAfter:    setEndAfter,

      collapse: collapse,

      selectNode:         selectNode,
      selectNodeContents: selectNodeContents,

      compareBoundaryPoints: compareBoundaryPoints,

      deleteContents:  deleteContents,
      extractContents: extractContents,
      cloneContents:   cloneContents,

      insertNode:       insertNode,
      surroundContents: surroundContents,

      cloneRange: cloneRange,
      toString:   toString,
      detach:     detach,

      _commonAncestorContainer: _commonAncestorContainer
    };
  })());
}

if (!window.getSelection) {
  window.getSelection = function() {
    return Selection.getInstance();
  };

  SelectionImpl = function() {
    this.anchorNode = null;
    this.anchorOffset = 0;
    this.focusNode = null;
    this.focusOffset = 0;
    this.isCollapsed = true;
    this.rangeCount = 0;
    this.ranges = [];
  }

  Object.extend(SelectionImpl.prototype, (function() {
    function addRange(r) {
      return true;
    }

    function collapse() {
      return true;
    }

    function collapseToStart() {
      return true;
    }

    function collapseToEnd() {
      return true;
    }

    function getRangeAt() {
      return true;
    }

    function removeAllRanges() {
      this.anchorNode = null;
      this.anchorOffset = 0;
      this.focusNode = null;
      this.focusOffset = 0;
      this.isCollapsed = true;
      this.rangeCount = 0;
      this.ranges = [];
    }

    function _addRange(r) {
      if (r.startContainer.nodeType != Node.TEXT_NODE) {
        var start = this._getRightStart(r.startContainer);
        var startOffset = 0;
      } else {
        var start = r.startContainer;
        var startOffset = r.startOffset;
      }
      if (r.endContainer.nodeType != Node.TEXT_NODE) {
        var end = this._getRightEnd(r.endContainer);
        var endOffset = end.data.length;
      } else {
        var end = r.endContainer;
        var endOffset = r.endOffset;
      }

      var rStart = this._selectStart(start, startOffset);
      var rEnd   = this._selectEnd(end,endOffset);
      rStart.setEndPoint('EndToStart', rEnd);
      rStart.select();
      document.selection._selectedRange = r;
    }

    function _getRightStart(start, offset) {
      if (start.nodeType != Node.TEXT_NODE) {
        if (start.nodeType == Node.ELEMENT_NODE) {
          start = start.childNodes(offset);
        }
        return getNextTextNode(start);
      } else {
        return null;
      }
    }

    function _getRightEnd(end, offset) {
      if (end.nodeType != Node.TEXT_NODE) {
        if (end.nodeType == Node.ELEMENT_NODE) {
          end = end.childNodes(offset);
        }
        return getPreviousTextNode(end);
      } else {
        return null;
      }
    }

    function _selectStart(start, offset) {
      var r = document.body.createTextRange();
      if (start.nodeType == Node.TEXT_NODE) {
        var moveCharacters = offset, node = start;
        var moveToNode = null, collapse = true;
        while (node.previousSibling) {
          switch (node.previousSibling.nodeType) {
            case Node.ELEMENT_NODE:
              moveToNode = node.previousSibling;
              collapse = false;
              break;
            case Node.TEXT_NODE:
              moveCharacters += node.previousSibling.data.length;
          }
          if (moveToNode != null) {
            break;
          }
          node = node.previousSibling;
        }
        if (moveToNode == null) {
          moveToNode = start.parentNode;
        }

        r.moveToElementText(moveToNode);
        r.collapse(collapse);
        r.move('Character', moveCharacters);
        return r;
      } else {
        return null;
      }
    }

    function _selectEnd(end, offset) {
      var r = document.body.createTextRange(), node = end;
      if (end.nodeType == 3) {
        var moveCharacters = end.data.length - offset;
        var moveToNode = null, collapse = false;
        while (node.nextSibling) {
          switch (node.nextSibling.nodeType) {
            case Node.ELEMENT_NODE:
              moveToNode = node.nextSibling;
              collapse   = true;
              break;
            case Node.TEXT_NODE:
              moveCharacters += node.nextSibling.data.length;
              break;
          }
          if (moveToNode != null) {
            break;
          }
          node = node.nextSibling;
        }
        if (moveToNode == null) {
          moveToNode = end.parentNode;
          collapse   = false;
        }

        switch (moveToNode.nodeName.toLowerCase()) {
          case 'p':
          case 'div':
          case 'h1':
          case 'h2':
          case 'h3':
          case 'h4':
          case 'h5':
          case 'h6':
            moveCharacters++;
        }

        r.moveToElementText(moveToNode);
        r.collapse(collapse);

        r.move('Character', -moveCharacters);
        return r;
      }

      return null;
    }

    function getPreviousTextNode(node) {
      var stack = [];
      var current = null;
      while (node) {
        stack = [];
        current = node;
        while (current) {
          while (current) {
          if (current.nodeType == 3 && current.data.replace(/^\s+|\s+$/, '').length) {
            return current;
          }
          if (current.previousSibling) {
            stack.push (current.previousSibling);
          }
          current = current.lastChild;
        }
        current = stack.pop();
        }
        node = node.previousSibling;
      }
      return null;
    }

    function getNextTextNode(node) {
      var stack = [];
      var current = null;
      while (node) {
        stack = [];
        current = node;
        while (current) {
          while (current) {
            if (current.nodeType == 3 && current.data.replace(/^\s+|\s+$/, '').length) {
                return current;
            }
            if (current.nextSibling) {
                stack.push (current.nextSibling);
            }
            current = current.firstChild;
          }
          current = stack.pop();
        }
        node = node.nextSibling;
      }
      return null;
    }

    return {
      removeAllRanges: removeAllRanges,

      _addRange: _addRange,
      _getRightStart: _getRightStart,
      _getRightEnd: _getRightEnd,
      _selectStart: _selectStart,
      _selectEnd: _selectEnd
    };
  })());

  Selection = new function() {
    var instance = null;
    this.getInstance = function() {
      if (instance == null) {
        return (instance = new SelectionImpl());
      } else {
        return instance;
      }
    };
  };
}

Object.extend(Range.prototype, (function() {
  function getNode() {
    var node = this.commonAncestorContainer;

    if (this.startContainer == this.endContainer)
      if (this.startOffset - this.endOffset < 2)
        node = this.startContainer.childNodes[this.startOffset];

    while (node.nodeType == Node.TEXT_NODE)
      node = node.parentNode;

    return node;
  }

  return {
    getNode: getNode
  };
})());
WysiHat.Selection = Class.create((function() {
  function initialize(editor) {
    this.window = editor.getWindow();
    this.document = editor.getDocument();
  }

  function getSelection() {
    return this.window.getSelection ? this.window.getSelection() : this.document.selection;
  }

  function getRange() {
    var selection = this.getSelection();

    try {
      var range;
      if (selection.getRangeAt)
        range = selection.getRangeAt(0);
      else
        range = selection.createRange();
    } catch(e) { return null; }

    if (Prototype.Browser.WebKit) {
      range.setStart(selection.baseNode, selection.baseOffset);
      range.setEnd(selection.extentNode, selection.extentOffset);
    }

    return range;
  }

  function selectNode(node) {
    var selection = this.getSelection();

    if (Prototype.Browser.IE) {
      var range = createRangeFromElement(this.document, node);
      range.select();
    } else if (Prototype.Browser.WebKit) {
      selection.setBaseAndExtent(node, 0, node, node.innerText.length);
    } else if (Prototype.Browser.Opera) {
      range = this.document.createRange();
      range.selectNode(node);
      selection.removeAllRanges();
      selection.addRange(range);
    } else {
      var range = createRangeFromElement(this.document, node);
      selection.removeAllRanges();
      selection.addRange(range);
    }
  }

  function getNode() {
    var nodes = null, candidates = [], children, el;
    var range = this.getRange();

    if (!range)
      return null;

    var parent;
    if (range.parentElement)
      parent = range.parentElement();
    else
      parent = range.commonAncestorContainer;

    if (parent) {
      while (parent.nodeType != 1) parent = parent.parentNode;
      if (parent.nodeName.toLowerCase() != "body") {
        el = parent;
        do {
          el = el.parentNode;
          candidates[candidates.length] = el;
        } while (el.nodeName.toLowerCase() != "body");
      }
      children = parent.all || parent.getElementsByTagName("*");
      for (var j = 0; j < children.length; j++)
        candidates[candidates.length] = children[j];
      nodes = [parent];
      for (var ii = 0, r2; ii < candidates.length; ii++) {
        r2 = createRangeFromElement(this.document, candidates[ii]);
        if (r2 && compareRanges(range, r2))
          nodes[nodes.length] = candidates[ii];
      }
    }

    return nodes.first();
  }

  function createRangeFromElement(document, node) {
    if (document.body.createTextRange) {
      var range = document.body.createTextRange();
      range.moveToElementText(node);
    } else if (document.createRange) {
      var range = document.createRange();
      range.selectNodeContents(node);
    }
    return range;
  }

  function compareRanges(r1, r2) {
    if (r1.compareEndPoints) {
      return !(
        r2.compareEndPoints('StartToStart', r1) == 1 &&
        r2.compareEndPoints('EndToEnd', r1) == 1 &&
        r2.compareEndPoints('StartToEnd', r1) == 1 &&
        r2.compareEndPoints('EndToStart', r1) == 1
        ||
        r2.compareEndPoints('StartToStart', r1) == -1 &&
        r2.compareEndPoints('EndToEnd', r1) == -1 &&
        r2.compareEndPoints('StartToEnd', r1) == -1 &&
        r2.compareEndPoints('EndToStart', r1) == -1
      );
    } else if (r1.compareBoundaryPoints) {
      return !(
        r2.compareBoundaryPoints(0, r1) == 1 &&
        r2.compareBoundaryPoints(2, r1) == 1 &&
        r2.compareBoundaryPoints(1, r1) == 1 &&
        r2.compareBoundaryPoints(3, r1) == 1
        ||
        r2.compareBoundaryPoints(0, r1) == -1 &&
        r2.compareBoundaryPoints(2, r1) == -1 &&
        r2.compareBoundaryPoints(1, r1) == -1 &&
        r2.compareBoundaryPoints(3, r1) == -1
      );
    }

    return null;
  };

  function setBookmark() {
    var bookmark = this.document.getElementById('bookmark');
    if (bookmark)
      bookmark.parentNode.removeChild(bookmark);

    bookmark = this.document.createElement('span');
    bookmark.id = 'bookmark';
    bookmark.innerHTML = '&nbsp;';

    if (Prototype.Browser.IE) {
      var range = this.document.selection.createRange();
      var parent = this.document.createElement('div');
      parent.appendChild(bookmark);
      range.collapse();
      range.pasteHTML(parent.innerHTML);
    }
    else {
      var range = this.getRange();
      range.insertNode(bookmark);
    }
  }

  function moveToBookmark() {
    var bookmark = this.document.getElementById('bookmark');
    if (!bookmark)
      return;

    if (Prototype.Browser.IE) {
      var range = this.getRange();
      range.moveToElementText(bookmark);
      range.collapse();
      range.select();
    } else if (Prototype.Browser.WebKit) {
      var selection = this.getSelection();
      selection.setBaseAndExtent(bookmark, 0, bookmark, 0);
    } else {
      var range = this.getRange();
      range.setStartBefore(bookmark);
    }

    bookmark.parentNode.removeChild(bookmark);
  }

  return {
    initialize:     initialize,
    getSelection:   getSelection,
    getRange:       getRange,
    getNode:        getNode,
    selectNode:     selectNode,
    setBookmark:    setBookmark,
    moveToBookmark: moveToBookmark
  };
})());
WysiHat.Toolbar = Class.create((function() {
  function initialize(editor) {
    this.editor = editor;
    this.element = this.createToolbarElement();
  }

  function createToolbarElement() {
    var toolbar = new Element('div', { 'class': 'editor_toolbar' });
    this.editor.insert({before: toolbar});
    return toolbar;
  }

  function addButtonSet(set) {
    var toolbar = this;
    $A(set).each(function(button){
      toolbar.addButton(button);
    });
  }

  function addButton(options, handler) {
    options = $H(options);

    if (!options.get('name'))
      options.set('name', options.get('label').toLowerCase());
    var name = options.get('name');

    var button = this.createButtonElement(this.element, options);

    var handler = this.buttonHandler(name, options);
    this.observeButtonClick(button, handler);

    var handler = this.buttonStateHandler(name, options);
    this.observeStateChanges(button, name, handler)
  }

  function createButtonElement(toolbar, options) {
    var button = Element('a', {
      'class': 'button', 'href': '#'
    });
    button.update('<span>' + options.get('label') + '</span>');
    button.addClassName(options.get('name'));

    toolbar.appendChild(button);

    return button;
  }

  function buttonHandler(name, options) {
    if (options.handler)
      return options.handler;
    else if (options.get('handler'))
      return options.get('handler');
    else
      return function(editor) { editor.execCommand(name); };
  }

  function observeButtonClick(element, handler) {
    var toolbar = this;
    element.observe('click', function(event) {
      handler(toolbar.editor);
      toolbar.editor.fire("wysihat:change");
      toolbar.editor.fire("wysihat:cursormove");
      Event.stop(event);
    });
  }

  function buttonStateHandler(name, options) {
    if (options.query)
      return options.query;
    else if (options.get('query'))
      return options.get('query');
    else
      return function(editor) { return editor.queryCommandState(name); };
  }

  function observeStateChanges(element, name, handler) {
    var toolbar = this;
    var previousState = false;
    toolbar.editor.observe("wysihat:cursormove", function(event) {
      var state = handler(toolbar.editor);
      if (state != previousState) {
        previousState = state;
        toolbar.updateButtonState(element, name, state);
      }
    });
  }

  function updateButtonState(element, name, state) {
    if (state)
      element.addClassName('selected');
    else
      element.removeClassName('selected');
  }

  return {
    initialize:           initialize,
    createToolbarElement: createToolbarElement,
    addButtonSet:         addButtonSet,
    addButton:            addButton,
    createButtonElement:  createButtonElement,
    buttonHandler:        buttonHandler,
    observeButtonClick:   observeButtonClick,
    buttonStateHandler:   buttonStateHandler,
    observeStateChanges:  observeStateChanges,
    updateButtonState:    updateButtonState
  };
})());

WysiHat.Toolbar.ButtonSets = {};

WysiHat.Toolbar.ButtonSets.Basic = $A([
  { label: "Bold" },
  { label: "Underline" },
  { label: "Italic" }
]);
