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

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.dita.elements.langref.base.Topic
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDCode
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.publish.structure.AliasesSection
import uk.nhs.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.datadictionary.publish.structure.CodesSection
import uk.nhs.datadictionary.publish.structure.DescriptionSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.FormatLengthSection
import uk.nhs.datadictionary.publish.structure.ItemLinkListSection
import uk.nhs.datadictionary.publish.structure.WhereUsedSection
import uk.nhs.datadictionary.services.NhsDataDictionaryService

@MicronautTest
class NhsElementStructureSpec extends DataDictionaryComponentStructureSpec<NhsDDElement> {
    @Inject
    NhsDataDictionaryService dataDictionaryService


    NhsDDClass relatedPatientClass
    NhsDDBusinessDefinition relatedRadiofrequncyAblationDef

    NhsDDElement previousItemCodesChange
    NhsDDElement previousItemLinkedAttributesChange
    NhsDDElement previousItemFormatChange

    @Override
    protected NhsDataDictionary createDataDictionary() {
        dataDictionaryService.newDataDictionary()
    }

    @Override
    String getDefinition() {
        """<p>The type of Ablative Therapy given to a <a href="dm:Classes and Attributes|dc:PATIENT">PATIENT</a> 
during a Liver Cancer Care Spell.</p>"""
    }

    @Override
    void setupRelatedItems() {
        relatedPatientClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6"),
            branchId: branchId,
            name: "PATIENT")

