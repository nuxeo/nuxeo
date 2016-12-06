# Nuxeo Diff

This repo hosts the source code of a plugin for Nuxeo Platform that allows to render a diff between two documents or two versions of a document.
The comparison takes into account all the properties shared by the documents, which means that if a comparison is done between two documents of a different type, only the schemas in common will be "diffed".
The comparison also takes into account blob-type properties.


## Building and deploying

    mvn clean install

## Deploying

Install [the Nuxeo Diff Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-diff).

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addons_nuxeo-diff-master)](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-diff-master/)

## Configuring

### Diff display

The _DiffDisplayService_ offers several extension points to configure the document diff display.
Most of the code samples exposed here can be found in the _diff-display-contrib.xml_ and _diff-widgets-contrib.xml_ files.

#### Configuring groups of properties to display with the diffDisplay extension point.

A _diffDisplay_ contribution represents a number of _diffBlocks_ that you want to display when asking for a document comparison.
It is bound to a document type.
A _diffBlock_ contribution represents a number of properties (fields) that you want to display (see next section).

When asking for the comparison between 2 versions of a document, the _diffDisplay_ bound to the document type or a super type is used.
If no _diffDisplay_ is found for this type or a super type a fall back is done on the default diff display mode: one block per document schema and for each block all the fields of the schema that are different.
_Beware that in this case the order of the schemas and of the fields is undefined._

When asking for the comparison between 2 documents:
- if they are of the same type: if  a _diffDisplay_ is found for this type or a super type then it is used, else a fall back is done on the default diff display mode
- if they are of different types: if  a _diffDisplay_ is found for a common super type then it is used, else a fall back is done on the default diff display mode.

For example, this is the _diffDisplay_ contribution bound to the _File_ type:

    <diffDisplay type="File">
      <diffBlocks>
        <diffBlock name="heading" />
        <diffBlock name="dublincore" />
        <diffBlock name="files" />
      </diffBlocks>
    </diffDisplay>

_Note that the order of the diffBlocks is taken into account when rendering the diff display._

#### Configuring a group of properties to display with the diffBlock extension point

A _diffBlock_ contribution represents a number of _fields_ that you want to display. It is rendered as a foldable box.
The_label_ attribute of a _diffBlock_ contribution is used as the title of the foldable box.
A _field_ is defined by its _schema_ and its _name_.

For example, this is the "heading" _diffBlock_ contribution:

    <diffBlock name="heading" label="label.diffBlock.heading">
      <fields>
        <field schema="dublincore" name="title" />
        <field schema="dublincore" name="description" />
      </fields>
    </diffBlock>

_Note that the order of the fields is taken into account when rendering the diff block._

For complex properties, you can contribute inside the _field_ element the property _items_ that you want to display:

    <field schema="complextypes" name="complex">
      <items>
        <item name="stringItem" />
        <item name="thirdItem" />
        <item name="fourthItem" />
      </items>
    </field>

_Note that the order of the items is taken into account when rendering the field._

This is used for the _files_ field of the _files_ diff block:

    <field schema="files" name="files">
      <items>
        <!-- Display the file only, not the filename which is managed
             by the file widget type -->
        <item name="file" displayContentDiffLinks="true" />
      </items>
    </field>

If no _items_ are specified, all the property items are displayed.

For content properties (that hold a blob) or string ones you can set the _displayContentDiffLinks_ attribute to _true_ on a _field_ or an _item_ to display the content diff links.
These links will open a fancybox showing the detailed content diff using the usual green and red colors to distinguish the added/removed parts of the content.
For now, 2 links are displayed: _Textual diff_ based on a text conversion and _Html diff_ based on an html conversion (keeps the content layout).
See the _Content diff_ section for more information.

#### Configuring property widgets with the widgets extension point

##### Principle

When rendering a _diffBlock_, the _DiffDisplayService_ builds a layout definition on the fly, including a layout row for each _field_ of the _diffBlock_.
Each row contains a widget definition for the _field_, and the layout template renders 2 instances of this widget definition: one for the left document and one for the right document.
The content diff links, if displayed, are also rendered by a widget inside the layout row.

How is the widget definition built for a given _field_?
A lookup is done in the _LayoutStore_ service to find a specific widget definition named with the xpath of the property.
If such a definition is not found, a lookup is done to find a generic widget definition named with the type of the property.

