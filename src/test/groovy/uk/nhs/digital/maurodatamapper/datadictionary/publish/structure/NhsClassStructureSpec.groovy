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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDClassRelationship
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.publish.structure.AliasesSection
import uk.nhs.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.datadictionary.publish.structure.ClassAttributeSection
import uk.nhs.datadictionary.publish.structure.ClassRelationshipSection
import uk.nhs.datadictionary.publish.structure.DescriptionSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.WhereUsedSection
import uk.nhs.datadictionary.services.NhsDataDictionaryService


@MicronautTest(startApplication = true, environments = ['secured'])
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
class NhsClassStructureSpec extends DataDictionaryComponentStructureSpec<NhsDDClass> {

    @Inject
    NhsDataDictionaryService dataDictionaryService

    NhsDDClass relatedServiceClass
    NhsDDClass relatedPatientClass
    NhsDDClass relatedCareProfessionalClass

    @Override
    String getDefinition() {
        """<p>A provision of <a href="dm:Classes and Attributes|dc:SERVICE">SERVICES</a> 
to a <a href="dm:Classes and Attributes|dc:PATIENT">PATIENT</a> by one or more 
<a href="dm:Classes and Attributes|dc:CARE PROFESSIONAL">CARE PROFESSIONALS</a>.</p>"""
    }

