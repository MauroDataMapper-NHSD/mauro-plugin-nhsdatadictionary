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
package uk.nhs.digital.maurodatamapper.datadictionary

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import spock.lang.Specification
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetConstraint
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDDSupportingInformation
import uk.nhs.datadictionary.NhsDataDictionary

@MicronautTest(startApplication = true, environments = ['secured'])
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
class NhsDataDictionarySpec extends Specification {
    void "should process links from xml for classes and attributes"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDClass eventDateTimeClass = new NhsDDClass()
        eventDateTimeClass.catalogueItem = new DataClass()
        eventDateTimeClass.catalogueItem.label = "EVENT DATE TIME"
        eventDateTimeClass.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/classes/event_date_time.html"])
        eventDateTimeClass.catalogueItem.description = "Defines individual <a href=\"https://datadictionary.nhs.uk/attributes/event_date.html\">EVENT DATES</a> and <a href=\"https://datadictionary.nhs.uk/attributes/event_time.html\">EVENT TIMES</a>."
        dataDictionary.classes[eventDateTimeClass.name] = eventDateTimeClass

        NhsDDAttribute eventDateAttribute = new NhsDDAttribute()
        eventDateAttribute.catalogueItem = new DataElement()
        eventDateAttribute.catalogueItem.label = "EVENT DATE"
        eventDateAttribute.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/attributes/event_date.html"])
        eventDateAttribute.catalogueItem.description = "<p>The date, month, year and century, or any combination of these elements, of an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>.</p>"
        eventDateAttribute.parentClass = eventDateTimeClass // Required to produce the correct path
        dataDictionary.attributes[eventDateAttribute.name] = eventDateAttribute

        NhsDDAttribute eventTimeAttribute = new NhsDDAttribute()
        eventTimeAttribute.catalogueItem = new DataElement()
        eventTimeAttribute.catalogueItem.label = "EVENT TIME"
        eventTimeAttribute.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/attributes/event_time.html"])
        eventTimeAttribute.catalogueItem.description = "<p>The time (using a 24 hour clock) at which an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>, or the action in an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>, takes place.</p><p>This may include representation of a time zone.</p>"
        eventTimeAttribute.parentClass = eventDateTimeClass // Required to produce the correct path
        dataDictionary.attributes[eventTimeAttribute.name] = eventTimeAttribute

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedEventDateDefinition = "<p>The date, month, year and century, or any combination of these elements, of an <a href=\"dm:Classes and Attributes|dc:EVENT DATE TIME\">EVENT DATE TIME</a>.</p>"
        String expectedEventTimeDefinition = "<p>The time (using a 24 hour clock) at which an <a href=\"dm:Classes and Attributes|dc:EVENT DATE TIME\">EVENT DATE TIME</a>, or the action in an <a href=\"dm:Classes and Attributes|dc:EVENT DATE TIME\">EVENT DATE TIME</a>, takes place.</p><p>This may include representation of a time zone.</p>"
        String expectedEventDateTimeDefinition = "Defines individual <a href=\"dm:Classes and Attributes|dc:EVENT DATE TIME|de:EVENT DATE\">EVENT DATES</a> and <a href=\"dm:Classes and Attributes|dc:EVENT DATE TIME|de:EVENT TIME\">EVENT TIMES</a>."