This allows you to only contribute a specific widget definition if the generic one doesn't match your needs for a given field, typically if you need a custom template, label or custom properties.

##### Example

Lets say we have contributed the following _diffBlock_:

    <diffBlock name="myCustomBlock" label="label.diffBlock.custom">
      <fields>
        <field schema="file" name="content" />
        <field schema="dublincore" name="title" />
      </fields>
    </diffBlock>

and the following widgets to the _widgets_ extension point of the _org.nuxeo.ecm.platform.forms.layout.LayoutStore_ component:

    <extension target="org.nuxeo.ecm.platform.forms.layout.LayoutStore"
      point="widgets">

      <widget name="file:content" type="file">
        <categories>
          <category>diff</category>
        </categories>
        <labels>
          <label mode="any">label.summary.download.file</label>
        </labels>
        <translated>true</translated>
        <properties mode="any">
        </properties>
      </widget>

      <widget name="string" type="template">
        <categories>
          <category>diff</category>
        </categories>
        <properties mode="any">
          <property name="widgetType">text</property>
          <property name="template">
            /widgets/generic_diff_widget_template.xhtml
          </property>
        </properties>
      </widget>

    </extension>

When rendering the "myCustomBlock" _diffBlock_, the _DiffDisplayService_ will:

- Look for a specific widget definition named "file:content" in the _LayoutStore_, find it and use it for the "file:content" field.

- Look for a specific widget definition named "dublincore:title" in the _LayoutStore_, won't find it and therefore will look for a generic widget definition named with the field type, ie. "string", find it and use it for the dublincore:title field.

In this use case, the "string" generic widget definition is sufficient to display the "dublincore:title" field.
It uses a widget of type "text" with "label.dublincore.title" as a label and "dublincore:title" as a field definition.
We can easily understand here the interest of generic widgets: once you have the type and xpath of a property, the matching widget definition can be computed on the fly using the property type to guess the widget type ("string" => "text", "date" => "datetime", etc.) and the property xpath for the field definition and label.

The "file:content" specific widget definition is contributed here to use a custom label "label.summary.download.file" instead of the one that would have been generated for the "content" generic widget definition: "label.file.content".

_Note that in both cases (generic and specific) you don't need to define the widget field definitions since they are automatically computed from the property xpath, except in particular cases like "note:note" where the "mime-type" field is needed._

##### List and complex properties

You might already know that the widgets used to display list and complex properties have subwidgets.
In the case of a list property, a subwidget is needed for the list items; in the case of a complex property, a subwidget is needed for each item of the complex property.
The lookup done by the _DiffDisplayService_ for the first-level widgets is also done recursively for the subwidgets!

###### List property

For a list property, lets take the example of "dublincore:contributors", which is a string list.

- To display the list, nothing special is needed so the "scalarList" generic widget definition can be used.

- To display a list item (a contributor, which is of type "string"), the "string" generic widget definition doesn't match our needs: it would display the contributor's username whereas we want to display its fullname (firstname lastname).
So we need a specific widget definition for the list items subwidget to use a custom template able to display the contributor's fullname.
The name of this widget definition must match the xpath of the list item property, ie. "dublincore:contributors/item".

Therefore, two widget definitions are involved:

- The "scalarList" generic widget definition:

        <widget name="scalarList" type="template">
          <categories>
            <category>diff</category>
          </categories>
          <properties mode="any">
            <property name="display">inline</property>
            <property name="displayAllItems">false</property>
            <property name="displayItemIndexes">true</property>
            <property name="template">
              /widgets/list_diff_widget_template.xhtml
            </property>
          </properties>
        </widget>

- The "dublincore:contributors/item" specific widget definition:

        <widget name="dublincore:contributors/item" type="template">
          <categories>
            <category>diff</category>
          </categories>
          <labels>
            <label mode="any">label.dublincore.contributors.item</label>
          </labels>
          <translated>true</translated>
          <properties mode="any">
            <property name="template">/widgets/contributors_item_widget_template.xhtml
            </property>
          </properties>
        </widget>

###### Complex property

For a complex property, lets take the example of a "complextypes:complex" property with two items "stringItem" and "directoryItem".
"stringItem" is a simple string, but "directoryItem" is a string that needs to be bound to the "myDirectory" directory.

- To display the complex property, nothing special is needed so the "complex" generic widget definition can be used.

