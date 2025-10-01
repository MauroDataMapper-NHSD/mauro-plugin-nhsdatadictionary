/*
 * Copyright 2020-2025 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package datadictionary.publish.structure

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.dita.elements.langref.base.Topic
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDCode
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.publish.structure.AliasesSection
import uk.nhs.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.datadictionary.publish.structure.CodesSection
import uk.nhs.datadictionary.publish.structure.DescriptionSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.ItemLinkListSection
import uk.nhs.datadictionary.publish.structure.WhereUsedSection
import uk.nhs.datadictionary.services.NhsDataDictionaryService

@MicronautTest(startApplication = false, environments = ['secured'])
@Property(name = "flyway.enabled", value = "false")
class NhsAttributeStructureSpec extends DataDictionaryComponentStructureSpec<NhsDDAttribute> {

    @Inject
    NhsDataDictionaryService dataDictionaryService

    NhsDDClass relatedActivityClass

    NhsDDAttribute previousItemCodesChange
    NhsDDAttribute previousItemLinkedElementsChange

    @Override
    String getDefinition() {
        """<p>The date, month, year and century, or any combination of these elements, that is of relevance to an 
<a href="dm:Classes and Attributes|dc:ACTIVITY">ACTIVITY</a>.</p>"""
    }

    @Override
    void setupRelatedItems() {
        relatedActivityClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("79ab1e21-4407-4aae-b777-7b7920fa1963"),
            branchId: branchId,
            name: "ACTIVITY")
    }

    @Override
    protected void setupComponentPathResolver() {
        super.setupComponentPathResolver()

        componentPathResolver.add(relatedActivityClass.getMauroPath(), relatedActivityClass)
    }

    @Override
    protected void setupCatalogueItemPathResolver() {
        super.setupCatalogueItemPathResolver()

        catalogueItemPathResolver.add(relatedActivityClass.getMauroPath(), relatedActivityClass.catalogueItemId)
    }

    @Override
    void setupActiveItem() {
        activeItem = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: "ACTIVITY DATE",
            definition: definition,
            otherProperties: [
                'aliasPlural': 'ACTIVITY DATES'
            ])

        NhsDDElement relatedActivityDateElement = new NhsDDElement(
            name: "ACTIVITY DATE (CRITICAL CARE)",
            catalogueItemId: UUID.fromString("b5170409-97aa-464e-9aad-657c8b2e00f8"),
            branchId: branchId)

        addWhereUsed(activeItem, relatedActivityDateElement)
        addWhereUsed(
            activeItem,
            new NhsDDClass(name: "ACTIVITY DATE TIME", catalogueItemId: UUID.fromString("542a6963-cce2-4c8f-b60d-b86b13d43bbe"), branchId: branchId))

        addChangeLog(
            activeItem,
            new NhsDDChangeLog(reference: "CR1000", referenceUrl: "https://test.nhs.uk/change/cr1000", description: "Change 1000", implementationDate: "01 April 2024"),
            new NhsDDChangeLog(reference: "CR2000", referenceUrl: "https://test.nhs.uk/change/cr2000", description: "Change 2000", implementationDate: "01 September 2024"))

        activeItem.codes.add(new NhsDDCode(code: "S", definition: "Standard", webOrder: 1))
        activeItem.codes.add(new NhsDDCode(code: "U", definition: "Undefined", webOrder: 0))
        activeItem.codes.add(new NhsDDCode(code: "B", definition: "British Summer Time", webOrder: 2, webPresentation: "British Summer Time - see <a href=\"https://time.com\">link</a>"))

        activeItem.instantiatedByElements.add(relatedActivityDateElement)
        activeItem.instantiatedByElements.add(new NhsDDElement(name: "ATTENDANCE DATE", catalogueItemId: UUID.fromString("faff11f5-cbfb-4faa-84d8-6d3b1eccd03b"), branchId: branchId))
    }

    @Override
    void setupRetiredItem() {
        retiredItem = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("fb096f90-3273-4c66-8023-c1e32ac5b795"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))
    }

    @Override
    void setupPreparatoryItem() {
        preparatoryItem = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("f5276a0c-5458-4fa5-9bd3-ff786aef932f"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))
    }

    @Override
    void setupPreviousItems() {
        previousItemDescriptionChange = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The previous description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemAliasesChange = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("8049682f-761f-4eab-b533-c00781615207"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: [
                'aliasPlural': "ACTIVITIES DATE",
                'aliasAlsoKnownAs': 'ACTIVITY DATE STAMP',
            ])

        previousItemCodesChange = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("c95f7856-3d6c-4a23-b4b5-c4e5615ea2fc"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The current description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemCodesChange.codes.add(new NhsDDCode(code: "S", definition: "Standard", webOrder: 1))
        previousItemCodesChange.codes.add(new NhsDDCode(code: "X", definition: "Undefined", webOrder: 0))
        previousItemCodesChange.codes.add(new NhsDDCode(code: "U", definition: "Universal Time (UTC)", webOrder: 2))

        previousItemLinkedElementsChange = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("76a9cac0-19d2-4880-87e7-c6061c4edadd"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The current description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemLinkedElementsChange.instantiatedByElements.add(new NhsDDElement(name: "ATTENDANCE DATE", catalogueItemId: UUID.fromString("faff11f5-cbfb-4faa-84d8-6d3b1eccd03b"), branchId: branchId))
        previousItemLinkedElementsChange.instantiatedByElements.add(new NhsDDElement(name: "BABY FIRST FEED DATE", catalogueItemId: UUID.fromString("abf8bdcb-5659-4811-b888-51aaba8d15d7"), branchId: branchId))

        previousItemAllChange = new NhsDDAttribute(
            catalogueItemId: UUID.fromString("eeb8929f-819a-4cfe-9209-1e9867fa2b68"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: previousItemDescriptionChange.definition,
            otherProperties: copyMap(previousItemAliasesChange.otherProperties))
    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItem.DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 6
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof CodesSection
            (structure.sections[1] as CodesSection).title == "National Codes"
            structure.sections[2] instanceof AliasesSection
            structure.sections[3] instanceof WhereUsedSection
            structure.sections[4] instanceof ItemLinkListSection
            (structure.sections[4] as ItemLinkListSection).title == "Data Elements"
            structure.sections[5] instanceof ChangeLogSection
        }
    }

    void "should have the correct retired item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = retiredItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItem.DictionaryItemState.RETIRED
            structure.name == retiredItem.name
            structure.sections.size() == 2
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ChangeLogSection
        }
    }

    void "should have the correct preparatory item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = preparatoryItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItem.DictionaryItemState.PREPARATORY
            structure.name == preparatoryItem.name
            structure.sections.size() == 2
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ChangeLogSection
        }
    }

    void "should render an active item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title outputclass='attribute'>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>The date, month, year and century, or any combination of these elements, that is of relevance to an ACTIVITY.</shortdesc>
  <topic id='attribute_activity_date_description'>
    <title>Description</title>
    <body>
      <div>
        <p>The date, month, year and century, or any combination of these elements, that is of relevance to an 

          <xref outputclass='class' keyref='class_activity' scope='local'>ACTIVITY</xref>.
        </p>
      </div>
    </body>
  </topic>
  <topic id='attribute_activity_date_nationalCodes'>
    <title>National Codes</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 4*'>
        <sthead outputclass='thead-light'>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry>U</stentry>
          <stentry>Undefined</stentry>
        </strow>
        <strow>
          <stentry>S</stentry>
          <stentry>Standard</stentry>
        </strow>
        <strow>
          <stentry>B</stentry>
          <stentry>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='attribute_activity_date_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Attribute is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ACTIVITY DATES</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='attribute_activity_date_whereUsed'>
    <title>Where Used</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 3* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Type</stentry>
          <stentry>Link</stentry>
          <stentry>How used</stentry>
        </sthead>
        <strow>
          <stentry>Data Element</stentry>
          <stentry>
            <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
          </stentry>
          <stentry>references in description ACTIVITY DATE</stentry>
        </strow>
        <strow>
          <stentry>Class</stentry>
          <stentry>
            <xref outputclass='class' keyref='class_activity_date_time' format='html'>ACTIVITY DATE TIME</xref>
          </stentry>
          <stentry>references in description ACTIVITY DATE</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='attribute_activity_date_dataElements'>
    <title>Data Elements</title>
    <body>
      <ul>
        <li>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </body>
  </topic>
  <topic id='attribute_activity_date_changeLog'>
    <title>Change Log</title>
    <body>
      <div>
        <p>Click on the links below to view the change requests this item is part of:</p>
      </div>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='2* 5* 3*'>
        <sthead outputclass='thead-light'>
          <stentry>Change Request</stentry>
          <stentry>Change Request Description</stentry>
          <stentry>Implementation Date</stentry>
        </sthead>
        <strow>
          <stentry>
            <xref href='https://test.nhs.uk/change/cr1000' format='html' scope='external'>CR1000</xref>
          </stentry>
          <stentry>Change 1000</stentry>
          <stentry>01 April 2024</stentry>
        </strow>
        <strow>
          <stentry>
            <xref href='https://test.nhs.uk/change/cr2000' format='html' scope='external'>CR2000</xref>
          </stentry>
          <stentry>Change 2000</stentry>
          <stentry>01 September 2024</stentry>
        </strow>
      </simpletable>
      <div>
        <p>Click 

          <xref outputclass='- topic/xref xref' href='https://www.datadictionary.nhs.uk/archive' format='html' scope='external'>here</xref> to see the Change Log Information for changes before January 2025.
        </p>
      </div>
    </body>
  </topic>
</topic>"""
        }
    }

    void "should render a retired item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = retiredItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        System.err.println("Short Description: " + retiredItem.getShortDescription())
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date_retired'>
  <title outputclass='attribute retired'>
    <text>ACTIVITY DATE (Retired)</text>
  </title>
  <shortdesc>This item has been retired from the NHS Data Model and Dictionary.</shortdesc>
  <topic id='attribute_activity_date_retired_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item has been retired from the NHS Data Model and Dictionary.</p>
        <p>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</p>
        <p>Access to the last live version of this item can be obtained by emailing 

          <xref href='mailto:information.standards@nhs.net' format='html' scope='external'>information.standards@nhs.net</xref> with "NHS Data Model and Dictionary - Archive Request" in the email subject line.
        </p>
      </div>
    </body>
  </topic>
  <topic id='attribute_activity_date_retired_changeLog'>
    <title>Change Log</title>
    <body />
  </topic>
</topic>"""
        }
    }

    void "should render a preparatory item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = preparatoryItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title outputclass='attribute'>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>This item is being used for development purposes and has not yet been approved.</shortdesc>
  <topic id='attribute_activity_date_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item is being used for development purposes and has not yet been approved.</p>
      </div>
    </body>
  </topic>
  <topic id='attribute_activity_date_changeLog'>
    <title>Change Log</title>
    <body />
  </topic>
</topic>"""
        }
    }

    void "should render an active item to website html"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to html"
        String titleHtml = structure.generateHtml(websiteHtmlPublishContext)

        then: "the title is published"
        verifyAll {
            titleHtml
            titleHtml == """<h1 class="title topictitle1 attribute">ACTIVITY DATE</h1>
<div class="- topic/body body">
  <p class="- topic/shortdesc shortdesc">The date, month, year and century, or any combination of these elements, that is of relevance to an ACTIVITY.</p>
</div>"""
        }

        when: "the description is converted to html"
        String descriptionHtml = structure.sections.find { it instanceof DescriptionSection }.generateHtml(websiteHtmlPublishContext)

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """<div class="- topic/body body">
  <div class="- topic/div div">
    <p class="- topic/p p"><p>The date, month, year and century, or any combination of these elements, that is of relevance to an 
<a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/79ab1e21-4407-4aae-b777-7b7920fa1963">ACTIVITY</a>.</p></p>
  </div>
</div>"""
        }

        when: "the codes are converted to html"
        String codesHtml = structure.sections.find { it instanceof CodesSection }.generateHtml(websiteHtmlPublishContext)

        then: "the codes are published"
        verifyAll {
            codesHtml
            codesHtml == """<div class="- topic/body body">
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 80%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Code</th>
          <th class="- topic/stentry stentry">Description</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">U</td>
          <td class="- topic/stentry stentry">Undefined</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">S</td>
          <td class="- topic/stentry stentry">Standard</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">B</td>
          <td class="- topic/stentry stentry">British Summer Time - see <a href="https://time.com">link</a></td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the linked elements are converted to html"
        String linkedElementsHtml = structure.sections.find { it instanceof ItemLinkListSection }.generateHtml(websiteHtmlPublishContext)

        then: "the linked elements are published"
        verifyAll {
            linkedElementsHtml
            linkedElementsHtml == """<div class="- topic/body body">
  <div>
    <ul class="- topic/ul ul">
      <li class="- topic/li li">
        <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
      </li>
      <li class="- topic/li li">
        <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
      </li>
    </ul>
  </div>
</div>"""
        }

        when: "the aliases are converted to html"
        String aliasesHtml = structure.sections.find { it instanceof AliasesSection }.generateHtml(websiteHtmlPublishContext)

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """<div class="- topic/body body">
  <p class="- topic/p p">This Attribute is also known by these names:</p>
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 34%" />
        <col style="width: 66%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Context</th>
          <th class="- topic/stentry stentry">Alias</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Plural</td>
          <td class="- topic/stentry stentry">ACTIVITY DATES</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the where used are converted to html"
        String whereUsedHtml = structure.sections.find { it instanceof WhereUsedSection }.generateHtml(websiteHtmlPublishContext)

        then: "the where used are published"
        verifyAll {
            whereUsedHtml
            whereUsedHtml == """<div class="- topic/body body">
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 15%" />
        <col style="width: 45%" />
        <col style="width: 40%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Type</th>
          <th class="- topic/stentry stentry">Link</th>
          <th class="- topic/stentry stentry">How used</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Data Element</td>
          <td class="- topic/stentry stentry">
            <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
          </td>
          <td class="- topic/stentry stentry">references in description ACTIVITY DATE</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Class</td>
          <td class="- topic/stentry stentry">
            <a class="class" title="ACTIVITY DATE TIME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/542a6963-cce2-4c8f-b60d-b86b13d43bbe">ACTIVITY DATE TIME</a>
          </td>
          <td class="- topic/stentry stentry">references in description ACTIVITY DATE</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the change log is converted to html"
        String changeLogHtml = structure.sections.find { it instanceof ChangeLogSection }.generateHtml(websiteHtmlPublishContext)

        then: "the change log is published"
        verifyAll {
            changeLogHtml
            changeLogHtml == """<div class="- topic/body body">
  <div class="- topic/body body"><p>Click on the links below to view the change requests this item is part of:</p></div>
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 55%" />
        <col style="width: 25%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Change Request</th>
          <th class="- topic/stentry stentry">Change Request Description</th>
          <th class="- topic/stentry stentry">Implementation Date</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">
            <a href="https://test.nhs.uk/change/cr1000">CR1000</a>
          </td>
          <td class="- topic/stentry stentry">Change 1000</td>
          <td class="- topic/stentry stentry">01 April 2024</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">
            <a href="https://test.nhs.uk/change/cr2000">CR2000</a>
          </td>
          <td class="- topic/stentry stentry">Change 2000</td>
          <td class="- topic/stentry stentry">01 September 2024</td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="- topic/body body"><p>Click <a class="- topic/xref xref" href="https://www.datadictionary.nhs.uk/archive" target="_blank" rel="external noopener">here</a> to see the Change Log Information for changes before January 2025.</p></div>
</div>"""
        }
    }

    void "should produce no diff when items are the same"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = activeItem.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "no diff is returned"
        verifyAll {
            !diff
        }
    }

    void "should produce a diff for a new item to change paper dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        when: "a diff is produced against no previous item"
        DictionaryItem diff = structure.produceDiff(null)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: New</shortdesc>
  <body>
    <div outputclass='new'>
      <div>
        <p>The date, month, year and century, or any combination of these elements, that is of relevance to an 

          <xref outputclass='class' keyref='class_activity' scope='local'>ACTIVITY</xref>.
        </p>
      </div>
    </div>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>S</stentry>
          <stentry outputclass='new'>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Attribute is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITY DATES</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for a new item to change paper html"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        when: "a diff is produced against no previous item"
        DictionaryItem diff = structure.produceDiff(null)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: New</h4>
  <div>
    <div class="new">
      <p><p>The date, month, year and century, or any combination of these elements, that is of relevance to an 
<a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/79ab1e21-4407-4aae-b777-7b7920fa1963">ACTIVITY</a>.</p></p>
    </div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td class="new">S</td>
            <td class="new">Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Attribute is also known by these names:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 34%" />
          <col style="width: 66%" />
        </colgroup>
        <thead>
          <tr>
            <th>Context</th>
            <th>Alias</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Plural</td>
            <td class="new">ACTIVITY DATES</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li class="new">
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item description to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemDescriptionChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: Updated description, National Codes, Changed elements</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>S</stentry>
          <stentry outputclass='new'>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item aliases to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAliasesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: National Codes, Aliases, Changed elements</shortdesc>
  <body>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>S</stentry>
          <stentry outputclass='new'>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Attribute is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITY DATES</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>ACTIVITY DATE STAMP</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>ACTIVITIES DATE</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for all changes to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: Updated description, National Codes, Aliases, Changed elements</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>S</stentry>
          <stentry outputclass='new'>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Attribute is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITY DATES</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>ACTIVITY DATE STAMP</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>ACTIVITIES DATE</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for a retired item to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = retiredItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date_retired'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: Retired</shortdesc>
  <body>
    <div>
      <div>
        <ph id='removed-diff-0' outputclass='diff-html-removed'>The previous description</ph>
        <p>
          <ph id='added-diff-0' outputclass='diff-html-added'>This item has been retired from the NHS Data Model and Dictionary.</ph>
        </p>
        <p>
          <ph outputclass='diff-html-added'>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</ph>
        </p>
        <p>
          <ph outputclass='diff-html-added'>Access to the last live version of this item can be obtained by emailing</ph>
          <xref href='mailto:information.standards@nhs.net' format='html' scope='external'>
            <ph outputclass='diff-html-added'>information.standards@nhs.net</ph>
          </xref>
          <ph outputclass='diff-html-added'>with "NHS Data Model and Dictionary - Archive Request" in the email subject line.</ph>
        </p>
      </div>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item description to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"

        DictionaryItem previousStructure = previousItemDescriptionChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: Updated description, National Codes, Changed elements</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td class="new">S</td>
            <td class="new">Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li class="new">
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item aliases to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAliasesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: National Codes, Aliases, Changed elements</h4>
  <div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td class="new">S</td>
            <td class="new">Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Attribute is also known by these names:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 34%" />
          <col style="width: 66%" />
        </colgroup>
        <thead>
          <tr>
            <th>Context</th>
            <th>Alias</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Plural</td>
            <td class="new">ACTIVITY DATES</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">ACTIVITY DATE STAMP</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">ACTIVITIES DATE</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li class="new">
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for all changes to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"

        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: Updated description, National Codes, Aliases, Changed elements</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td class="new">S</td>
            <td class="new">Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Attribute is also known by these names:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 34%" />
          <col style="width: 66%" />
        </colgroup>
        <thead>
          <tr>
            <th>Context</th>
            <th>Alias</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Plural</td>
            <td class="new">ACTIVITY DATES</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">ACTIVITY DATE STAMP</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">ACTIVITIES DATE</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li class="new">
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item codes to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemCodesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: National Codes, Changed elements</shortdesc>
  <body>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry>S</stentry>
          <stentry>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>X</stentry>
          <stentry outputclass='deleted'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>U</stentry>
          <stentry outputclass='deleted'>Universal Time (UTC)</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item codes to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemCodesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: National Codes, Changed elements</h4>
  <div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td>S</td>
            <td>Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
          <tr>
            <td class="deleted">X</td>
            <td class="deleted">Undefined</td>
          </tr>
          <tr>
            <td class="deleted">U</td>
            <td class="deleted">Universal Time (UTC)</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li class="new">
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item linked elements to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemLinkedElementsChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='attribute_activity_date'>
  <title>
    <text>ACTIVITY DATE</text>
  </title>
  <shortdesc>Change to Attribute: National Codes, Changed elements</shortdesc>
  <body>
    <div>
      <p>
        <b>National Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>U</stentry>
          <stentry outputclass='new'>Undefined</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>S</stentry>
          <stentry outputclass='new'>Standard</stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>B</stentry>
          <stentry outputclass='new'>
            <div>British Summer Time - see 

              <xref href='https://time.com' format='html' scope='external'>link</xref>
            </div>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='element' keyref='data_element_activity_date_critical_care' format='html'>ACTIVITY DATE (CRITICAL CARE)</xref>
        </li>
        <li>
          <xref outputclass='element' keyref='data_element_attendance_date' format='html'>ATTENDANCE DATE</xref>
        </li>
        <li outputclass='deleted'>
          <xref outputclass='element' keyref='data_element_baby_first_feed_date' format='html'>BABY FIRST FEED DATE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item linked elements to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemLinkedElementsChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>ACTIVITY DATE</h3>
  <h4>Change to Attribute: National Codes, Changed elements</h4>
  <div>
    <p>
      <b>National Codes</b>
    </p>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">U</td>
            <td class="new">Undefined</td>
          </tr>
          <tr>
            <td class="new">S</td>
            <td class="new">Standard</td>
          </tr>
          <tr>
            <td class="new">B</td>
            <td class="new">British Summer Time - see <a href="https://time.com">link</a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Data Elements</b>
      </p>
      <ul>
        <li class="new">
          <a class="element" title="ACTIVITY DATE (CRITICAL CARE)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY DATE (CRITICAL CARE)</a>
        </li>
        <li>
          <a class="element" title="ATTENDANCE DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ATTENDANCE DATE</a>
        </li>
        <li class="deleted">
          <a class="element" title="BABY FIRST FEED DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/abf8bdcb-5659-4811-b888-51aaba8d15d7">BABY FIRST FEED DATE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }
}
