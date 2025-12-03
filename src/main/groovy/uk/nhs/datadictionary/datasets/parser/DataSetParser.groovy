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
package uk.nhs.datadictionary.datasets.parser

import groovy.util.logging.Slf4j
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SemanticLinkType
import org.maurodata.domain.model.AdministeredItem
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.utils.DDHelperFunctions

@Slf4j
class DataSetParser {

    static final String xmlFileName = "valuesets-expanded-september.xml"

    static Map<String, DataModel> dataModels = [:]

    /*    static void main(String[] args) {

            CatalogueClientFactory.newFromPropertiesPath("/Users/james/git/catalogue-tools/localclient.properties").withCloseable {
                XmlSlurper slurper = XmlSlurper.newInstance()
                def xml = slurper.parse(new File(xmlFileName))

                xml.DDDataSet.each { ddDataSetXml ->
                    log.info(ddDataSetXml.name.text())
                    String dataSetName = ddDataSetXml.name.text()
                    DataModel dataModel = new DataModel(label: dataSetName)
                    log.info("Processing: " + dataSetName)
                    //parseDataSet(ddDataSetXml.definition, dataModel, null)
                    if (dataSetName.startsWith("CDS")) {
                        CDSDataSetParser.parseCDSDataSet(ddDataSetXml.definition, dataModel, new DataDictionary())
                    } else {
                        parseDataSet(ddDataSetXml.definition, dataModel, new DataDictionary())
                    }
                    dataModels[dataSetName] = dataModel
                }
                //dataModels.keySet().each { log.debug(it)}
                Map<String, String> tests = [:]

                tests["Inter-Provider_Transfer_Administrative_Minimum_Data_Set"] = "8C8E5E2E8E6E1E4E2E"
                tests["National_Workforce_Data_Set"] = "6C6E17E17E33E9E55E"
                tests["Chlamydia_Testing_Activity_Data_Set"] = "4C3E9E1C2E3E8E"
                tests["National_Neonatal_Data_Set_-_Episodic_and_Daily_Care"] =
                "14C2C1C2E16E1C2E13E2C13E9E3C2E1C2E1E1C2E1E15E1C1C2E1E28E6C1C2E1E2E2E1C2E1E2E1C2E1E15E2C5C1C2E1E2E1C2E1E2E2E8E2C2E1C2E1E2E2C3E1C1C2E1E4E2C1E1C1C2E1E8E2C1E2C1C2E1E2E9E2C1E1C1C2E1E8E2C1E1C1C2E1E2C1E1C1C2E1E3E11C3C1C2E1E2E2E7E8E4E5E2E7E1E1C2E6E1E1E1E"
                tests["National_Neonatal_Data_Set_-_Two_Year_Neonatal_Outcomes_Assessment"] =
                "1C22C2C2C2E2E8E2C2E2E5E2C1C2E5E3C2E2E2E2E8E2E2E4E3E4E3C1C2E1E1C2E1E1C2E1E3E7E6E5E3E3E1C1C2E1E4E12E12E4E24E1C1C2E1E6E1C1C2E1E9E"
                tests["Radiotherapy_Data_Set"] = "5C1E3E7E9E6E"
                tests["Community_Services_Data_Set"] = "10C7E7C25E5E3E8E4E3E4E5C13E7E2E10E4E2C21E13E10E4C2E2E4E3E2C5E4E12C4E3E5E12E6E4E4E4E4E2E4E3E5E6E"
                tests["Maternity_Services_Data_Set"] = "8C8E4C10E5E3E3E9C28E10E10E4E6E7E4E3E11E5C18E15E3E20E17E6C27E5E4E4E17E3E4C11E4E7E6E2C5E4E6E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Upper_Gastrointestinal"] = "8C1E1E7E3E6E1E1E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Lung"] = "4C5C1E1E2E2E1E1E4E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Children_Teenagers_and_Young_Adults"] = "10C1E2E1E4C1E1E2E1E2C1E1E2E1E2E2E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Skin"] = "1C2E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Core"] =
                "38C6E1E2C1E1E3E2C3E1E3C5E3E2E2C1E2E1C1E9E8E3E3E2C3C1E1E3E2E2E2C1E1E1E11E5E3E1E2C1E2E2E4E8E4E4E2C1E5E7E6E4E4E14E2E10E11E3E5E2E3E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Central_Nervous_System"] = "7C4E1E3E1E2E1E2C1E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Haematological"] = "7C7C1E1E1E2E4E3E1E4C1E1E1E1E4E4C2E2E3E1E3C2E3E4E1E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Colorectal"] = "2C2E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Head_and_Neck"] = "3C2E3E5E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Breast"] = "3C1E1E6E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Sarcoma"] = "3C5E2C3E1E2C1E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Pathology"] = "35C6E1E2C1E1E3E1C1E4E13E1E2C1E1E25E16E2E7E7E4E4E6E8E7E4E1E4E2E9E1E2E2E3E5E11E3E2E5E3E5E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Gynaecological"] = "2C1E2E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Liver"] = "7C3E1E1E3E2E1E1E"
                tests["Cancer_Outcomes_and_Services_Data_Set_-_Urological"] = "7C2E3E3E4E3E2C1E1E2E"
                tests["HIV_and_AIDS_Reporting_Data_Set"] = "9C8E1E15E5E4E15E8E18E2E"
                tests["Systemic_Anti-Cancer_Therapy_Data_Set"] = "6C1C2E9E1C2E1E10E4E10E6E"
                tests["Sexual_and_Reproductive_Health_Activity_Data_Set"] = "5C3E7E5E4E1E"
                tests["Female_Genital_Mutilation_Data_Set"] = "3C11E18E10E"
                tests["Improving_Access_to_Psychological_Therapies_Data_Set"] = "8C7E6C12E5E15E2E3E3E2C9E5E5E3C21E10E7E4C3E5E5E3E6E4E"
                tests["National_Cancer_Waiting_Times_Monitoring_Data_Set"] = "4C4E15E21E14E"
                tests["Mental_Health_Services_Data_Set"] =
                "12C7E12C13E5E5E4E13E5E3E8E4E3E3E3E7C20E2E9E7E8E4E4E4C23E10E2E10E11E5C12E4E5E5E5E13C15E17E5E6E12E2E2E5E7E6E5E4E2E6C4E4E4E4E5E3E5E2C4E4E4C10E3E10E4E7E"
                tests["Diagnostic_Imaging_Data_Set"] = "3C7E5E1C2E4E"
                tests["GUMCAD_Sexually_Transmitted_Infection_Surveillance_System_Data_Set"] = "6C2E10E1C4E10E4C3E4E2E5E5E22E"
                tests["National_Joint_Registry_Data_Set_-_Common_Details"] = "1C4C1C1C2E1E1E1C2E6E5E4E"
                tests["National_Joint_Registry_Data_Set_-_Ankle"] = "2C7C9E4E2E9E1E1E2E9C4E2E3E3E2E9E1E1E2E"
                tests["National_Joint_Registry_Data_Set_-_Knee"] = "2C7C4E5E2E6E1E1E2E9C4E2E4E3E2E6E1E1E2E"
                tests["National_Joint_Registry_Data_Set_-_Hip"] = "2C7C2E5E2E6E1E1E2E9C4E2E6E3E2E6E1E1E2E"
                tests["National_Joint_Registry_Data_Set_-_Elbow"] = "2C8C1E2E5E2E6E1E1E2E10C1E4E2E3E3E2E6E1E1E2E"
                tests["National_Joint_Registry_Data_Set_-_Shoulder"] = "2C11C1E3E7E2E6E3E5E1E1E13E2E13C1E4E2E5E6E2E6E3E5E1E1E13E2E"
                tests["AIDC_for_Patient_Identification_Data_Set"] = "3C3C4E2E4E5E4E"
                tests["NHS_Continuing_Healthcare_Patient_Level_Data_Set"] = "5C7E11E38E14E7E"
                tests["Aggregate_Contract_Monitoring_Data_Set"] = "5C3E4E3E18E8E"
                tests["Patient_Level_Information_Costing_System_Acute_Data_Set_-_Out-Patient_Care"] = "5C8E8E8E1E4E"
                tests["Patient_Level_Information_Costing_System_Acute_Data_Set_-_Admitted_Patient_Care"] = "5C8E8E10E3E4E"
                tests["Patient_Level_Information_Costing_System_Acute_Data_Set_-_Emergency_Care"] = "5C8E8E6E1E4E"
                tests["Patient_Level_Information_Costing_System_Data_Set_-_Reconciliation"] = "3C6E2E2E"
                tests["Patient_Level_Contract_Monitoring_Data_Set"] = "6C3E5E8E11E19E5E"
                tests["Drugs_Patient_Level_Contract_Monitoring_Data_Set"] = "6C3E4E7E15E16E5E"
                tests["Paediatric_Critical_Care_Minimum_Data_Set"] = "5C3E6E1E1E1E"
                tests["Devices_Patient_Level_Contract_Monitoring_Data_Set"] = "6C3E4E7E13E16E4E"
                tests["Neonatal_Critical_Care_Minimum_Data_Set"] = "5C3E7E2E1E1E"
                tests["Critical_Care_Minimum_Data_Set"] = "1C34E"
                //tests["Information_Sharing_to_Tackle_Violence_Minimum_Data_Set"] = ""
                tests["NHS_Continuing_Healthcare_Data_Set"] = "12C3E5E3E2E5E3E3E3E2E4E1E2E"
                tests["Cover_of_Vaccination_Evaluated_Rapidly_(COVER)_Data_Set"] = "6C4E1C2E2E1C2E2E1C2E2E1C2E2E1C2E2E"

                tests["CDS_V6-2_Type_020_-_Outpatient_CDS"] =
                "20C2C2C1C2E1E4E3C1C3E2C2E5E3C2E2C1E1E2E3E1C3E1C4E3C1E2E2E3C1E1E1E1C17E1C6E7C1E2E2E2E2E2E2E3C1E2E2E1C4E1C2E1C5E1C2E1C1E"
                tests["CDS_V6-2-1_Type_005B_-_CDS_Transaction_Header_Group_-_Bulk_Update_Protocol"] = "1C1C12E"
                tests["CDS_V6-2-1_Type_003_-_CDS_Message_Header"] = "1C1C4E"
                tests["CDS_V6-2_Type_100_-_Elective_Admission_List_-_Event_During_Period_(Old_Service_Agreement)_CDS"] = "7C2C2C1C2E1E4E1C7E"
                tests["CDS_V6-2_Type_060_-_Elective_Admission_List_-_Event_During_Period_(Add)_CDS"] =
                "20C2C2C1C2E1E4E3C1C3E2C2E5E3C2E2C1E1E2E3E1C2E1C7E1C14E1C4E7C1E2E2E2E2E2E2E3C1E2E2E1C3E1C2E1C2E1C1E1C3E1C1E1C2E"
                tests["CDS_V6-2-2_Type_011_-_Emergency_Care_CDS"] =
                "23C2C2C1C2E1E4E3C1C3E2C2E5E3C2E2C1E1E2E3E1C7E1C5E1C2E1C2E1C2E1C14E1C10E1C1E1C6E1C4E1C3E1C3E1C3E1C5E1C13E1C2E"
                tests["CDS_V6-2_Type_200_-_Admitted_Patient_Care_-_Unfinished_Delivery_Episode_CDS"] =
                "34C2C2C1C2E1E4E3C1C3E2C2E5E2C2E2C1E1E5E1C5E1C1E1C10E1C6E1C11E1C3E1C6E1C4E3C1E2E2E3C1E1E1E7C1E2E2E2E2E2E2E3C1E2E2E1C10E1C14E1C10E3C4E3E2E3C8E12E7E1C2E1C2E1C1E1C1E1C2E1C4E1C5E2C5E3C3E2C2E3E3C2E2E1E1C4E1C3E"
                tests["CDS_V6-2_Type_140_-_Admitted_Patient_Care_-_Finished_Delivery_Episode_CDS"] =
                "34C2C2C1C2E1E4E3C1C3E2C2E5E2C2E2C1E1E5E1C5E1C1E1C10E1C6E1C11E1C3E1C6E1C4E3C1E2E2E3C1E1E1E7C1E2E2E2E2E2E2E3C1E2E2E1C10E1C14E1C10E3C4E3E2E3C8E12E7E1C2E1C2E1C1E1C1E1C2E1C4E1C5E2C5E3C3E2C2E3E3C2E2E1E1C4E1C3E"

                tests.each { key, value ->
                    verifyDataModel(key, value)
                }
            }
    } */