- To display the "directoryItem" item, the "string" generic widget definition doesn't match our needs: it would display the directory entry code stored in the backend whereas we want to display its label.
So we need a specific widget definition for the "directoryItem" subwidget to use the "selectOneDirectory" widget type bound to the "myDirectory" directory.
As for a list item, the name of this widget definition must match the xpath of the complex property item, ie. "complextypes:complex/directoryItem".

Therefore, two widget definitions are involved:

- The "complex" generic widget definition:

        <widget name="complex" type="template">
          <categories>
            <category>diff</category>
          </categories>
          <properties mode="any">
            <property name="display">inline</property>
            <property name="template">
              /widgets/complex_diff_widget_template.xhtml
            </property>
          </properties>
        </widget>

- The "complextypes:complex/directoryItem" specific widget definition:

        <widget name="complextypes:complex/directoryItem" type="selectOneDirectory">
          <categories>
            <category>diff</category>
          </categories>
          <labels>
            <label mode="any">label.complextypes.complex.directoryItem</label>
          </labels>
          <translated>true</translated>
          <properties mode="any">
            <property name="directoryName">myDirectory</property>
            <property name="localize">true</property>
            <property name="ordering">ordering,label</property>
          </properties>
        </widget>

###### Useful widget properties

You can use the following properties on a list widget definition (typically "scalarList", "complexList" or "files:files"):

`<property name="displayAllItems">[true|false]</property>`
If set to _true_, all the list items will be displayed, otherwise only the different ones will be.

`<property name="displayItemIndexes">[true|false]</property>`
If set to _true_, a subwidget will be added to the widget definition to display the list item indexes.

You can use the following property on a complex widget definition (typically "complex"):

`<property name="display">[inline|*]</property>`
If set to _inline_ the complex items will be displayed as a table with one line and one column per item, otherwise as a table with one column and one line per item.

##### About the value bound to the diff widgets

If you take a look at the _layout_diff_template.xhtml_, you will see that the _value_ passed to the _<nxl:widget>_ tag is _#{value.leftValue}_ or _#{value.rightValue}_, _value_ being the object passed to the _<nxl:layout>_ tag _value_ attribute: _diffDisplayBlock_, of type _DiffDisplayBlockImpl_.
The _leftValue_ and _rightValue_ members of _DiffDisplayBlockImpl_ are of type Map<String, Map<String, PropertyDiffDisplay>>. The first level Map keys are schema names, the second level ones are field keys.
Finally, the _PropertyDiffDisplay_ object has two members: _value_ and _styleClass_, _value_ holding the value to display and _styleClass_ the css style class to apply to the &lt;span&gt; wrapping the value.

For example if we compare two documents where only the "dublincore:title" property is different ("My first doc" and "My second doc") we could have the following _diffDisplayBlock_ object:

    diffDisplayBlock.getLeftValue() = {dublincore={title={value="My first doc", styleClass="redBackgroundColor"}}}
    diffDisplayBlock.getRightValue() = {dublincore={title={value="My second doc", styleClass="greenBackgroundColor"}}}

On the widget side, the field definitions must match the _diffDisplayBlock_ object structure, that's why the generated field definitions of the widget used for "dublincore:title" would be:

    <fields>
      <field>dublincore:title/value</field>
      <field>dublincore:title/styleClass</field>
    </fields>

This is important to know when designing a custom template for a diff widget (ie. where field definitions are automatically generated): you can use #{field 0} for the value itself and #{field 1} for the css style class associated to the value.
By default, only the items of a complex property or of a list property where the _displayAllItems_ widget property is _true_ can have a styleClass equal to _redBackgroundColor_ or _greenBackgroundColor_ in order to highlight the different items among all.

#### To summarize: what you need to contribute to have a nice diff result for your custom document types

- A _diffDisplay_ contribution for each document type.

- The associated _diffBlock_ contributions. Don't forget that you can specify the items you want to display for a complex property and the fields/items for which you want to display the content diff links.

- The specific widgets needed when the generic ones don't match your needs. Typically for a date property if you need to change the date format, or for a property bound to a directory to specifiy the directory name. Also don't forget that you can contribute a specific widget for a complex property item or a list item, using the item xpath.

- The labels for each _diffBlock_, each widget and each subwidget in your _messages*.properties_ files.
For example:

        label.diffBlock.custom=My custom diff block title
        label.customSchema.customField=Custom field
        label.customSchema.customField.firstComplexItem=First item of the complex custom field

### Content diff

Work in progress!

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
