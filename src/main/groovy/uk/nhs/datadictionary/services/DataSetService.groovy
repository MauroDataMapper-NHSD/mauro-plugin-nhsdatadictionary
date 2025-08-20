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
package uk.nhs.datadictionary.services

import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelType
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.security.CatalogueUser
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetClass
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.datasets.parser.CDSDataSetParser
import uk.nhs.datadictionary.datasets.parser.DataSetParser
import uk.nhs.datadictionary.publish.MauroCatalogueItemPathResolver
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.Section
import uk.nhs.datadictionary.publish.structure.datasets.DataSetSection

@Slf4j
@Singleton
class DataSetService extends DataDictionaryComponentService<DataModel, NhsDDDataSet> {

    static XmlParser xmlParser = new XmlParser(false, false)
    static {
        xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }


    @Override
    NhsDDDataSet show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        // Load all available Data Elements into the dictionary so that the Data Set preview, when loading classes/element rows, can
        // match up elements in the specification tables
        DataModel elementsModel = nhsDataDictionaryService.getElementsModel(versionedFolderId)
        nhsDataDictionaryService.addElementsToDictionary(elementsModel, dataDictionary)

        DataModel dataModel = dataModelService.get(id)

        NhsDDDataSet dataSet = getNhsDataDictionaryComponentFromCatalogueItem(dataModel, dataDictionary)
        dataSet.definition = convertLinksInDescription(versionedFolderId, dataSet.getDescription())

        DictionaryItem structure = dataSet.getPublishStructure()
        Section specificationSection = structure.sections.find { it instanceof DataSetSection }
        if (specificationSection) {
            // TODO: Improve this preview, all HTML preview items should use publish model, this just gets a quick result for a data set specification fix
            MauroCatalogueItemPathResolver pathResolver = new MauroCatalogueItemPathResolver(
                dataDictionary.containingVersionedFolder,
                dataModelService,
                dataClassService,
                dataElementService,
                terminologyService)

            PublishContext publishContext = new PublishContext(PublishTarget.WEBSITE)
            publishContext.setItemLinkScanner(
                ItemLinkScanner.createForHtmlPreview(
                    dataDictionary.containingVersionedFolder.id,
                    pathResolver))

            // Don't pretty print the output, try to reduce the response size
            publishContext.prettyPrintHtml = false

            String specificationHtml = specificationSection.generateHtml(publishContext)
            dataSet.htmlStructure = specificationHtml
        }