    @Override
    void setupRelatedItems() {
        relatedServiceClass = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("79ab1e21-4407-4aae-b777-7b7920fa1963"),
                label: "SERVICE"),
            branchId)

        relatedPatientClass = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6"),
                label: "PATIENT"),
            branchId)

        relatedCareProfessionalClass = new NhsDDClass(
            new DataClass(
                id : UUID.fromString("9ac6a0b2-b4bf-48af-ad4d-0ecfe268df57"),
                label: "CARE PROFESSIONAL"),
            branchId)
    }

    @Override
    protected void setupComponentPathResolver() {
        super.setupComponentPathResolver()

        componentPathResolver.add(relatedServiceClass.getMauroPath(), relatedServiceClass)
        componentPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass)
        componentPathResolver.add(relatedCareProfessionalClass.getMauroPath(), relatedCareProfessionalClass)
    }

    @Override
    protected void setupCatalogueItemPathResolver() {
        super.setupCatalogueItemPathResolver()

        catalogueItemPathResolver.add(relatedServiceClass.getMauroPath(), relatedServiceClass.catalogueItem.id)
        catalogueItemPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass.catalogueItem.id)
        catalogueItemPathResolver.add(relatedCareProfessionalClass.getMauroPath(), relatedCareProfessionalClass.catalogueItemId)
    }

    @Override
    void setupActiveItem() {
        activeItem = new NhsDDClass(
            new DataClass(
            id: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            label: "ACTIVITY",
            description: definition),
        branchId)
        activeItem.dataDictionary = dataDictionary
        activeItem.addOtherProperties(['aliasPlural': 'ACTIVITIES'])

        addWhereUsed(activeItem, activeItem)    // Self reference
        addWhereUsed(
            activeItem,
            new NhsDDAttribute(
                new DataElement(
                    label: "ACTIVITY COUNT",
                    id: UUID.fromString("b5170409-97aa-464e-9aad-657c8b2e00f8")),
            branchId))

        addWhereUsed(
            activeItem,
            new NhsDDElement(
                new DataElement(
                    label: "ACTIVITY COUNT (POINT OF DELIVERY)",
                    id: UUID.fromString("542a6963-cce2-4c8f-b60d-b86b13d43bbe")),
            branchId))

        addChangeLog(
            activeItem,
            new NhsDDChangeLog(reference: "CR1000", referenceUrl: "https://test.nhs.uk/change/cr1000", description: "Change 1000", implementationDate: "01 April 2024"),
            new NhsDDChangeLog(reference: "CR2000", referenceUrl: "https://test.nhs.uk/change/cr2000", description: "Change 2000", implementationDate: "01 September 2024"))


        NhsDDAttribute attribute1 = new NhsDDAttribute(
            new DataElement(label: "ACTIVITY IDENTIFIER"))
        attribute1.addOtherProperties(["isKey": "true"])
        activeItem.keyAttributes.add(attribute1)

        activeItem.otherAttributes.add(
            new NhsDDAttribute(
                new DataElement(label: "ACTIVITY COUNT")))

        activeItem.otherAttributes.add(
            new NhsDDAttribute(
                new DataElement(label: "ACTIVITY DURATION")))

        activeItem.classRelationships.add(new NhsDDClassRelationship(key: true, relationshipDescription: "supplied by", targetClass: new NhsDDClass(new DataClass(label: "ORGANISATION"))))
        activeItem.classRelationships.add(new NhsDDClassRelationship(key: false, relationshipDescription: "located at", targetClass: new NhsDDClass(new DataClass(label: "ADDRESS"))))
    }

    @Override
    void setupRetiredItem() {
        retiredItem = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("fb096f90-3273-4c66-8023-c1e32ac5b795"),
                label: this.activeItem.name,
                description: this.activeItem.description),
            branchId)
        retiredItem.dataDictionary = dataDictionary
        retiredItem.addOtherProperties(this.activeItem.otherProperties)
        retiredItem.whereUsed = copyMap(this.activeItem.whereUsed)
    }

    @Override
    void setupPreparatoryItem() {
        preparatoryItem = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("f5276a0c-5458-4fa5-9bd3-ff786aef932f"),
                label: this.activeItem.name,
                description: this.activeItem.description),
            branchId)
        preparatoryItem.dataDictionary = dataDictionary
        preparatoryItem.addOtherProperties(this.activeItem.otherProperties)
        preparatoryItem.whereUsed = copyMap(this.activeItem.whereUsed)
    }

    @Override
    void setupPreviousItems() {
        previousItemDescriptionChange = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
                label: this.activeItem.name,
                description: 'The previous description'),
            branchId)
        previousItemDescriptionChange.dataDictionary = dataDictionary
        previousItemDescriptionChange.addOtherProperties(this.activeItem.otherProperties)

        previousItemAliasesChange = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("8049682f-761f-4eab-b533-c00781615207"),
                label: this.activeItem.name,
                description: this.activeItem.description),
            branchId)
        previousItemAliasesChange.dataDictionary = dataDictionary
        previousItemAliasesChange.addOtherProperties([
                'aliasPlural': "ACTIVITY GROUP",
                'aliasAlsoKnownAs': 'ALTERNATIVE',
            ])

        previousItemAllChange = new NhsDDClass(
            new DataClass(
                id: UUID.fromString("eeb8929f-819a-4cfe-9209-1e9867fa2b68"),
                label: this.activeItem.name,
                description: previousItemDescriptionChange.description),
            branchId)
        previousItemAllChange.dataDictionary = dataDictionary
        previousItemAllChange.addOtherProperties(previousItemAliasesChange.otherProperties)
    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItem.DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 5
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ClassAttributeSection
            structure.sections[2] instanceof ClassRelationshipSection
            structure.sections[3] instanceof AliasesSection
            structure.sections[4] instanceof WhereUsedSection
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
            structure.sections[1] instanceof AliasesSection
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
            structure.sections[1] instanceof AliasesSection
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
            ditaXml == """<topic id='class_activity'>
  <title outputclass='class'>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>A provision of SERVICES to a PATIENT by one or more CARE PROFESSIONALS.</shortdesc>
  <topic id='class_activity_description'>
    <title>Description</title>
    <body>
      <div>
        <p>A provision of 

          <xref outputclass='class' keyref='class_service' scope='local'>SERVICES</xref> to a 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENT</xref> by one or more 

          <xref outputclass='class' keyref='class_care_professional' scope='local'>CARE PROFESSIONALS</xref>.
        </p>
      </div>
    </body>
  </topic>
  <topic id='class_activity_attributes'>
    <title>Attributes</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 9*'>
        <sthead outputclass='thead-light'>
          <stentry>Key</stentry>
          <stentry>Attribute Name</stentry>
        </sthead>
        <strow>
          <stentry>Key</stentry>
          <stentry>
            <xref outputclass='attribute' keyref='attribute_activity_identifier' format='html'>ACTIVITY IDENTIFIER</xref>
          </stentry>
        </strow>
        <strow>
          <stentry />
          <stentry>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
        </strow>
        <strow>
          <stentry />
          <stentry>
            <xref outputclass='attribute' keyref='attribute_activity_duration' format='html'>ACTIVITY DURATION</xref>
          </stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='class_activity_relationships'>
    <title>Relationships</title>
    <body>
      <p>Each ACTIVITY:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 5* 5*'>
        <sthead outputclass='thead-light'>
          <stentry>Key</stentry>
          <stentry>Relationship</stentry>
          <stentry>Class</stentry>
        </sthead>
        <strow>
          <stentry>Key</stentry>
          <stentry>supplied by</stentry>
          <stentry>
            <xref outputclass='class' keyref='class_organisation' format='html'>ORGANISATION</xref>
          </stentry>
        </strow>
        <strow>
          <stentry />
          <stentry>located at</stentry>
          <stentry>
            <xref outputclass='class' keyref='class_address' format='html'>ADDRESS</xref>
          </stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='class_activity_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Class is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ACTIVITIES</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='class_activity_whereUsed'>
    <title>Where Used</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 3* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Type</stentry>
          <stentry>Link</stentry>
          <stentry>How used</stentry>
        </sthead>
        <strow>
          <stentry>Class</stentry>
          <stentry>
            <xref outputclass='class' keyref='class_activity' format='html'>ACTIVITY</xref>
          </stentry>
          <stentry>references in description ACTIVITY</stentry>
        </strow>
        <strow>
          <stentry>Attribute</stentry>
          <stentry>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
          <stentry>references in description ACTIVITY</stentry>
        </strow>
        <strow>
          <stentry>Data Element</stentry>
          <stentry>
            <xref outputclass='element' keyref='data_element_activity_count_point_of_delivery' format='html'>ACTIVITY COUNT (POINT OF DELIVERY)</xref>
          </stentry>
          <stentry>references in description ACTIVITY</stentry>
        </strow>
      </simpletable>
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
            ditaXml == """<topic id='class_activity_retired'>
  <title outputclass='class retired'>
    <text>ACTIVITY (Retired)</text>
  </title>
  <shortdesc>This item has been retired from the NHS Data Model and Dictionary.</shortdesc>
  <topic id='class_activity_retired_description'>
    <title>Description</title>
    <body>
      <div>
        <p>
          <b>This item has been retired from the NHS Data Model and Dictionary.</b>
        </p>
        <p>The last version of this item is available in the November 2025 release of the NHS Data Model and Dictionary.</p>
        <p>Access to the last live version of this item can be obtained by emailing 

          <xref href='mailto:support.digitalservices@nhs.net' format='html' scope='external'>support.digitalservices@nhs.net</xref> with "NHS Data Model and Dictionary - Archive Request" in the email subject line.
        </p>
      </div>
    </body>
  </topic>
  <topic id='class_activity_retired_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Class is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ACTIVITIES</stentry>
        </strow>
      </simpletable>
    </body>
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
            ditaXml == """<topic id='class_activity'>
  <title outputclass='class'>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>This item is being used for development purposes and has not yet been approved.</shortdesc>
  <topic id='class_activity_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item is being used for development purposes and has not yet been approved.</p>
      </div>
    </body>
  </topic>
  <topic id='class_activity_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Class is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ACTIVITIES</stentry>
        </strow>
      </simpletable>
    </body>
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
            titleHtml == """<h1 class="title topictitle1 class">ACTIVITY</h1>
