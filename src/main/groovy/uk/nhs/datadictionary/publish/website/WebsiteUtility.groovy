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
package uk.nhs.datadictionary.publish.website

import groovy.util.logging.Slf4j
import org.maurodata.dita.DitaProject
import org.maurodata.dita.elements.langref.base.DitaMap
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.elements.langref.base.TopicRef
import org.maurodata.dita.elements.langref.base.TopicSet
import org.maurodata.dita.enums.Linking
import org.maurodata.dita.enums.Toc
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent

import java.nio.file.Path

@Slf4j
class WebsiteUtility {

    static final String TO_BE_OVERRIDDEN_TEXT = "This text should be overridden by custom text stored in a GitHub library"

    static byte[] generateWebsite(NhsDataDictionary dataDictionary, DataDictionaryImportParameters parameters) {
        DitaProject ditaProject = new DitaProject("NHS Data Model and Dictionary", "nhs_data_dictionary")
        ditaProject.useTopicsFolder = false


        dataDictionary.getAllComponents().
            sort { it.ditaKey }.
            each { component ->
            component.dataDictionary = dataDictionary
            ditaProject.addExternalKey(component.getDitaKey(), component.otherProperties["ddUrl"])
            dataDictionary.pathLookup[component.getMauroPath()] = component
        }

        dataDictionary.getAllComponents().each { component ->
            if(!component.isCommissioningDataSetFolder()) {
                component.calculateWhereUsed()
            }
        }

//        dataDictionary.allComponents.each {component ->
//            component.replaceLinksInDefinition(pathLookup)
//            component.updateWhereUsed()
//        }


//        allStereotypes.each {name, stereotype ->
//
//            ditaProject.registerMap('', stereotype, "All ${name}", Toc.NO)
//        }


        if(!parameters.omitDataSetFolders && !parameters.omitDataSets) {
            DataSetsWebsiteHelper.dataSetsIndex(dataDictionary, parameters, ditaProject)
        }

        generateIndexTopics(dataDictionary, ditaProject, parameters)
        generateAllItemsIndex(dataDictionary, ditaProject, parameters)

        dataDictionary.allComponents.
            sort {it.name }.
            each {component ->
                if(!(component instanceof NhsDDDataSet || component instanceof  NhsDDDataSetFolder)) {
                    if (parameters.isPublishableComponent(component)) {
                        String path = "${component.getPluralStereotypeForWebsite()}"
                        ditaProject.registerTopic(path, component.generateTopic(), component.getNameWithoutNonAlphaNumerics().toLowerCase())
                    }
                }
            }

        Map<String, ByteArrayOutputStream> staticContentMap = GitHubStaticContentHelperService.getGithubDirAsMap()
        ByteArrayOutputStream zipContents = ditaProject.writeToZip(staticContentMap)


        return zipContents.toByteArray()
    }



    static Map<String, Topic> getFlatIndexTopics(Map<String, List<NhsDataDictionaryComponent>> componentMap, String indexPrefix) {

        return componentMap.collectEntries {alphaIndex, componentList ->
            [alphaIndex, Topic.build (id: "${indexPrefix}-index-${alphaIndex}"){
                title alphaIndex.toUpperCase()
                body {
                    simpletable(relColWidth: ["10*"], outputClass: "table table-sm table-striped") {
                        stHead(outputClass: "thead-light") {
                            stentry "Item Name"
                        }
                        componentList.each {component ->
                            strow {
                                stentry {
                                    xRef component.calculateXRef()
                                }
                            }
                        }
                    }
                }
            }]
        }
    }

    static void generateIndexTopics(NhsDataDictionary dataDictionary, DitaProject ditaProject, DataDictionaryImportParameters parameters) {

        if(!parameters.omitElements) {
            generateIndexMap(ditaProject, "data_elements", "Elements", dataDictionary, dataDictionary.elements.values())

        }
        if(!parameters.omitAttributes) {
            generateIndexMap(ditaProject, "attributes", "Attributes", dataDictionary, dataDictionary.attributes.values())
        }
        if(!parameters.omitClasses) {
            generateIndexMap(ditaProject, "classes", "Classes", dataDictionary, dataDictionary.classes.values())
        }
        if(!parameters.omitBusinessDefinitions) {
            generateIndexMap(ditaProject, "nhs_business_definitions", "NHS Business Definitions", dataDictionary, dataDictionary.businessDefinitions.values())

        }
        if(!parameters.omitSupportingInformation) {
            generateIndexMap(ditaProject, "supporting_information", "Supporting Information", dataDictionary, dataDictionary.supportingInformation.values())
        }
    }