    static void verifyDataModel(String name, String hash, Boolean print = false) {
        String generatedHash = generateDataSetHash(dataModels[name])
        if (generatedHash != hash || hash == "") {
            printDataModel(dataModels[name])
            log.error(generateDataSetHash(dataModels[name]))
        }
        assert (generatedHash == hash)
        assert (hash != "")
    }

    static void parseDataSet(Node definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> dataClasses = []
        if (definition.children().find {it instanceof Node && it.name() == "table" && DDHelperFunctions.tableIsClassHeader(it)}) {
            dataClasses = OtherDataSetParser.parseDataSetWithHeaderTables(definition, dataModel, dataDictionary)
        } else {
            definition.children().findAll {it instanceof Node && it.name() == "table"}.each {Node table ->
                dataClasses.addAll(OtherDataSetParser.parseDataClassTable(table, dataModel, dataDictionary, 1))
            }
        }
        if (dataClasses.size() == 1 && !dataClasses[0].label) {
            dataClasses[0].label = "Data Set Data Elements"
        }
        dataClasses.eachWithIndex {dataClass, index ->
            dataModel.childDataClasses.add(dataClass)
            //setOrder(dataClass, index + 1)
        }
        fixPotentialDuplicates(dataModel)
    }

    static boolean isSkyBlue(Node node) {
        return node.@class == "skyblue"
    }


