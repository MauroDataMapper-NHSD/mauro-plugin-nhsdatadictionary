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
package uk.nhs.datadictionary


import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.facet.Metadata
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.datasets.DataSetSection
import uk.nhs.datadictionary.publish.structure.datasets.cds.LegacyCdsDataSetSection
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetTable
import uk.nhs.datadictionary.services.profiles.MauroPersistenceService

@Slf4j
class NhsDDDataSet extends NhsDataDictionaryComponent <DataModel> {



    @Override
    String getStereotype() {
        "Data Set"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "data_sets"
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".data set"
    }

    List<String> path = []
    String definitionAsXml
    String htmlStructure

    List<NhsDDDataSetClass> dataSetClasses = []

    List<NhsDDDataSetClass> getSortedDataSetClasses() {
        dataSetClasses.sort { it.webOrder }
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        super.fromXml(xml, dataDictionary)
        NhsDDWebPage explanatoryWebPage = dataDictionary.webPagesByUin[xml.explanatoryPage.text()]
        if(explanatoryWebPage) {
            definition = explanatoryWebPage.definition
        } else {
            if(isRetired()) {
                log.info("Cannot find explanatory page for dataset: {}", name)
            } else {
                log.warn("Cannot find explanatory page for dataset: {}", name)
            }
        }
        path = getWebPath()
        path.removeLast()
        definitionAsXml = XmlUtil.serialize(xml.definition[0])
        otherProperties["approvingOrganisation"] = "Data Alliance Partnership Board (DAPB)"
    }

    @Override
    String calculateShortDescription() {
        if(!definition || definition == "") {
            return name
        }
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            List<String> aliases = [name]
            aliases.addAll(getAliases().values())
            if (path && !path.empty) {
                aliases.add(path.last())
            }

            try {

                List<String> allSentences = calculateSentences(definition)
                if(isRetired()) {
                    return allSentences[0]
                } else {
                    return allSentences.find {sentence ->
                        aliases.find {alias ->
                            sentence.contains(alias)
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDDataSet"
    }


    String getMauroPath() {
        "dm:${name}"
    }

    @Override
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = DictionaryItem.create(this)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            addDataSetSection(dictionaryItem)
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addDataSetSection(DictionaryItem dictionaryItem) {
        boolean isCdsDataSet = useCdsClassRender()

        if (isCdsDataSet) {
            // Handle legacy case for CDS data sets. Maybe one day rebuild this...
            dictionaryItem.addSection(new LegacyCdsDataSetSection(dictionaryItem, this))
            return
        }

        List<OtherDataSetTable> tables = sortedDataSetClasses.collect {dataSetClass ->
            dataSetClass.buildOtherDataSetTable()
        }

        dictionaryItem.addSection(new DataSetSection(dictionaryItem, tables))
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())

        if (isActivePage()) {
            if (getDataSetClasses()) {
                topics.add(specificationTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
        }

        topics.add(changeLogTopic())
        return topics
    }

    Topic specificationTopic() {
        log.info("Generating specification for: ${name}")
        Topic.build {
            title "Specification"
            id getDitaKey() + "_specification"
            body {
                getDataSetClasses().sort { it.webOrder }.each { dataSetClass ->
                    if (useCdsClassRender()) {
                        div dataSetClass.outputCDSClassAsDita(dataDictionary)
                    } else {
                        div dataSetClass.outputClassAsDita(dataDictionary)
                    }
                }
            }
        }
    }

    List<String> getDitaFolderPath() {
        webPath.collect {
            it.replaceAll("[^A-Za-z0-9 ]", "").replace(" ", "_")
        }
    }


    Set<NhsDDDataSetElement> getAllElements() {
        Set<NhsDDDataSetElement> dataSetElements = [] as Set<NhsDDDataSetElement>
        dataSetClasses.each {dataSetClass ->
            dataSetElements.addAll(dataSetClass.getAllElements())
        }
        return dataSetElements
    }

    boolean useCdsClassRender() {
        name.startsWith('CDS') || name.startsWith('ECDS') || name.startsWith('Emergency Care Data Set')
    }

    @Override
    NhsDataDictionaryComponent<DataModel> fromMauroItem(NhsDataDictionary dataDictionary, MauroPersistenceService mauroPersistenceService, DataModel catalogueItem) {
        super.fromMauroItem(dataDictionary, mauroPersistenceService, catalogueItem)
        this.catalogueItem.childDataClasses.each {dataClass ->
            dataSetClasses.add(new NhsDDDataSetClass(dataClass, dataDictionary))
        }
        dataSetClasses = dataSetClasses.sort { it.webOrder }
        return this
    }

    @Override
    List<Edit> getMergeEditsForChangeLog() {
        // Special code for loading the change log for a Data Set, since all the edit history entries are stored
        // not just in the Data Set (Data Model), but child components too
        DataModel dataModel = catalogueItem as DataModel
        if (!dataModel) {
            return []
        }

        List<Edit> allMergeEdits = []
        loadDataModelMergeEdits(allMergeEdits, dataModel)
        dataModel.dataClasses.each { dataClass ->
            loadDataClassMergeEdits(allMergeEdits, dataClass)
        }

        allMergeEdits.sort { it.dateCreated }
    }

    void loadDataModelMergeEdits(List<Edit> allMergeEdits, DataModel item) {
        log.info("Getting merge edits for data model '$item.label'")
        List<Edit> mergeEdits = item.edits.findAll {
            it.title = EditType.MERGE
        }
        if (mergeEdits.empty) {
            return
        }

        allMergeEdits.addAll(mergeEdits)
    }

    void loadDataClassMergeEdits(List<Edit> allMergeEdits, DataClass item) {
        log.info("Getting merge edits for data class '$item.label'")
        List<Edit> mergeEdits = item.edits.findAll {
            it.title = EditType.MERGE
        }
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
        List<Edit> mergeEdits = item.edits.findAll {
            it.title = EditType.MERGE
        }
        if (mergeEdits.empty) {
            return
        }

        allMergeEdits.addAll(mergeEdits)
    }


}