        relatedRadiofrequncyAblationDef = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("685a7609-a5ba-4112-bbd3-d8d5df0cdf4f"),
            branchId: branchId,
            name: "Radiofrequency Ablation")
    }

    @Override
    protected void setupComponentPathResolver() {
        super.setupComponentPathResolver()

        componentPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass)
        componentPathResolver.add(relatedRadiofrequncyAblationDef.getMauroPath(), relatedRadiofrequncyAblationDef)
    }

    @Override
    protected void setupCatalogueItemPathResolver() {
        super.setupCatalogueItemPathResolver()

        catalogueItemPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass.catalogueItemId)
        catalogueItemPathResolver.add(relatedRadiofrequncyAblationDef.getMauroPath(), relatedRadiofrequncyAblationDef.catalogueItemId)
    }

    @Override
    void setupActiveItem() {
        activeItem = new NhsDDElement(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: "ABLATIVE THERAPY TYPE",
            definition: definition,
            otherProperties: [
                'formatLength': 'an1',
                'aliasPlural': 'ABLATIVE THERAPY TYPES'
            ])

        addWhereUsed(activeItem, activeItem)    // Self reference
        addWhereUsed(
            activeItem,
            new NhsDDDataSet(name: "Cancer Outcomes and Services Data Set - Liver", catalogueItemId: UUID.fromString("542a6963-cce2-4c8f-b60d-b86b13d43bbe"), branchId: branchId))

        addChangeLog(
            activeItem,
            new NhsDDChangeLog(reference: "CR1000", referenceUrl: "https://test.nhs.uk/change/cr1000", description: "Change 1000", implementationDate: "01 April 2024"),
            new NhsDDChangeLog(reference: "CR2000", referenceUrl: "https://test.nhs.uk/change/cr2000", description: "Change 2000", implementationDate: "01 September 2024"))

        activeItem.codes.add(
            new NhsDDCode(
                webOrder: 0,
                isDefault: false,
                code: "R",
                definition: "Radiofrequency Ablation",
                webPresentation: """<a href="te:NHS Business Definitions|tm:Radiofrequency Ablation">Radiofrequency Ablation</a>"""))

        activeItem.codes.add(
            new NhsDDCode(
                webOrder: 1,
                isDefault: false,
                code: "M",
                definition: "Microwave Ablation"))

        activeItem.codes.add(
            new NhsDDCode(
                webOrder: 2,
                isDefault: true,
                code: "9",
                definition: "Not Known"))

        activeItem.instantiatesAttributes.add(
            new NhsDDAttribute(
                name: "ABLATIVE THERAPY TYPE",
                catalogueItemId: UUID.fromString("faff11f5-cbfb-4faa-84d8-6d3b1eccd03b"),
                branchId: branchId))
    }

    @Override
    void setupRetiredItem() {
        retiredItem = new NhsDDElement(
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
        preparatoryItem = new NhsDDElement(
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
        previousItemDescriptionChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The previous description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemAliasesChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("8049682f-761f-4eab-b533-c00781615207"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: [
                'aliasPlural': "ACTIVITIES DATE",
                'aliasAlsoKnownAs': 'ACTIVITY DATE STAMP',
            ])

        previousItemCodesChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("c95f7856-3d6c-4a23-b4b5-c4e5615ea2fc"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The current description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemCodesChange.codes.add(
            new NhsDDCode(
                webOrder: 0,
                isDefault: false,
                code: "R",
                definition: "Radiofrequency Ablation",
                webPresentation: """<a href="te:NHS Business Definitions|tm:Radiofrequency Ablation">Radiofrequency Ablation</a>"""))

        previousItemCodesChange.codes.add(
            new NhsDDCode(
                webOrder: 1,
                isDefault: false,
                code: "M",
                definition: "Microwave Ablation Type"))

        previousItemCodesChange.codes.add(
            new NhsDDCode(
                webOrder: 2,
                isDefault: true,
                code: "9",
                definition: "Not Known"))

        previousItemCodesChange.codes.add(
            new NhsDDCode(
                webOrder: 3,
                isDefault: true,
                code: "X",
                definition: "Undefined"))

        previousItemLinkedAttributesChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("76a9cac0-19d2-4880-87e7-c6061c4edadd"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The current description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemLinkedAttributesChange.instantiatesAttributes.add(
            new NhsDDAttribute(
                name: "ABLATIVE THERAPY TYPE",
                catalogueItemId: UUID.fromString("faff11f5-cbfb-4faa-84d8-6d3b1eccd03b"),
                branchId: branchId))

        previousItemLinkedAttributesChange.instantiatesAttributes.add(
            new NhsDDAttribute(
                name: "BINET STAGE",
                catalogueItemId: UUID.fromString("94f02a26-1db9-45f2-9dfa-9a588c961b13"),
                branchId: branchId))

        previousItemAllChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("eeb8929f-819a-4cfe-9209-1e9867fa2b68"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: previousItemDescriptionChange.definition,
            otherProperties: copyMap(previousItemAliasesChange.otherProperties))

        previousItemFormatChange = new NhsDDElement(
            catalogueItemId: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The current description",
            otherProperties: copyMap(this.activeItem.otherProperties))
        previousItemFormatChange.otherProperties['formatLength'] = "an10 CCYY-MM-DD"
    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItem.DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 8
            structure.sections[0] instanceof FormatLengthSection
            structure.sections[1] instanceof DescriptionSection
            structure.sections[2] instanceof CodesSection
            (structure.sections[2] as CodesSection).title == "National Codes"
            structure.sections[3] instanceof CodesSection
            (structure.sections[3] as CodesSection).title == "Default Codes"
            structure.sections[4] instanceof AliasesSection
            structure.sections[5] instanceof WhereUsedSection
            structure.sections[6] instanceof ItemLinkListSection
            (structure.sections[6] as ItemLinkListSection).title == "Attribute"
            structure.sections[7] instanceof ChangeLogSection
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title outputclass='element'>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>The type of Ablative Therapy given to a PATIENT during a Liver Cancer Care Spell.</shortdesc>
  <topic id='data_element_ablative_therapy_type_formatLength'>
    <title>Format / Length</title>
    <body>
      <p>an1</p>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_description'>
    <title>Description</title>
    <body>
      <div>
        <p>The type of Ablative Therapy given to a 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENT</xref> during a Liver Cancer Care Spell.
        </p>
      </div>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_nationalCodes'>
    <title>National Codes</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 4*'>
        <sthead outputclass='thead-light'>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry>R</stentry>
          <stentry>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry>M</stentry>
          <stentry>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_defaultCodes'>
    <title>Default Codes</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 4*'>
        <sthead outputclass='thead-light'>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry>9</stentry>
          <stentry>Not Known</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Data Element is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ABLATIVE THERAPY TYPES</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_whereUsed'>
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
            <xref outputclass='element' keyref='data_element_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
          </stentry>
          <stentry>references in description ABLATIVE THERAPY TYPE</stentry>
        </strow>
        <strow>
          <stentry>Data Set</stentry>
          <stentry>
            <xref outputclass='dataSet' keyref='data_set_cancer_outcomes_and_services_data_set_-_liver' format='html'>Cancer Outcomes and Services Data Set - Liver</xref>
          </stentry>
          <stentry>references in description ABLATIVE THERAPY TYPE</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_attributes'>
    <title>Attribute</title>
    <body>
      <ul>
        <li>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
        </li>
      </ul>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_changeLog'>
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
        verifyAll {
            ditaXml == """<topic id='data_element_ablative_therapy_type_retired'>
  <title outputclass='element retired'>
    <text>ABLATIVE THERAPY TYPE (Retired)</text>
  </title>
  <shortdesc>This item has been retired from the NHS Data Model and Dictionary.</shortdesc>
  <topic id='data_element_ablative_therapy_type_retired_description'>
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
  <topic id='data_element_ablative_therapy_type_retired_changeLog'>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title outputclass='element'>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>This item is being used for development purposes and has not yet been approved.</shortdesc>
  <topic id='data_element_ablative_therapy_type_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item is being used for development purposes and has not yet been approved.</p>
      </div>
    </body>
  </topic>
  <topic id='data_element_ablative_therapy_type_changeLog'>
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
            titleHtml == """<h1 class="title topictitle1 element">ABLATIVE THERAPY TYPE</h1>
<div class="- topic/body body">
  <p class="- topic/shortdesc shortdesc">The type of Ablative Therapy given to a PATIENT during a Liver Cancer Care Spell.</p>
</div>"""
        }

        when: "the description is converted to html"
        String descriptionHtml = structure.sections.find { it instanceof DescriptionSection }.generateHtml(websiteHtmlPublishContext)

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """<div class="- topic/body body">
  <div class="- topic/div div">
    <p class="- topic/p p"><p>The type of Ablative Therapy given to a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENT</a> 
during a Liver Cancer Care Spell.</p></p>
  </div>
</div>"""
        }

        when: "the format length is converted to html"
        String formatLengthHtml = structure.sections.find { it instanceof FormatLengthSection }.generateHtml(websiteHtmlPublishContext)

        then: "the format length is published"
        verifyAll {
            formatLengthHtml
            formatLengthHtml == """<div class="- topic/body body">
  <div class="- topic/div div">
    <p class="- topic/p p">an1</p>
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
          <td class="- topic/stentry stentry">R</td>
          <td class="- topic/stentry stentry"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">M</td>
          <td class="- topic/stentry stentry">Microwave Ablation</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the linked attributes are converted to html"
        String linkedElementsHtml = structure.sections.find { it instanceof ItemLinkListSection }.generateHtml(websiteHtmlPublishContext)

        then: "the linked attributes are published"
        verifyAll {
            linkedElementsHtml
            linkedElementsHtml == """<div class="- topic/body body">
  <div>
    <ul class="- topic/ul ul">
      <li class="- topic/li li">
        <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
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
  <p class="- topic/p p">This Data Element is also known by these names:</p>
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
          <td class="- topic/stentry stentry">ABLATIVE THERAPY TYPES</td>
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
            <a class="element" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/901c2d3d-0111-41d1-acc9-5b501c1dc397">ABLATIVE THERAPY TYPE</a>
          </td>
          <td class="- topic/stentry stentry">references in description ABLATIVE THERAPY TYPE</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Data Set</td>
          <td class="- topic/stentry stentry">
            <a class="dataSet" title="Cancer Outcomes and Services Data Set - Liver" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/dataSet/542a6963-cce2-4c8f-b60d-b86b13d43bbe">Cancer Outcomes and Services Data Set - Liver</a>
          </td>
          <td class="- topic/stentry stentry">references in description ABLATIVE THERAPY TYPE</td>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: New</shortdesc>
  <body>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p outputclass='new'>an1</p>
    </div>
    <div outputclass='new'>
      <div>
        <p>The type of Ablative Therapy given to a 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENT</xref> during a Liver Cancer Care Spell.
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Data Element is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ABLATIVE THERAPY TYPES</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: Updated description, National Codes, Default Codes, Changed attributes</shortdesc>
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: Format / Length, National Codes, Default Codes, Aliases, Changed attributes</shortdesc>
  <body>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p outputclass='new'>an1</p>
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Data Element is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ABLATIVE THERAPY TYPES</stentry>
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
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: Format / Length, Updated description, National Codes, Default Codes, Aliases, Changed attributes</shortdesc>
  <body>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p outputclass='new'>an1</p>
    </div>
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Data Element is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ABLATIVE THERAPY TYPES</stentry>
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
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type_retired'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: Retired</shortdesc>
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: New</h4>
  <div>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p class="new">an1</p>
    </div>
    <div class="new">
      <p><p>The type of Ablative Therapy given to a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENT</a> 
during a Liver Cancer Care Spell.</p></p>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Data Element is also known by these names:</p>
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
            <td class="new">ABLATIVE THERAPY TYPES</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: Updated description, National Codes, Default Codes, Changed attributes</h4>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: Format / Length, National Codes, Default Codes, Aliases, Changed attributes</h4>
  <div>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p class="new">an1</p>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Data Element is also known by these names:</p>
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
            <td class="new">ABLATIVE THERAPY TYPES</td>
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
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: Format / Length, Updated description, National Codes, Default Codes, Aliases, Changed attributes</h4>
  <div>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p class="new">an1</p>
    </div>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Data Element is also known by these names:</p>
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
            <td class="new">ABLATIVE THERAPY TYPES</td>
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
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: National Codes, Default Codes, Changed attributes</shortdesc>
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
          <stentry>R</stentry>
          <stentry>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>M</stentry>
          <stentry outputclass='deleted'>Microwave Ablation Type</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry>9</stentry>
          <stentry>Not Known</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>X</stentry>
          <stentry outputclass='deleted'>Undefined</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: National Codes, Default Codes, Changed attributes</h4>
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
            <td>R</td>
            <td><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
          <tr>
            <td class="deleted">M</td>
            <td class="deleted">Microwave Ablation Type</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td>9</td>
            <td>Not Known</td>
          </tr>
          <tr>
            <td class="deleted">X</td>
            <td class="deleted">Undefined</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
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
        DictionaryItem previousStructure = previousItemLinkedAttributesChange.getPublishStructure()
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: National Codes, Default Codes, Changed attributes</shortdesc>
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Attributes</b>
      </p>
      <ul>
        <li>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
        </li>
        <li outputclass='deleted'>
          <xref outputclass='attribute' keyref='attribute_binet_stage' format='html'>BINET STAGE</xref>
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
        DictionaryItem previousStructure = previousItemLinkedAttributesChange.getPublishStructure()
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: National Codes, Default Codes, Changed attributes</h4>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Attributes</b>
      </p>
      <ul>
        <li>
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
        </li>
        <li class="deleted">
          <a class="attribute" title="BINET STAGE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/94f02a26-1db9-45f2-9dfa-9a588c961b13">BINET STAGE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item format length to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemFormatChange.getPublishStructure()
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
            ditaXml == """<topic id='data_element_ablative_therapy_type'>
  <title>
    <text>ABLATIVE THERAPY TYPE</text>
  </title>
  <shortdesc>Change to Data Element: Format / Length, National Codes, Default Codes, Changed attributes</shortdesc>
  <body>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p outputclass='new'>an1</p>
      <p outputclass='deleted'>an10 CCYY-MM-DD</p>
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
          <stentry outputclass='new'>R</stentry>
          <stentry outputclass='new'>
            <div>
              <xref outputclass='businessDefinition' keyref='nhs_business_definition_radiofrequency_ablation' scope='local'>Radiofrequency Ablation</xref>
            </div>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new'>M</stentry>
          <stentry outputclass='new'>Microwave Ablation</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Default Codes</b>
      </p>
      <simpletable relcolwidth='1* 4*'>
        <sthead>
          <stentry>Code</stentry>
          <stentry>Description</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>9</stentry>
          <stentry outputclass='new'>Not Known</stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li outputclass='new'>
          <xref outputclass='attribute' keyref='attribute_ablative_therapy_type' format='html'>ABLATIVE THERAPY TYPE</xref>
        </li>
      </ul>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item format length to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemFormatChange.getPublishStructure()
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
  <h3>ABLATIVE THERAPY TYPE</h3>
  <h4>Change to Data Element: Format / Length, National Codes, Default Codes, Changed attributes</h4>
  <div>
    <div>
      <p>
        <b>Format / Length</b>
      </p>
      <p class="new">an1</p>
      <p class="deleted">an10 CCYY-MM-DD</p>
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
            <td class="new">R</td>
            <td class="new"><a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/685a7609-a5ba-4112-bbd3-d8d5df0cdf4f">Radiofrequency Ablation</a></td>
          </tr>
          <tr>
            <td class="new">M</td>
            <td class="new">Microwave Ablation</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>
      <b>Default Codes</b>
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
            <td class="new">9</td>
            <td class="new">Not Known</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <p>
        <b>Attribute</b>
      </p>
      <ul>
        <li class="new">
          <a class="attribute" title="ABLATIVE THERAPY TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/faff11f5-cbfb-4faa-84d8-6d3b1eccd03b">ABLATIVE THERAPY TYPE</a>
        </li>
      </ul>
    </div>
  </div>
</div>"""
        }
    }
}