    static void printDataModel(DataModel dataModel) {
        log.debug("===============================\n{}", dataModel.label)
        dataModel.childDataClasses.sort {getOrder(it)}.each {dataClass ->
            printDataClass(dataClass, 3)
        }
    }

    static void printDataClass(DataClass dataClass, int indent) {
        String indentString = ""
        for (int i = 0; i < indent; i++) {
            indentString += " "
        }
        String choiceString = ""
        if (isChoice(dataClass)) {
            choiceString = " (choice)"
        } else if (isAnd(dataClass)) {
            choiceString = " (and)"
        } else if (isNotOption(dataClass)) {
            choiceString = " (not option)"
        }
        String webOrder = DataSetParser.getOrder(dataClass)
        log.debug("${indentString}${dataClass.label}${choiceString} (${webOrder})")
        List<AdministeredItem> components = []
        if (dataClass.dataElements) {
            components.addAll(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            components.addAll(dataClass.dataClasses)
        }
        indentString += "   "
        components.sort {getOrder(it)}.each {component ->
            if (component instanceof DataClass) {
                printDataClass(component, indent + 3)
            } else if (component instanceof DataElement) {
                log.debug("${indentString}${component.label}")
            }
        }
    }

    static String generateDataSetHash(DataModel dataModel) {
        String ret = ""
        if (dataModel.childDataClasses) {
            ret += dataModel.childDataClasses.size() + "C"
            dataModel.childDataClasses.sort {getOrder(it)}.each {dataClass ->
                ret += generateDataClassHash(dataClass)
            }
        }
        return ret

    }

    static String generateDataClassHash(DataClass dataClass) {

        String ret = ""
        if (dataClass.dataClasses) {
            ret += dataClass.dataClasses.size() + "C"
            dataClass.dataClasses.sort {getOrder(it)}.each {childDataClass ->
                ret += generateDataClassHash(childDataClass)
            }
        }
        if (dataClass.dataElements) {
            ret += "" + dataClass.dataElements.size() + "E"
        }
        return ret
    }

    static Integer getOrder(AdministeredItem item) {
        Metadata md = item.getMetadata().find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_WEB_ORDER}
        if(md) {
            return Integer.parseInt(md.value())
        }
        if(item instanceof DataElement) {
            return item.order
        } else if(item instanceof DataClass) {
            return item.order
        }
        else {
            return 0
        }

    }