        verifyAll {
            eventDateAttribute.description == expectedEventDateDefinition
            eventTimeAttribute.description == expectedEventTimeDefinition
            eventDateTimeClass.description == expectedEventDateTimeDefinition
        }
    }

    void "should process links from xml for terminology definitions"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDBusinessDefinition businessDefinition = new NhsDDBusinessDefinition()
        businessDefinition.catalogueItem = new Term()
        businessDefinition.catalogueItem.code = "Abbreviated Mental Test Score"
        businessDefinition.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html"])
        businessDefinition.catalogueItem.description = "<p>The <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a> is an <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a>.</p>"
        dataDictionary.businessDefinitions[businessDefinition.name] = businessDefinition

        NhsDDSupportingInformation supportingInformation = new NhsDDSupportingInformation()
        supportingInformation.catalogueItem = new Term()
        supportingInformation.catalogueItem.code = "Accessible Information"
        supportingInformation.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/supporting_information/accessible_information.html"])
        supportingInformation.catalogueItem.description = "<a href=\"https://datadictionary.nhs.uk/supporting_information/accessible_information.html\">Accessible Information</a> is information which is able to be read or received and understood by the individual or group for which it is intended.</p>"
        dataDictionary.supportingInformation[supportingInformation.name] = supportingInformation

        NhsDDDataSetConstraint dataSetConstraint = new NhsDDDataSetConstraint()
        dataSetConstraint.catalogueItem = new Term()
        dataSetConstraint.catalogueItem.code = "Community Services Data Set Constraints"
        dataSetConstraint.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/data_sets/message_documentation/data_set_constraints/community_services_data_set_constraints.html"])
        dataSetConstraint.catalogueItem.description = "<p>The <a href=\"https://datadictionary.nhs.uk/data_sets/message_documentation/data_set_constraints/community_services_data_set_constraints.html\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Community Services Data Set</a>.</p>"
        dataDictionary.dataSetConstraints[dataSetConstraint.name] = dataSetConstraint

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedBusinessDefinition = "<p>The <a href=\"te:NHS Business Definitions|tm:Abbreviated Mental Test Score\">Abbreviated Mental Test Score</a> is an <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a>.</p>"
        String expectedSupportingInformation = "<a href=\"te:Supporting Information|tm:Accessible Information\">Accessible Information</a> is information which is able to be read or received and understood by the individual or group for which it is intended.</p>"
        String expectedDataSetConstraint = "<p>The <a href=\"te:Data Set Constraints|tm:Community Services Data Set Constraints\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Community Services Data Set</a>.</p>"
        verifyAll {
            businessDefinition.description == expectedBusinessDefinition
            supportingInformation.description == expectedSupportingInformation
            dataSetConstraint.description == expectedDataSetConstraint
        }
    }

    void "should process links from xml for data sets"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDDataSet dataSet = new NhsDDDataSet()
        dataSet.path = ["Administrative Data Sets"]
        dataSet.catalogueItem = new DataModel()
        dataSet.catalogueItem.label = "Inter-Provider Transfer Administrative Minimum Data Set"
        dataSet.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/data_sets/administrative_data_sets/inter-provider_transfer_administrative_minimum_data_set.html"])
        dataSet.catalogueItem.description = "<p>The <a href=\"https://datadictionary.nhs.uk/data_sets/administrative_data_sets/inter-provider_transfer_administrative_minimum_data_set.html\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Inter-Provider Transfer Administrative Minimum Data Set</a>.</p>"
        dataDictionary.dataSets[dataSet.name] = dataSet

        NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder()
        dataSetFolder.catalogueItem = new Folder()
        dataSetFolder.catalogueItem.label = "Inter-Provider Transfer Administrative Minimum Data Set Overview"
        dataSetFolder.folderPath = ["Administrative Data Sets"]
        dataSetFolder.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/data_sets/administrative_data_sets/overviews/inter-provider_transfer_administrative_minimum_data_set_overview.html"])
        dataSetFolder.catalogueItem.description = "<p>This <a href=\"https://datadictionary.nhs.uk/data_sets/administrative_data_sets/overviews/inter-provider_transfer_administrative_minimum_data_set_overview.html\">Inter-Provider_Transfer_Administrative_Minimum_Data_Set</a> specifies the data necessary to permit the receiving <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/health_care_provider.html\">Health_Care_Provider</a> to be able to report the <a href=\"https://datadictionary.nhs.uk/classes/patient.html\">PATIENT</a>'s progress along their <a href=\"https://datadictionary.nhs.uk/classes/patient_pathway.html\">PATIENT_PATHWAY</a> and, in particular, their <a href=\"https://datadictionary.nhs.uk/classes/referral_to_treatment_period.html\">REFERRAL_TO_TREATMENT_PERIOD</a>.</p>"
        dataDictionary.dataSetFolders[[dataSetFolder.name]] = [dataSetFolder]

        NhsDDElement dataElement = new NhsDDElement()
        dataElement.catalogueItem = new DataElement()
        dataElement.catalogueItem.label = "ABBREVIATED MENTAL TEST SCORE"
        dataElement.addOtherProperties(["ddUrl": "https://datadictionary.nhs.uk/data_elements/abbreviated_mental_test_score.html"])
        dataElement.catalogueItem.description = "<a href=\"https://datadictionary.nhs.uk/data_elements/abbreviated_mental_test_score.html\">ABBREVIATED_MENTAL_TEST_SCORE</a> is the <a href=\"https://datadictionary.nhs.uk/attributes/person_score.html\">PERSON_SCORE</a> where the <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a> is <em>'<a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a>'</em>.<p>The score is in the range 0 to 10.</p>"
        dataDictionary.elements[dataElement.name] = dataElement

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedDataSetDefinition = "<p>The <a href=\"fo:Data Sets|fo:Administrative Data Sets|dm:Inter-Provider Transfer Administrative Minimum Data Set\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Inter-Provider Transfer Administrative Minimum Data Set</a>.</p>"
        String expectedDataSetFolderDefinition = "<p>This <a href=\"fo:Data Sets|fo:Administrative Data Sets\">Inter-Provider Transfer Administrative Minimum Data Set</a> specifies the data necessary to permit the receiving <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/health_care_provider.html\">Health_Care_Provider</a> to be able to report the <a href=\"https://datadictionary.nhs.uk/classes/patient.html\">PATIENT</a>'s progress along their <a href=\"https://datadictionary.nhs.uk/classes/patient_pathway.html\">PATIENT_PATHWAY</a> and, in particular, their <a href=\"https://datadictionary.nhs.uk/classes/referral_to_treatment_period.html\">REFERRAL_TO_TREATMENT_PERIOD</a>.</p>"
        String expectedDataElementDefinition = "<a href=\"dm:Data Elements|dc:A|de:ABBREVIATED MENTAL TEST SCORE\">ABBREVIATED MENTAL TEST SCORE</a> is the <a href=\"https://datadictionary.nhs.uk/attributes/person_score.html\">PERSON_SCORE</a> where the <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a> is <em>'<a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a>'</em>.<p>The score is in the range 0 to 10.</p>"
        verifyAll {
            dataSet.description == expectedDataSetDefinition
            dataSetFolder.description == expectedDataSetFolderDefinition
            dataElement.description == expectedDataElementDefinition
        }
    }
}