        return dataSet
    }

    @Override
    Set<DataModel> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        Set<DataModel> returnModels = [] as Set
        Folder dataSetsFolder = nhsDataDictionaryService.getDataSetsFolder(versionedFolderId)

        getAllDataSets([], dataSetsFolder, includeRetired).values().each {dataModels ->
            returnModels.addAll(dataModels.findAll {dataModel ->
                includeRetired || !catalogueItemIsRetired(dataModel)
            })
        }
        return returnModels
    }

    Map<List<String>, Set<DataModel>> getAllDataSets(List<String> currentPath, Folder dataSetsFolder, boolean includeRetired = false) {
        Map<List<String>, Set<DataModel>> returnModels = [:]
        returnModels[currentPath] = dataModelService.findAllByFolderId(dataSetsFolder.id) as Set
        dataSetsFolder.childFolders.each {childFolder ->
            List<String> newPath = []
            newPath.addAll(currentPath)
            newPath.add(childFolder.label)
            returnModels.putAll(getAllDataSets(newPath, childFolder, includeRetired))
        }
        return returnModels
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".data set"
    }

    @Override
    NhsDDDataSet getNhsDataDictionaryComponentFromCatalogueItem(DataModel catalogueItem, NhsDataDictionary dataDictionary, List<Metadata> metadata = null) {
        NhsDDDataSet dataSet = new NhsDDDataSet()
        nhsDataDictionaryComponentFromItem(dataDictionary, catalogueItem, dataSet, metadata)
        catalogueItem.childDataClasses.each {dataClass ->
            dataSet.dataSetClasses.add(new NhsDDDataSetClass(dataClass, dataDictionary))
        }
        dataSet.dataSetClasses = dataSet.dataSetClasses.sort { it.webOrder }
        dataSet.dataDictionary = dataDictionary
        return dataSet
    }

    @Override
    List<Edit> getMergeEditsForChangeLog(NhsDataDictionaryComponent component) {
        // Special code for loading the change log for a Data Set, since all the edit history entries are stored
        // not just in the Data Set (Data Model), but child components too
        DataModel dataModel = component.catalogueItem as DataModel
        if (!dataModel) {
            return []
        }

        List<Edit> allMergeEdits = []
        loadDataModelMergeEdits(allMergeEdits, dataModel)
        dataModel.childDataClasses.each { dataClass ->
            loadDataClassMergeEdits(allMergeEdits, dataClass)
        }

        allMergeEdits.sort { it.dateCreated }
    }

    void loadDataModelMergeEdits(List<Edit> allMergeEdits, DataModel item) {
        log.info("Getting merge edits for data model '$item.label'")
        List<Edit> mergeEdits = editService.findAllByResourceAndTitle(item.domainType, item.id, EditTitle.MERGE)
        if (mergeEdits.empty) {
            return
        }

        allMergeEdits.addAll(mergeEdits)
    }

    void loadDataClassMergeEdits(List<Edit> allMergeEdits, DataClass item) {
        log.info("Getting merge edits for data class '$item.label'")
        List<Edit> mergeEdits = editService.findAllByResourceAndTitle(item.domainType, item.id, EditTitle.MERGE)
        if (mergeEdits.empty) {
            return
        }

        allMergeEdits.addAll(mergeEdits)

        item.dataClasses.each { dataClass ->
            loadDataClassMergeEdits(allMergeEdits, dataClass)
        }

        item.getDataElements().each { dataElement ->
            loadDataElementMergeEdits(allMergeEdits, dataElement)
        }
    }

    void loadDataElementMergeEdits(List<Edit> allMergeEdits, DataElement item) {
        log.info("Getting merge edits for data element '$item.label'")
        List<Edit> mergeEdits = editService.findAllByResourceAndTitle(item.domainType, item.id, EditTitle.MERGE)
        if (mergeEdits.empty) {
            return
        }

        allMergeEdits.addAll(mergeEdits)
    }

    /*
        String outputClassAsDita(DataClass dataClass, DataDictionary dataDictionary) {

            StringBuffer result = new StringBuffer()
            if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
                result.append "<b>One of the following options must be used:</b>"

                getSortedChildElements(dataClass).eachWithIndex {childDataClass, idx ->
                    if (idx != 0) {
                        result.append "<b>Or</b>"
                    }
                    outputClassAsDita((DataClass) childDataClass, dataDictionary)
                }


            } else {
                result.append "<table>"
                result.append "<tbody>"
                result.append "<tr class='thead-light'>"
                result.append "<td colspan='2'><b>${dataClass.label}</b>"
                if (dataClass.description) {
                    result.append "<p>${dataClass.description}</p>"
                }
                result.append "</td>"
                result.append "</tr>"

                if (dataClass.dataElements && dataClass.dataElements.size() > 0) {
                    result.append "<tr>"
                    result.append "<td style='width:20%'>Mandation</td>"
                    result.append "<td style='width:80%'>Data Elements</td>"
                    result.append "</tr>"
                }

                result.append addAllChildRows(dataClass, dataDictionary)
                result.append "</tbody>"
                result.append "</table>"
                return result.toString()

            }
        }

        String addAllChildRows(DataClass parentDataClass, DataDictionary dataDictionary) {
            StringBuffer result = new StringBuffer()
            if (parentDataClass.dataElements) {
                getSortedChildElements(parentDataClass).each {child ->
                    result.append addChildRow(child, dataDictionary)
                }

            } else {
                // no dataElements
                getSortedChildElements(parentDataClass).eachWithIndex {childDataClass, idx ->
                    if (DataSetParser.isChoice(parentDataClass) && idx != 0) {
                        result.append "<tr><td colspan='2'><b>Or</b></td></tr>"
                    }
                    result.append addSubClass((DataClass) childDataClass, dataDictionary)
                }
            }
            return result
        }

        String addChildRow(CatalogueItem child, DataDictionary dataDictionary) {
            return "<tr><td>${DataSetParser.getMRO(child)}</td><td>${createLinkFromItem(child, dataDictionary)}</td></tr>"
        }

        String createLinkFromItem(CatalogueItem catalogueItem, DataDictionary dataDictionary) {
            if (catalogueItem instanceof DataElement) {
                return createLinkFromDataElement((DataElement) catalogueItem, dataDictionary)
            } else if (catalogueItem instanceof DataClass) {
                String xRefs = ""
                if (DataSetParser.isAddress(catalogueItem)) {
                    DataElement childElement = (DataElement) getSortedChildElements(catalogueItem)[0]
                    String link = createLinkFromItem(childElement, dataDictionary)
                    xRefs += link.replace("</p>", " - <a class='class' href=''>ADDRESS STRUCTURED</a></p>")
                    xRefs += "<p>Or</p>"
                    xRefs += link.replace("</p>", " - <a class='class' href=''>ADDRESS UNSTRUCTURED</a></p>")
                } else if (DataSetParser.isChoice(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                    getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                        if (idx != 0) {
                            xRefs += "<p>Or</p>"
                        }
                        xRefs += createLinkFromItem(childItem, dataDictionary)
                    }
                } else if (DataSetParser.isAnd(catalogueItem) && (catalogueItem.label.startsWith("Choice") || catalogueItem.label.startsWith("And"))) {
                    getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                        if (idx != 0) {
                            xRefs += "<p>And</p>"
                        }
                        xRefs += createLinkFromItem(childItem, dataDictionary)
                    }
                } else if (DataSetParser.isInclusiveOr(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                    getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                        if (idx != 0) {
                            xRefs += "<p>And/Or</p>"
                        }
                        xRefs += createLinkFromItem(childItem, dataDictionary)
                    }
                }
                return xRefs
            }
        }

        String addSubClass(DataClass dataClass, DataDictionary dataDictionary) {

            StringBuffer result = new StringBuffer()

            if (dataClass.label != "Choice") {
                result.append "<tr class='table-primary'><td><b>Mandation</b></td>"
                result.append "<td><b>${dataClass.label}</b>"
                if (dataClass.description) {
                    result.append "<p>${dataClass.description}</p>"
                }
                result.append "</td></tr>"

            }
            result.append addAllChildRows(dataClass, dataDictionary)
            return result
        }

        String createLinkFromDataElement(DataElement dataElement, DataDictionary dataDictionary) {
            String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
            DDElement ddElement = dataDictionary.elements[uin]
            String retValue
            if (ddElement) {
                retValue = "<p><a class='element' href='#/preview/element/${ddElement.catalogueItem.id.toString()}'></a>${ddElement.name}</p>"
            } else {
                log.debug("Cannot find uin: ${dataElement.label}, ${uin}")
                retValue = "<p>${dataElement.label}</p>"
            }
            if (dataElement.maxMultiplicity == -1) {
                retValue += "<p>Multiple occurrences of this item are permitted</p>"
            }
            return retValue
        }

        List<CatalogueItem> getSortedChildElements(DataClass dataClass) {
            List<CatalogueItem> children = []
            if (dataClass.dataElements) {
                children.addAll(dataClass.dataElements)
            }
            if (dataClass.dataClasses) {
                children.addAll(dataClass.dataClasses)
            }

            return children.sort {
                child -> Integer.parseInt(DDHelperFunctions.getMetadataValue(child, "Web Order") ?: "0")
            }

        }

        String outputCDSClassAsDita(DataClass dataClass, DataDictionary dataDictionary, DataModel dataModel) {

            int totalDepth = calculateClassDepth(dataClass)
            StringBuffer result = new StringBuffer()
            if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
                result.append "<p> <b>One of the following options must be used:</b></p>"
                dataClass.dataClasses
                    .sort {childDataClass -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataClass, "Web Order") ?: "0")}
                    .eachWithIndex {childDataClass, idx ->
                        result.append outputCDSClassAsDita(childDataClass, dataDictionary, dataModel)
                        if (idx < dataClass.dataClasses.size() - 1) {
                            result.append "<p> <b>Or</b></p>"
                        }
                    }


            } else if (!dataClass.dataClasses && !dataClass.dataElements && DataSetParser.isDataSetReference(dataClass)) {
                // We've got a pointer to another dataset
                //String linkedDataSetUrl = DataSetParser.getDataSetReferenceTo(dataClass)
                //log.debug(dataClass.label.replaceFirst("DATA GROUP: ", ""))
                DDDataSet linkedDataSet = dataDictionary.dataSets.values().find {
                    it.ddUrl == DataSetParser.getDataSetReferenceTo(dataClass)
                }
                if (!linkedDataSet) {
                    log.debug("Cannot link dataset: ${DataSetParser.getDataSetReferenceTo(dataClass)}")
                }
                result.append "<table>"
                result.append addTopLevelCDSHeader(dataClass, linkedDataSet.name, dataClass.description, totalDepth, false)

                result.append "<tr>"
                result.append "<td style='text-align: center'>${DataSetParser.getMRO(dataClass)}</td>"
                result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(dataClass)}</td>"
                result.append "<td colspan='${totalDepth * 2 + 3}'><p> <b>Data Group:</b><p>" +
                              "<a href='#/preview/dataSet/${linkedDataSet.catalogueItem.id}' class='dataSet'>${linkedDataSet.name}</a></p></td>"
                result.append "</table>"
            } else {
                String label = dataClass.label
                if (label.endsWith(" 1")) {
                    label = label.replaceAll(" 1", "")
                }
                result.append "<table>"
                result.append addTopLevelCDSHeader(dataClass, label, dataClass.description, totalDepth)
                result.append "<tbody>"
                result.append addCDSClassContents(dataClass, dataModel, dataDictionary, totalDepth, 0)
                result.append "</tbody>"
                result.append "</table>"

            }
        }

        int calculateClassDepth(DataClass dataClass) {
            if (dataClass.dataElements) {
                return 0
            } else {
                int maxDepth = 0
                dataClass.dataClasses.each {childDataClass ->
                    int thisDepth = calculateClassDepth(childDataClass)
                    if (thisDepth > maxDepth) {
                        maxDepth = thisDepth
                    }
                }
                return maxDepth + 1
            }

        }

        int calculateClassRows(DataClass dataClass) {

            if (DataSetParser.isChoice(dataClass) && (!dataClass.dataClasses || dataClass.dataClasses.size() == 0)) {
                return 0 // nominal header row will be added later on
            }
            int total = 0
            if (dataClass.dataElements) {
                total += dataClass.dataElements.size()
            }

            dataClass.dataClasses.each {
                total += calculateClassRows(it) + 1
            }
            if (DataSetParser.isChoice(dataClass) && dataClass.dataClasses && dataClass.dataClasses.size() > 0) {
                // To cope with the 'or' rows
                total += dataClass.dataClasses.size()
            }

            log.debug("Data Class Rows: ${dataClass.label} - ${total}")
            return total

        }

        String addTopLevelCDSHeader(DataClass dataClass, String groupName, String function, int totalDepth, boolean includeMRO = true) {
            StringBuffer result = new StringBuffer()
            result.append "<thead>"
            result.append "<tr>"
            result.append "<td colspan='2'>Notation</td>"
            result.append "<td colspan='${totalDepth * 2 + 2}'><p> <b>Data Group:</b> ${groupName}</p></td>"
            result.append "</tr>"
            result.append "<tr class='thead-light'>"
            String groupStatusString = "<p>Group Status</p>"
            if (includeMRO) {
                groupStatusString += "<p>${DataSetParser.getMRO(dataClass) ?: ""}</p>"
            }
            String groupRepeatsString = "<p>Group Repeats</p>"
            if (includeMRO) {
                groupRepeatsString += "<p>${DataSetParser.getGroupRepeats(dataClass) ?: ""}</p>"
            }
            result.append "<td colspan='${totalDepth * 2}'>${groupStatusString}</td>"
            result.append "<td colspan='${totalDepth * 2}'>${groupRepeatsString}</td>"
            result.append "<td colspan='${totalDepth * 2 + 1}'><p> <b>Function:</b> ${function}</p></td>"
            result.append "</tr>"
            result.append "</thead>"
            return result
        }

        String addCDSClassContents(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth, int currentDepth) {

            StringBuffer result = new StringBuffer()

            if (dataClass.dataElements) {
                getSortedChildElements(dataClass).each {child ->
                    result.append addCDSChildRow(child, dataDictionary, totalDepth, currentDepth)
                }
            } else {
                // no dataElements
                if (dataClass.dataClasses) {
                    if (DataSetParser.isChoice(dataClass)) {
                        result.append "<tr><td><p> <b>One of the following DATA GROUPS must be used:</b></p></td></tr>"
                    }
                    List<DataClass> sortedClasses = getSortedChildElements(dataClass) as List<DataClass>
                    sortedClasses.each {childDataClass ->
                        result.append addCDSSubClass(childDataClass, dataModel, dataDictionary, totalDepth, currentDepth + 1)
                        if (DataSetParser.isChoice(dataClass) &&
                            childDataClass != sortedClasses.last()) {
                            result.append "<tr><td><p> <b>Or</b></p></td></tr>"
                        }
                    }
                } else {
                    log.debug("No data elements or data classes... ")
                    log.debug(dataModel.label)
                    log.debug(dataClass.label)
                }
            }
            return result.toString()
        }

        String addCDSSubClass(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth,
                              int currentDepth) {
            StringBuffer result = new StringBuffer()
            result.append addCDSClassHeader(dataClass, dataModel, dataDictionary, totalDepth, currentDepth)
            result.append addCDSClassContents(dataClass, dataModel, dataDictionary, totalDepth, currentDepth)
            return result.toString()
        }

        String addCDSClassHeader(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth,
                                 int currentDepth) {
            int moreRows = calculateClassRows(dataClass)
            StringBuffer result = new StringBuffer()
            String name = dataClass.label
            if (name.endsWith(" 1")) {
                name = name.replace(" 1", "")
            }
            String nameEntry = "<b>${name}</b>"
            if (dataClass.description) {
                String description = dataClass.description
                description = description
                    .replace(
                        "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/nhs_number_status_indicator_code_(baby)" +
                        ".html\">NHS_NUMBER_STATUS_INDICATOR_CODE_(BABY)</a>",
                        "<xref keyref=\"element_nhs_number_status_indicator_code__baby_\">NHS STATUS INDICATOR CODE (BABY)</xref>")
                    .replace("<a href=\"https://v3.datadictionary.nhs.uk/web_site_content/supporting_definitions/secondary_uses_service.html\">Secondary_Uses_Service</a>",
                             "<xref keyref=\"supporting_definition_secondary_uses_service\">Secondary Uses Service</xref>")
                    .replace(
                        "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/nhs_number_status_indicator_code.html\">NHS_NUMBER_STATUS_INDICATOR_CODE</a>",
                        "<xref keyref=\"element_nhs_number_status_indicator_code\">NHS NUMBER STATUS INDICATOR CODE</xref>")
                    .replace(
                        "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/messages/cds_v6-2_type_003_-_cds_message_header.html\">CDS V6-2 Type 003 - Commissioning " +
                        "Data Set Message Header</a>",
                        "<xref keyref=\"dataset_cds_v6-2_type_003_-_cds_message_header\">CDS V6-2 Type 003 - Commissioning Data Set Message Header</xref>")
                nameEntry += "<p>${description}</p>"
            }
            result.append "<tr class='thead-light table-primary'>"
            result.append "<td rowspan='${moreRows + 1}'>${DataSetParser.getMRO(dataClass)}</td>"
            result.append "<td rowspan='${moreRows + 1}'>${DataSetParser.getGroupRepeats(dataClass)}</td>"
            result.append "<td colspan='3'><p>${nameEntry}</p></td>"
            result.append "<td>Rules</td>"

            return result
        }

        String addCDSChildRow(CatalogueItem child, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
            StringBuffer result = new StringBuffer()
            if (child instanceof DataElement) {
                String uin = DDHelperFunctions.getMetadataValue(child, "uin")
                if (uin) {
                    DDElement ddElement = dataDictionary.elements[uin]
                    //Row elementRow = new Row(entries: [])
                    if (ddElement) {
                        result.append "<tr>"
                        result.append "<td style='text-align: center'>${DataSetParser.getMRO(child)}</td>"
                        result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(child)}</td>"
                        result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>" +
                                      "<a href='#/preview/elements/${ddElement.catalogueItem.id.toString()}'>${ddElement.name}</a></td>"
                        result.append "<td style='text-align: center'>${DataSetParser.getRules(child).replaceAll("\\n", "<br/>")}</td>"
                        result.append "</tr>"

                    } else {
                        result.append "<tr>"
                        result.append "<td style='text-align: center'>${DataSetParser.getMRO(child)}</td>"
                        result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(child)}</td>"
                        result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>${child.label}</td>"
                        result.append "<td style='text-align: center'>${DataSetParser.getRules(child).replaceAll("\\n", "<br/>")}</td>"
                        result.append "</tr>"
                    }
                } else {
                    log.debug("Cannot find uin: " + child.label)
                }
            } else if (child instanceof DataClass) {
                if (DataSetParser.isChoice(child) && child.label.startsWith("Choice")) {
                    //Row elementRow = new Row(entries: [])
                    String mro = ""
                    String groupRepeats = ""
                    String xRefs = ""
                    String rules = ""
                    ((DataClass) child).dataElements
                        .sort {childDataElement -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataElement, "Web Order"))}
                        .eachWithIndex {childDataElement, idx ->
                            String uin = DDHelperFunctions.getMetadataValue(childDataElement, "uin")
                            DDElement ddElement = dataDictionary.elements[uin]
                            if (idx != 0) {
                                xRefs += "<p>Or</p>"
                                mro += "<p>Or</p>"
                                groupRepeats += "<p>Or</p>"
                                rules += "<p>-</p>"
                            }
                            mro += "<p>${DataSetParser.getMRO(childDataElement)}</p>"
                            groupRepeats += "<p>${DataSetParser.getGroupRepeats(childDataElement)}</p>"
                            rules += "<p>${DataSetParser.getRules(childDataElement)?.replaceAll("\\n", "<br/>") ?: ""}</p>"

                            if (ddElement) {
                                xRefs += "<p><a href='#/preview/elements/${ddElement.catalogueItem.id.toString()}' " +
                                         "class='element'>${ddElement.name}</a></p>"
                            } else {
                                xRefs += "<p>${childDataElement.label}</p>"
                                log.debug("no ddElement! ${uin} ${childDataElement.label}")
                            }
                        }
                    result.append "<tr>"
                    result.append "<td style='text-align: center'>${mro}</td>"
                    result.append "<td style='text-align: center'>${groupRepeats}</td>"
                    result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>${xRefs}</td>"
                    result.append "<td style='text-align: center'>${rules}</td>"
                    result.append "</tr>"

                }
            }
            return result.toString()
        }
    */
    void persistDataSets(NhsDataDictionary dataDictionary, Folder dictionaryFolder, DataModel coreDataModel) {

        Folder dataSetsFolder = getFolderAtPath(dictionaryFolder, [NhsDataDictionary.DATA_SETS_FOLDER_NAME])
        dataDictionary.dataSets.each {name, dataSet ->
            createAndSaveDataModel(dataSet, dataSetsFolder, dictionaryFolder, dataDictionary)
        }
    }

    /*
    void setFolderDescriptions(Folder sourceFolder, List<String> path, NhsDataDictionary nhsDataDictionary) {
        NhsDDWebPage matchingWebPage = nhsDataDictionary.webPages[sourceFolder.label + " Introduction"]
        if(matchingWebPage) {
            sourceFolder.description = matchingWebPage.definition
            sourceFolder.save()
        } else {
            log.warn("Cannot match folder name: " + sourceFolder.label)
        }
        sourceFolder.childFolders.each {childFolder ->
            List<String> newPath = []
            newPath.addAll(path)
            newPath.add(childFolder.label)
            setFolderDescriptions(childFolder, newPath, nhsDataDictionary)
        }
    }
     */

    void createAndSaveDataModel(NhsDDDataSet dataSet, Folder dataSetsFolder, Folder dictionaryFolder,
                                NhsDataDictionary nhsDataDictionary) {
        Folder folder = getFolderAtPath(dataSetsFolder, dataSet.path)

        DataModel dataSetDataModel = new DataModel(
            label: dataSet.name,
            description: dataSet.definition,
            dataModelType: DataModelType.DATA_STANDARD,
            folder: folder,
            branchName: nhsDataDictionary.branchName
        )
        folder.dataModels.add(dataSetDataModel)
        Node definition = xmlParser.parseText(dataSet.definitionAsXml)

        if (dataSet.name.startsWith('CDS') || dataSet.name.startsWith('ECDS') || dataSet.name.startsWith('Emergency Care Data Set')) {
            CDSDataSetParser.parseCDSDataSet(definition, dataSetDataModel, nhsDataDictionary)
        } else {
            DataSetParser.parseDataSet(definition, dataSetDataModel, nhsDataDictionary)
        }
        addMetadataFromComponent(dataSetDataModel, dataSet)
    }

/*
    void addDataClassToDataModel(DataClass dataClass, DataModel dataModel) {
        dataModel.childDataClasses.add(dataClass)
        dataClass.dataClasses.each {childDataClass -> addDataClassToDataModel(childDataClass, dataModel)}
    }
*/

    NhsDDDataSet getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.dataSets.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}