    static void setMetadata(AdministeredItem item, String key, String value) {
        if(!item) {
            log.error("Setting $key on null element!")
        } else {
            if(item.metadata.find {
                it.key == key
            }) {
                throw new Exception("Duplicate metadata ($key) set!")
            } else {
                item.metadata(getDataSetNamespace(item), key, value)
            }
        }

    }

    static void setOrder(AdministeredItem item, Integer order) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_WEB_ORDER, order.toString())
    }

    static void setChoice(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_CHOICE, "true")
    }

    static void setAnd(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_AND, "true")
    }

    static void setAddress(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_ADDRESS_CHOICE, "true")
    }

    static void setNameChoice(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_NAME_CHOICE, "true")
    }

    static void setInclusiveOr(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_INCLUSIVE_OR, "true")
    }

    static void setDataSetReference(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE, "true")
    }

    static boolean isDataSetReference(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE
                && it.value == "true")
        }
    }

    static void setDataSetReferenceTo(AdministeredItem item, String dataSetName) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE_TO, dataSetName)
    }


    static String getDataSetReferenceTo(AdministeredItem item) {
        item.getMetadata().find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE_TO }.value
    }

    static void setMultiplicityText(AdministeredItem item, def multiplicity) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_MULTIPLICITY_TEXT, parsePossibleParagraphs(multiplicity))
    }

    static String getMultiplicityText(AdministeredItem item) {
        item.getMetadata().find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_MULTIPLICITY_TEXT }.value
    }

    static void setMRO(AdministeredItem item, def mro) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_MRO, mandationFromMRO(parsePossibleParagraphs(mro)))
    }

    static String mandationFromMRO(String mro) {
        switch (mro.trim().toLowerCase()) {
            case 'm':
                return 'Mandatory'
            case 'r':
                return 'Required'
            case 'o':
                return 'Optional'
            case 'p':
                return 'Pilot'
            default:
                return ''
        }

    }

    static String getMRO(AdministeredItem item) {
        if(item instanceof DataElement && ((DataElement)item).importingDataClasses != null) {
            // Do nothing yet
        } else {
            item.getMetadata().find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_MRO  }?.value
        }
    }

    static void setRules(AdministeredItem item, def rules) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_RULES, parsePossibleParagraphs(rules))
    }

    static String getRules(AdministeredItem item) {
        item.getMetadata().find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_RULES }?.value
    }

    static void setGroupRepeats(AdministeredItem item, def groupRepeats) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_GROUP_REPEATS, parsePossibleParagraphs(groupRepeats))
    }

    static String getGroupRepeats(AdministeredItem item) {
        item.getMetadata().find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_GROUP_REPEATS }?.value
    }


    static void setNotOption(AdministeredItem item) {
        setMetadata(item, NhsDataDictionary.DATASET_TABLE_KEY_NOT_OPTION, "true")
    }

    static boolean isChoice(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_CHOICE
                && it.value == "true")
        }
    }

    static boolean isAnd(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_AND
                && it.value == "true")
        }
    }

    static boolean isInclusiveOr(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == "Inclusive"
                && it.value == "true")
        }
    }

    static boolean isAddress(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_ADDRESS_CHOICE
                && it.value == "true")
        }
    }

    static boolean isNotOption(AdministeredItem item) {
        item.metadata.find {
            (it.namespace == getDataSetNamespace(item)
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_NOT_OPTION
                && it.value == "true")
        }
    }


    static List<List<Node>> partitionByDuckBlueClasses(Node dataSetDefinition) {

        List<List<Node>> topLevelClasses = []
        List<Node> list = []
        dataSetDefinition.children().each {childNode ->
            if (childNode instanceof Node && childNode.name() == "table" && DDHelperFunctions.tableIsClassHeader(childNode)) {
                if (list.size() > 0) {
                    topLevelClasses.add(list)
                }
                list = []
                list.add(childNode)
            } else if(childNode instanceof Node) {
                list.add(childNode)
            }
        }
        topLevelClasses.add(list)
        topLevelClasses
    }

    static void setNameAndDescriptionFromCell(DataClass dataClass, Node cell) {
        String cellContent = cell.text().trim()
        // log.debug(cellContent)
        int colonIndex = cellContent.indexOf(":")

        if (colonIndex > 0 && !cellContent.startsWith("CHOICE")) {
            dataClass.label = cellContent.subSequence(0, colonIndex)
            dataClass.description = cellContent.substring(colonIndex).replaceFirst(":", "")
        } else {
            int hyphenIndex = cellContent.indexOf(" - ")
            int carryIndex = cellContent.indexOf("To carry details")
            if (hyphenIndex > 0 && !cellContent.startsWith("CHOICE")) {
                dataClass.label = cellContent.subSequence(0, hyphenIndex)
                dataClass.description = cellContent.substring(hyphenIndex).replaceFirst("-", "")
            } else if (carryIndex > 0 && !cellContent.startsWith("CHOICE")) {
                dataClass.label = cellContent.subSequence(0, carryIndex)
                dataClass.description = cellContent.substring(carryIndex)
            } else {
                if ((!dataClass.label || dataClass.label == "") && cellContent.startsWith("To carry the ")
                    && cellContent.indexOf("details.") > 0) {
                    // to deal with the GUMCAD data model...
                    int detailsLocation = cellContent.indexOf("details.")
                    dataClass.label = capitalizeGumCad(cellContent.subSequence(13, detailsLocation).toString())
                    dataClass.description = cellContent
                } else if ((!dataClass.label || dataClass.label == "") && cellContent.contains(".")) {
                    int detailsLocation = cellContent.indexOf(".")
                    dataClass.label = cellContent.subSequence(0, detailsLocation)
                    dataClass.description = cellContent.substring(detailsLocation + 1)
                } else {
                    if (dataClass.label && dataClass.label != "" && (!dataClass.description || dataClass.description == "")) {
                        dataClass.description = cellContent
                    } else {
                        dataClass.label = cellContent
                    }
                }
            }
        }
        dataClass.label = dataClass.label.trim()
        dataClass.description = dataClass.description?.trim() ?: null
    }

    static DataElement getElementFromText(Object anchor, DataModel dataModel, NhsDataDictionary dataDictionary, DataClass currentClass) {
        //String elementLabel = ((String)anchor.@href).replaceAll(" ", "%20")
        String elementUrl
        DataElement originalDataElement = null
        if(anchor && anchor.@href) {
            elementUrl = ((String) anchor.@href)

            if (fileNameFixes[elementUrl]) {
                elementUrl = fileNameFixes[elementUrl]
            }
            if (dataDictionary) {
                originalDataElement = dataDictionary.elementsByUrl[elementUrl]
            }
        } else if (anchor && !anchor.@href) {
            originalDataElement = dataDictionary.elements[anchor.text()]
        }
        DataElement dataElement
        if (!originalDataElement) {
            log.warn("Cannot find element: ${anchor} in class: ${currentClass.label}, model: ${dataModel.label}")
            DataType defaultDataType = dataModel.dataTypes.find {it.label == "DataType: Any"}
            if (!defaultDataType) {
                defaultDataType = new DataType(label: "DataType: Any", dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)
                dataModel.dataTypes.add(defaultDataType)
            }
            dataElement = new DataElement(label: DDHelperFunctions.tidyLabel(anchor.text()), dataType: defaultDataType)
            currentClass.dataElements.add(dataElement)
        } else {
            DataType dataType = dataModel.dataTypes.find {it.label == originalDataElement.dataType.label }
            if(!dataType) {
                dataType = copyDataType(originalDataElement.dataType)
                dataModel.dataTypes.add(dataType)
            }

            dataElement = new DataElement(label: DDHelperFunctions.tidyLabel(anchor.text()), dataType: dataType)
            currentClass.dataElements.add(dataElement)

            SemanticLink semanticLink = new SemanticLink(
                target: originalDataElement,
                linkType: SemanticLinkType.REFINES,
                multiFacetAwareItem: dataElement
                )
            dataElement.semanticLinks.add(semanticLink)

        }
        return dataElement
    }

    static DataType copyDataType(DataType original) {
        if(original.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE) {
            return new DataType(
                    label: original.label,
                    description: original.description,
                    units: original.units,
                    dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE
            )
        } else if(original.dataTypeKind == DataType.DataTypeKind.MODEL_TYPE) {
            return new DataType(
                    modelResourceDomainType: original.modelResourceDomainType,
                    modelResource: original.modelResource,
                    label: original.label,
                    description: original.description,
                    dataTypeKind: DataType.DataTypeKind.MODEL_TYPE
            )
        }
    }

    static final Map<String, String> fileNameFixes = [
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/percentage_of_nhs_continuing_healthcare_referrals_concluded_within_28_days_(standard)_de.asp":
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/percentage_of_nhs_continuing_healthcare_referrals_conluded_within_28_days_(standard)_de" +
            ".asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_local_identifier_de.asp"                                                 :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_identifier_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_team_local_identifier_de.asp"                                            :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_team_identifier_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(antenatal%20treatment)_de.asp"                                             :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(antenatal_treatment)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/h/hu/human_papillomavirus_vaccination_dose_de.asp"                                                :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/h/hu/human_papillomavirus_vaccination_dose_given_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/r/rep/residual_disease_size_(gynaecological_cancer)_de.asp"                                       :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/r/rep/residual_disease_size_(gynaecology_cancer)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pri/primary_induction_chemotherapy_failure_indicator_de.asp"                                    :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pri/primary_induction_failure_indicator_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/f/fo/french_american_british_classification_(acute_myeloid_leukaemia)_de.asp"                     :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/f/fo/french_american_british_classification_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/t/th/tissue_type_banked_at_diagnosis_de.asp"                                                      :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/t/th/tissue_banked_at_diagnosis_type_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_operating_surgeon_type_(cancer)_de.asp"                                  :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_surgeon_grade_(cancer)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(transthoracic_echocardiogram_test)_de.asp"                                 :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(transthoracic_echocardiograml)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_(baby))_de.asp"                           :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_baby)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_(mother))_de.asp"                         :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_mother)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/d/dea/decision_support_tool_completed_date_(nhs_continuing healthcare_standard)_de.asp"           :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/d/dea/decision_support_tool_completed_date_(nhs_continuing%20healthcare_standard)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(primary)_de.asp"                                                              :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(primary)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(secondary)_de.asp"                                                            :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(secondary)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(tertiary)_de.asp"                                                             :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(tertiary)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/peritoneal_involvement_indicator_(endometrial_cancer)_de.asp"                                :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/peritoneal_involvement_indicator_(endomertrial_cancer)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gene_or_stratification_biomarker_authorised_date_de.asp"                                        :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gene_or_stratification_biomarker_reported_date_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/s/st/stage_date_(cancer_site_specific_stage)_de.asp"                                              :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/s/st/stage_grouping_date_(cancer_site_specific_stage)_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_site_identifier_(of_acute_oncology_assessment)_de.asp"                         :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_site_identifier_(of_clinical_assessment)1_de.asp",
        "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/a/act/acute_oncology_assessment_patient_presentation_type_de.asp"                                 :
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/a/act/acute_oncology_assessment_patient_presentattion_type_de.asp"
    ]

    static void fixPotentialDuplicates(DataModel dataModel) {
        List<String> classNames = []
        dataModel.childDataClasses.each {childDataClass ->
            if (classNames.contains(childDataClass.label)) {
                log.debug("Duplicate class label: ${childDataClass.label} in ${dataModel.label}")
                int idx = 1
                while (classNames.contains(childDataClass.label + " " + idx)) {
                    idx++
                }
                childDataClass.label = childDataClass.label + " " + idx
            }
            classNames.add(childDataClass.label)
            fixPotentialDuplicates(childDataClass, dataModel.label)
        }
    }

    static void fixPotentialDuplicates(DataClass dataClass, String modelName) {
        if (dataClass.dataClasses) {
            List<String> classNames = []
            dataClass.dataClasses.each {childDataClass ->
                if (classNames.contains(childDataClass.label)) {
                    log.debug("Duplicate class label: ${childDataClass.label} in ${dataClass.label}  in ${modelName}")
                    int idx = 1
                    while (classNames.contains(childDataClass.label + " " + idx)) {
                        idx++
                    }
                    childDataClass.label = childDataClass.label + " " + idx
                }
                classNames.add(childDataClass.label)
                fixPotentialDuplicates(childDataClass, modelName)
            }
        }
        if (dataClass.dataElements) {
            List<String> elementNames = []
            dataClass.dataElements.each {childDataElement ->
                if (elementNames.contains(childDataElement.label)) {
                    log.debug("Duplicate element label: ${childDataElement.label} in ${dataClass.label} in ${modelName}")
                    int idx = 1
                    while (elementNames.contains(childDataElement.label + " " + idx)) {
                        idx++
                    }
                    childDataElement.label = childDataElement.label + " " + idx

                }
                elementNames.add(childDataElement.label)
            }
        }
    }


    static void getAndSetMRO(Node node, List<DataElement> elementList, DataClass choiceClass = null) {
        List<List<String>> params = getElementParameters(node)

        if (params.size() == 1 && elementList.size() > 1) {
            setMRO(choiceClass, params[0])
        } else if (params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setMRO(element, params[idx].join(';'))
            }
        } else {
            log.debug("Params error! {} {}\n{}",
                      "" + elementList.size(),
                      "" + params.size(),
                      elementList.toString())
            elementList.each {DataElement entry ->
                log.debug(entry.label)
            }
            params.each {
                log.debug(it.toString())
            }
            log.debug(node.toString())
        }

    }

    static void getAndSetRepeats(Node node, List<DataElement> elementList, DataClass choiceClass = null) {
        List<List<String>> params = getElementParameters(node)
        if (params.size() == 1 && elementList.size() > 1) {
            setGroupRepeats(choiceClass, params[0])
        } else if (params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setGroupRepeats(element, params[idx].join(';'))
            }
        } else {
            log.warn("Params error! ")
            log.warn(elementList.toString())
            elementList.each {DataElement entry ->
                log.warn(entry.label)
            }
            log.warn(node.toString())
        }

    }


    static void getAndSetRules(Node node, List<DataElement> elementList, DataClass choiceClass = null) {
        List<List<String>> params = getElementParameters(node)

        if (choiceClass && params.size() == 1 && elementList.size() > 1) {
            setRules(choiceClass, params[0])
        } else if (params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setRules(element, params[idx].join(';'))
            }
        } else {
            log.debug("Params error! ")
            log.debug(elementList.toString())
            elementList.each {DataElement entry ->
                log.debug(entry.label)
            }
            log.debug(node.toString())
        }

    }


    static List<List<String>> getElementParameters(Node td) {
        td.depthFirst().findAll {n ->
            n instanceof Node && (n.name() == "strong" || n.name() == "em" || n.name() == "p")
        }.each {
            n -> removeNodeKeepChildren(n)
        }


        // log.debug("getElementParameters")
        List<List<String>> allValues = []
        List<String> currentValues = []
        for (int i = 0; i < td.children().size(); i++) {
            if (!(td.children()[i] instanceof Node) &&
                (td.children()[i].trim().equalsIgnoreCase("or") || td.children()[i].trim().equalsIgnoreCase("and"))) {
                allValues.add(currentValues)
                currentValues = []
            } else if (!(td.children()[i] instanceof Node) &&
                       td.children()[i] != "\u00a0" &&
                       td.children()[i].trim().replace(" ", "") != "") {
                // log.debug("Adding value: '${td.children()[i]}'")
                currentValues.add(td.children()[i].trim())
            } else if (isBr(td.children()[i]) && td.children()[i + 1] && isBr(td.children()[i + 1])) {
                allValues.add(currentValues)
                currentValues = []
            } else if (isBr(td.children()[i]) &&
                       td.children()[i + 1] && !(td.children()[i + 1] instanceof Node) &&
                       (td.children()[i + 1] == " " || td.children()[i + 1] == "\u00a0") &&
                       td.children()[i + 2] && isBr(td.children()[i + 2])) {
                // log.debug("Here")
                allValues.add(currentValues)
                currentValues = []
                i += 2
            } else if (isBr(td.children()[i])) {
                // skip
            } else if (td.children()[i] instanceof Node && ["strong", "em", "p"].contains(td.children()[i].name()) ) {
                // This should be a String...
                if(td.children()[i].children()[0].toString().trim().replace(" ", "") != "") {
                    currentValues.add(td.children()[i].children()[0].trim())
                }
            } else {
                log.error("Unknown node!!\n{}", td.children()[i])
            }
        }
        allValues.add(currentValues)
        return allValues

    }

    static boolean isBr(Object n) {
        return (n instanceof Node && n.name() == "br")
    }

    static void removeNodeKeepChildren(Node node) {
        Node parent = node.parent()
        if(!parent) {
            log.error("null parent!")
            log.error(node.name().toString())
            log.error(node.toString())
        }
        int location = parent.children().indexOf(node)
        List<Node> nodes = node.children()
        parent.remove(node)
        nodes.remove(node)
        nodes.reverse().each { cn -> parent.children().add(location, cn)}
    }


    static List<List> partitionList(List input, Closure<Boolean> cl) {
        List ret = []
        List currentList = null
        input.each {inp ->
            if (cl.call(inp)) {
                if (currentList != null) {
                    ret.add(currentList)
                }
                currentList = null
            } else {
                if (currentList == null) {
                    currentList = [inp]
                } else {
                    currentList.add(inp)
                }
            }
        }
        if (currentList) {
            ret.add(currentList)
        }
        return ret


    }

    static String capitalizeGumCad(String input) {
        // log.debug(input)
        char[] chars = input.toLowerCase().toCharArray()
        boolean found = false
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i])
                found = true
            } else if (Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'') {
                // You can add other chars here
                found = false
            }
        }
        // log.debug(String.valueOf(chars))
        return String.valueOf(chars)
    }

    static List<Node> tableRowCells(Node tr) {
        tr.children().findAll { it instanceof Node && (it.name() == "td" || it.name() == "th") }
    }

    static String parsePossibleParagraphs(def input) {
        if (input instanceof String) {
            return input
        }
        if(input instanceof List) {
            String output = ""
            input.eachWithIndex {p, idx ->
                if (idx != 0) {
                    output += ";"
                }
                output += parsePossibleParagraphs(p)
            }
            return output
        }
        String output = ""
        input.p.eachWithIndex { p, idx ->
            if (idx != 0) {
                ouptut += ";"
            }
            output += p.text()
        }
        return output
    }

    static String getDataSetNamespace(AdministeredItem item) {
        if(item instanceof DataElement) {
            return NhsDataDictionary.METADATA_DATASET_ELEMENT_NAMESPACE
        } else {
            return NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
        }
    }


}