    static void generateAllItemsIndex(NhsDataDictionary dataDictionary, DitaProject ditaProject, DataDictionaryImportParameters parameters) {

        TopicSet indexTopicSet = TopicSet.build(id: "allItems-index-topicset", keyRef: "allItems-index-overview", navTitle: "All Items Index")

        dataDictionary.allComponentsByIndex(true).
            findAll {!it instanceof NhsDDDataSetFolder }.
            each {alphaIndex, components ->
            String indexId = "all_items__${alphaIndex.substring(0,1).toLowerCase()}"
            Topic indexPage = Topic.build (id: indexId) {
                title "All Items: ${alphaIndex}"
                body {
                    simpletable(relColWidth: ["7*", "3*"], outputClass: "table table-sm table-striped") {
                        stHead(outputClass: "thead-light") {
                            stentry "Item Name"
                            stentry "Item Type"
                        }
                        components.each {component ->
                            strow {
                                stentry {
                                    xRef component.calculateXRef()
                                }
                                stentry component.stereotype
                            }
                        }
                    }
                }
            }
            indexTopicSet.topicRef(TopicRef.build(keyRef: indexId))
            ditaProject.registerTopic("all_items_index__a-z_", indexPage)
        }


        DitaMap indexMap = DitaMap.build(id: 'all_items_index', toc: Toc.YES) {
            title "All Items Index"
            topicSet indexTopicSet
        }
        ditaProject.registerMap("", indexMap)

        Topic allItemsOverview = Topic.build {
            id 'allItems-index-overview'
            title 'All Items Index'
            shortdesc 'Lists all the items in the dictionary in alphabetical order'
            body {
                p TO_BE_OVERRIDDEN_TEXT
            }
        }
        ditaProject.registerTopic("", allItemsOverview, "all_items_index_overview")

        ditaProject.mainMap.mapRef {
            toc Toc.YES
            keyRef indexMap.id
        }
    }

    final static Map<String, String> shortDescMap = [
            "Elements": "Data Elements are the data items used withing Data Sets.",
            "Attributes": "The part of the data model describing the characteristics of Classes.  Attributes define the data within the model.",
            "Classes": "The part of the data model describing the aspects of the health and care business with significant characteristics.",
            "NHS Business Definitions":"The part of the data model that links the logical classes to the context of the health and care business.",
            "Supporting Information": "Provides information to help users understand content in the NHS Data Model and Dictionary. In addition you can also find the COVID-19 Population Risk Assessment Code List and General Practice Data for Planning and Research (GPDPR)."
    ]

    static void generateIndexMap(DitaProject ditaProject,
                     String lowercaseStereotype, String stereotype, NhsDataDictionary dataDictionary,
                     Collection<NhsDataDictionaryComponent> components) {
        DitaMap indexMap = DitaMap.build(id: "${lowercaseStereotype}-index") {
            title stereotype
        }

        Topic indexOverview = Topic.build {
            id "${lowercaseStereotype}-index-overview"
            title "${stereotype}"
            shortdesc shortDescMap[stereotype]?:"List of ${stereotype}"
            body {
                p TO_BE_OVERRIDDEN_TEXT
            }
        }
        ditaProject.registerTopic("", indexOverview, "${lowercaseStereotype}_overview")


        TopicRef topicSet = TopicRef.build(keys: ["${lowercaseStereotype}-index-page"],
                                           href: "../${lowercaseStereotype}_overview.dita",
                                           copyTo: "${lowercaseStereotype}-index.dita",
                                           chunk: ["to-content"],
                                           linking: Linking.NORMAL,
                                           navTitle: stereotype)
        indexMap.topicRef(topicSet)

        Map<String, Topic> indexTopics = getFlatIndexTopics(dataDictionary.componentsByIndex(components, false),
                                                            lowercaseStereotype)

        indexTopics.each {prefix, topic ->
            ditaProject.registerTopic(lowercaseStereotype, topic, prefix.toLowerCase())
            topicSet.topicRef(keyRef: topic.id, linking: Linking.NORMAL, toc: Toc.YES)
        }
        ditaProject.registerMap("", indexMap)
        ditaProject.mainMap.mapRef {
            toc Toc.YES
            keyRef indexMap.id
        }
    }

    private static void moveDirectory(File parentFrom, File parentTo) {
        log.warn("Moving " + parentFrom.toPath() + " to " + parentTo)

        for (File file : parentFrom.listFiles()) {

            // Is a regular file?
            if (!file.isDirectory()) { // Is a regular file
                File newName = new File(parentTo, file.getName())
                file.renameTo(newName)
                log.warn("Moved " + file.getAbsolutePath() + " to " + newName.getAbsolutePath())
            } else { // Is a directory
                File newName = new File(parentTo, file.getName())
                newName.mkdirs()
                log.warn("Moving dir " + file.getAbsolutePath() + " to " + newName.getAbsolutePath())
                moveDirectory(file, newName)
            }
        }
    }

}