<div class="- topic/body body">
  <p class="- topic/shortdesc shortdesc">A provision of SERVICES to a PATIENT by one or more CARE PROFESSIONALS.</p>
</div>"""
        }

        when: "the description is converted to html"
        String descriptionHtml = structure.sections.find { it instanceof DescriptionSection }.generateHtml(websiteHtmlPublishContext)

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """<div class="- topic/body body">
  <div class="- topic/div div">
    <p class="- topic/p p"><p>A provision of <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/79ab1e21-4407-4aae-b777-7b7920fa1963">SERVICES</a> 
to a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENT</a> by one or more 
<a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/9ac6a0b2-b4bf-48af-ad4d-0ecfe268df57">CARE PROFESSIONALS</a>.</p></p>
  </div>
</div>"""
        }

        when: "the attributes are converted to html"
        String attributesHtml = structure.sections.find { it instanceof ClassAttributeSection }.generateHtml(websiteHtmlPublishContext)

        then: "the attributes are published"
        verifyAll {
            attributesHtml
            attributesHtml == """<div class="- topic/body body">
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 5%" />
        <col style="width: 95%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Key</th>
          <th class="- topic/stentry stentry">Attribute Name</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Key</td>
          <td class="- topic/stentry stentry">
            <a class="attribute" title="ACTIVITY IDENTIFIER" href="#/preview/null/attribute/null">ACTIVITY IDENTIFIER</a>
          </td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry"></td>
          <td class="- topic/stentry stentry">
            <a class="attribute" title="ACTIVITY COUNT" href="#/preview/null/attribute/null">ACTIVITY COUNT</a>
          </td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry"></td>
          <td class="- topic/stentry stentry">
            <a class="attribute" title="ACTIVITY DURATION" href="#/preview/null/attribute/null">ACTIVITY DURATION</a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the relationships are converted to html"
        String relationshipsHtml = structure.sections.find { it instanceof ClassRelationshipSection }.generateHtml(websiteHtmlPublishContext)

        then: "the relationships are published"
        verifyAll {
            relationshipsHtml
            relationshipsHtml == """<div class="- topic/body body">
  <p class="- topic/p p">Each ACTIVITY:</p>
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 10%" />
        <col style="width: 45%" />
        <col style="width: 45%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Key</th>
          <th class="- topic/stentry stentry">Relationship</th>
          <th class="- topic/stentry stentry">Class</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Key</td>
          <td class="- topic/stentry stentry">supplied by</td>
          <td class="- topic/stentry stentry">
            <a class="class" title="ORGANISATION" href="#/preview/null/class/null">ORGANISATION</a>
          </td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry"></td>
          <td class="- topic/stentry stentry">located at</td>
          <td class="- topic/stentry stentry">
            <a class="class" title="ADDRESS" href="#/preview/null/class/null">ADDRESS</a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
        }

        when: "the aliases are converted to html"
        String aliasesHtml = structure.sections.find { it instanceof AliasesSection }.generateHtml(websiteHtmlPublishContext)

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """<div class="- topic/body body">
  <p class="- topic/p p">This Class is also known by these names:</p>
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
          <td class="- topic/stentry stentry">ACTIVITIES</td>
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
          <td class="- topic/stentry stentry">Class</td>
          <td class="- topic/stentry stentry">
            <a class="class" title="ACTIVITY" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/901c2d3d-0111-41d1-acc9-5b501c1dc397">ACTIVITY</a>
          </td>
          <td class="- topic/stentry stentry">references in description ACTIVITY</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Attribute</td>
          <td class="- topic/stentry stentry">
            <a class="attribute" title="ACTIVITY COUNT" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/b5170409-97aa-464e-9aad-657c8b2e00f8">ACTIVITY COUNT</a>
          </td>
          <td class="- topic/stentry stentry">references in description ACTIVITY</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Data Element</td>
          <td class="- topic/stentry stentry">
            <a class="element" title="ACTIVITY COUNT (POINT OF DELIVERY)" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/542a6963-cce2-4c8f-b60d-b86b13d43bbe">ACTIVITY COUNT (POINT OF DELIVERY)</a>
          </td>
          <td class="- topic/stentry stentry">references in description ACTIVITY</td>
        </tr>
      </tbody>
    </table>
  </div>
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
            ditaXml == """<topic id='class_activity'>
  <title>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>Change to Class: New</shortdesc>
  <body>
    <div outputclass='new'>
      <div>
        <p>A provision of 

          <xref outputclass='class' keyref='class_service' scope='local'>SERVICES</xref> to a 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENT</xref> by one or more 

          <xref outputclass='class' keyref='class_care_professional' scope='local'>CARE PROFESSIONALS</xref>.
        </p>
      </div>
    </div>
    <div>
      <p>Attributes of this Class are:</p>
      <simpletable relcolwidth='1* 9*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Attribute Name</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_identifier' format='html'>ACTIVITY IDENTIFIER</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_duration' format='html'>ACTIVITY DURATION</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>Each ACTIVITY:</p>
      <simpletable relcolwidth='1* 5* 5*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Relationship</stentry>
          <stentry>Class</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>supplied by</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_organisation' format='html'>ORGANISATION</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>located at</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_address' format='html'>ADDRESS</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Class is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITIES</stentry>
        </strow>
      </simpletable>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item description to change paper dita"() {
        given: "the publish structures are built"
        activeItem.catalogueItem.description = "The current description"
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
            ditaXml == """<topic id='class_activity'>
  <title>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>Change to Class: Updated description, Changed attributes, Changed relationships</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
    <div>
      <p>Attributes of this Class are:</p>
      <simpletable relcolwidth='1* 9*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Attribute Name</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_identifier' format='html'>ACTIVITY IDENTIFIER</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_duration' format='html'>ACTIVITY DURATION</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>Each ACTIVITY:</p>
      <simpletable relcolwidth='1* 5* 5*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Relationship</stentry>
          <stentry>Class</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>supplied by</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_organisation' format='html'>ORGANISATION</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>located at</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_address' format='html'>ADDRESS</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Class is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>ACTIVITIES</stentry>
        </strow>
      </simpletable>
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
            ditaXml == """<topic id='class_activity'>
  <title>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>Change to Class: Changed attributes, Changed relationships, Aliases</shortdesc>
  <body>
    <div>
      <div>
        <p>A provision of 

          <xref outputclass='class' keyref='class_service' scope='local'>SERVICES</xref> to a 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENT</xref> by one or more 

          <xref outputclass='class' keyref='class_care_professional' scope='local'>CARE PROFESSIONALS</xref>.
        </p>
      </div>
    </div>
    <div>
      <p>Attributes of this Class are:</p>
      <simpletable relcolwidth='1* 9*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Attribute Name</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_identifier' format='html'>ACTIVITY IDENTIFIER</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_duration' format='html'>ACTIVITY DURATION</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>Each ACTIVITY:</p>
      <simpletable relcolwidth='1* 5* 5*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Relationship</stentry>
          <stentry>Class</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>supplied by</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_organisation' format='html'>ORGANISATION</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>located at</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_address' format='html'>ADDRESS</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Class is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITIES</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>ALTERNATIVE</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>ACTIVITY GROUP</stentry>
        </strow>
      </simpletable>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for all changes to change paper dita"() {
        given: "the publish structures are built"
        activeItem.catalogueItem.description = "The current description"
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
            ditaXml == """<topic id='class_activity'>
  <title>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>Change to Class: Updated description, Changed attributes, Changed relationships, Aliases</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
    <div>
      <p>Attributes of this Class are:</p>
      <simpletable relcolwidth='1* 9*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Attribute Name</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_identifier' format='html'>ACTIVITY IDENTIFIER</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_count' format='html'>ACTIVITY COUNT</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>
            <xref outputclass='attribute' keyref='attribute_activity_duration' format='html'>ACTIVITY DURATION</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>Each ACTIVITY:</p>
      <simpletable relcolwidth='1* 5* 5*'>
        <sthead>
          <stentry>Key</stentry>
          <stentry>Relationship</stentry>
          <stentry>Class</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Key</stentry>
          <stentry outputclass='new'>supplied by</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_organisation' format='html'>ORGANISATION</xref>
          </stentry>
        </strow>
        <strow>
          <stentry outputclass='new' />
          <stentry outputclass='new'>located at</stentry>
          <stentry outputclass='new'>
            <xref outputclass='class' keyref='class_address' format='html'>ADDRESS</xref>
          </stentry>
        </strow>
      </simpletable>
    </div>
    <div>
      <p>This Class is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITIES</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>ALTERNATIVE</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>ACTIVITY GROUP</stentry>
        </strow>
      </simpletable>
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
            ditaXml == """<topic id='class_activity_retired'>
  <title>
    <text>ACTIVITY</text>
  </title>
  <shortdesc>Change to Class: Retired</shortdesc>
  <body>
    <div>
      <div>
        <ph id='removed-diff-0' outputclass='diff-html-removed'>The previous description</ph>
        <p>
          <b>
            <ph id='added-diff-0' outputclass='diff-html-added'>This item has been retired from the NHS Data Model and Dictionary.</ph>
          </b>
        </p>
        <p>
          <ph outputclass='diff-html-added'>The last version of this item is available in the November 2025 release of the NHS Data Model and Dictionary.</ph>
        </p>
        <p>
          <ph outputclass='diff-html-added'>Access to the last live version of this item can be obtained by emailing</ph>
          <xref href='mailto:support.digitalservices@nhs.net' format='html' scope='external'>
            <ph outputclass='diff-html-added'>support.digitalservices@nhs.net</ph>
          </xref>
          <ph outputclass='diff-html-added'>with "NHS Data Model and Dictionary - Archive Request" in the email subject line.</ph>
        </p>
      </div>
    </div>
    <div>
      <p>This Class is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>ACTIVITIES</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>ALTERNATIVE</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>ACTIVITY GROUP</stentry>
        </strow>
      </simpletable>
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
  <h3>ACTIVITY</h3>
  <h4>Change to Class: New</h4>
  <div>
    <div class="new">
      <p><p>A provision of <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/79ab1e21-4407-4aae-b777-7b7920fa1963">SERVICES</a> 
to a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENT</a> by one or more 
<a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/9ac6a0b2-b4bf-48af-ad4d-0ecfe268df57">CARE PROFESSIONALS</a>.</p></p>
    </div>
    <p>Attributes of this Class are:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 5%" />
          <col style="width: 95%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Attribute Name</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">
              <a class="attribute" title="ACTIVITY IDENTIFIER" href="#/preview/null/attribute/null">ACTIVITY IDENTIFIER</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY COUNT" href="#/preview/null/attribute/null">ACTIVITY COUNT</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY DURATION" href="#/preview/null/attribute/null">ACTIVITY DURATION</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>Each ACTIVITY:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 10%" />
          <col style="width: 45%" />
          <col style="width: 45%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Relationship</th>
            <th>Class</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">supplied by</td>
            <td class="new">
              <a class="class" title="ORGANISATION" href="#/preview/null/class/null">ORGANISATION</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">located at</td>
            <td class="new">
              <a class="class" title="ADDRESS" href="#/preview/null/class/null">ADDRESS</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Class is also known by these names:</p>
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
            <td class="new">ACTIVITIES</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item description to change paper html"() {
        given: "the publish structures are built"
        activeItem.catalogueItem.description = "The current description"

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
  <h3>ACTIVITY</h3>
  <h4>Change to Class: Updated description, Changed attributes, Changed relationships</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
    <p>Attributes of this Class are:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 5%" />
          <col style="width: 95%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Attribute Name</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">
              <a class="attribute" title="ACTIVITY IDENTIFIER" href="#/preview/null/attribute/null">ACTIVITY IDENTIFIER</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY COUNT" href="#/preview/null/attribute/null">ACTIVITY COUNT</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY DURATION" href="#/preview/null/attribute/null">ACTIVITY DURATION</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>Each ACTIVITY:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 10%" />
          <col style="width: 45%" />
          <col style="width: 45%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Relationship</th>
            <th>Class</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">supplied by</td>
            <td class="new">
              <a class="class" title="ORGANISATION" href="#/preview/null/class/null">ORGANISATION</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">located at</td>
            <td class="new">
              <a class="class" title="ADDRESS" href="#/preview/null/class/null">ADDRESS</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Class is also known by these names:</p>
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
            <td>Plural</td>
            <td>ACTIVITIES</td>
          </tr>
        </tbody>
      </table>
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
  <h3>ACTIVITY</h3>
  <h4>Change to Class: Changed attributes, Changed relationships, Aliases</h4>
  <div>
    <div>
      <p><p>A provision of <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/79ab1e21-4407-4aae-b777-7b7920fa1963">SERVICES</a> 
to a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENT</a> by one or more 
<a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/9ac6a0b2-b4bf-48af-ad4d-0ecfe268df57">CARE PROFESSIONALS</a>.</p></p>
    </div>
    <p>Attributes of this Class are:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 5%" />
          <col style="width: 95%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Attribute Name</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">
              <a class="attribute" title="ACTIVITY IDENTIFIER" href="#/preview/null/attribute/null">ACTIVITY IDENTIFIER</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY COUNT" href="#/preview/null/attribute/null">ACTIVITY COUNT</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY DURATION" href="#/preview/null/attribute/null">ACTIVITY DURATION</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>Each ACTIVITY:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 10%" />
          <col style="width: 45%" />
          <col style="width: 45%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Relationship</th>
            <th>Class</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">supplied by</td>
            <td class="new">
              <a class="class" title="ORGANISATION" href="#/preview/null/class/null">ORGANISATION</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">located at</td>
            <td class="new">
              <a class="class" title="ADDRESS" href="#/preview/null/class/null">ADDRESS</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Class is also known by these names:</p>
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
            <td class="new">ACTIVITIES</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">ALTERNATIVE</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">ACTIVITY GROUP</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for all changes to change paper html"() {
        given: "the publish structures are built"
        activeItem.catalogueItem.description = "The current description"

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
  <h3>ACTIVITY</h3>
  <h4>Change to Class: Updated description, Changed attributes, Changed relationships, Aliases</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
    <p>Attributes of this Class are:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 5%" />
          <col style="width: 95%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Attribute Name</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">
              <a class="attribute" title="ACTIVITY IDENTIFIER" href="#/preview/null/attribute/null">ACTIVITY IDENTIFIER</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY COUNT" href="#/preview/null/attribute/null">ACTIVITY COUNT</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">
              <a class="attribute" title="ACTIVITY DURATION" href="#/preview/null/attribute/null">ACTIVITY DURATION</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>Each ACTIVITY:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 10%" />
          <col style="width: 45%" />
          <col style="width: 45%" />
        </colgroup>
        <thead>
          <tr>
            <th>Key</th>
            <th>Relationship</th>
            <th>Class</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Key</td>
            <td class="new">supplied by</td>
            <td class="new">
              <a class="class" title="ORGANISATION" href="#/preview/null/class/null">ORGANISATION</a>
            </td>
          </tr>
          <tr>
            <td class="new"></td>
            <td class="new">located at</td>
            <td class="new">
              <a class="class" title="ADDRESS" href="#/preview/null/class/null">ADDRESS</a>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p>This Class is also known by these names:</p>
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
            <td class="new">ACTIVITIES</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">ALTERNATIVE</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">ACTIVITY GROUP</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }
}
