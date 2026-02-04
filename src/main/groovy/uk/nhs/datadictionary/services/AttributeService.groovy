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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SemanticLinkType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.SemanticLinkCacheableRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.utils.DDHelperFunctions

@Slf4j
@Transactional
@Singleton
@CompileStatic
class AttributeService extends DataDictionaryComponentService<DataElement, NhsDDAttribute> {

    @Inject ClassService classService

    @Inject AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository

    @Inject SemanticLinkCacheableRepository semanticLinkCacheableRepository

    String getStereotype() {
        return 'attribute'
    }

    @Override
    NhsDDAttribute show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        DataElement attributeElement = dataElementRepository.findById(id)
        attributeElement.semanticLinks.each {
            it.target = dataElementRepository.loadWithContent(it.targetMultiFacetAwareItemId)
        }
        NhsDDAttribute attribute = initialiseComponent(new NhsDDAttribute(), attributeElement, versionedFolderId)
        attribute.instantiatedByElements.addAll (getAllElementsForAttribute(null, attribute))
        //attribute.htmlDescription = convertLinksInDescription(versionedFolderId, attribute.getDescription())
        attribute.codes.each {code ->
            if(code.webPresentation) {
                code.webPresentation = convertLinksInDescription(versionedFolderId, code.webPresentation)
            }
        }
        // ensure no recursion
        attribute.instantiatedByElements.each { element ->
            element.codes = []
        }
        attribute.codes.each {code ->
            code.usedByElements = []
            code.owningAttribute = null
        }

        return attribute
    }

    List<NhsDDElement> getAllElementsForAttribute(NhsDataDictionary dataDictionary, NhsDDAttribute nhsDDAttribute) {
        semanticLinkCacheableRepository.readAllByTargetMultiFacetAwareItemId(nhsDDAttribute.catalogueItem.id)
        .findAll {semanticLink ->
            semanticLink.linkType == SemanticLinkType.REFINES
        }.collect {semanticLink ->
            if(dataDictionary) {
                return dataDictionary.elementsByCatalogueId[semanticLink.multiFacetAwareItemId]
            } else {
                DataElement dataElement = dataElementRepository.findById(semanticLink.multiFacetAwareItemId)
                return new NhsDDElement(dataElement).fromMauroItem(dataDictionary, mauroPersistenceService, dataElement)
            }
        }.findAll{
            !it.isRetired()
        }.sort {it.name} as List<NhsDDElement>
    }


    @Override
    Set<DataElement> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        DataModel classesModel = nhsDataDictionaryService.getClassesModel(versionedFolderId)
        return classesModel.dataElements.findAll { dataElement ->
            !(dataElement.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE) &&
            (includeRetired || !catalogueItemIsRetired(dataElement))
        } as Set
    }


    void createAttributes(NhsDataDictionary dataDictionary,
                           Folder dictionaryFolder, DataModel classesDataModel,
                           Map<String, Terminology> attributeTerminologiesByName, Map<String, DataClass> attributeClassesByUin, Set<String> attributeUinIsKey) {

        Folder attributeTerminologiesFolder = new Folder(label: NhsDataDictionary.ATTRIBUTE_TERMINOLOGIES_FOLDER_NAME)
        dictionaryFolder.childFolders.add(attributeTerminologiesFolder)

        DataType stringDataType = classesDataModel.dataTypes.find {it.label == "String"}
        if (!stringDataType) {
            stringDataType = new DataType(label: "String", dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)
            classesDataModel.dataTypes.add(stringDataType)
        }
        DataClass retiredDataClass = classesDataModel.dataClasses.find { it.label == "Retired"}

        List<Terminology> terminologies = []
        dataDictionary.attributes.each { name, attribute ->
            attribute.codes.each {

            }
            if (attribute.codes.size() > 0) {
                Terminology terminology = createAttributeTerminology(
                        name, attribute,
                        attributeTerminologiesFolder,
                        dataDictionary
                )
                terminologies.add(terminology)
            }
        }
        attributeTerminologiesByName.putAll(terminologies.collectEntries{ [it.label, it]})

        //int idx = 0
        dataDictionary.attributes.each { name, attribute ->
            DataType dataType = stringDataType

            if (attribute.codes.size() > 0) {
                dataType = createAttributeTerminologyType(
                        name,
                        attributeTerminologiesByName[name],
                        classesDataModel)
            }

            DataElement attributeDataElement = new DataElement(
                label: name,
                description: attribute.description,
                dataType: dataType)

            addMetadataFromComponent(attributeDataElement, attribute)
            DataClass parentClass = attributeClassesByUin[attribute.uin]
            if(!parentClass) {
                if(!attribute.isRetired()) {
                    log.error("Attribute ${name} is not retired, but doesn't have a class!")
                }
                parentClass = retiredDataClass
            }
            parentClass.dataElements.add(attributeDataElement)

            if(attributeUinIsKey.contains(attribute.uin)) {
                addToMetadata(attributeDataElement, attribute.getMetadataNamespace(), "isKey", attributeUinIsKey.contains(attribute.uin).toString())
            }

            // Reload all terms into the session
            attribute.catalogueItem = attributeDataElement
        }

    }

    NhsDDAttribute getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.attributes.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

    static Terminology createAttributeTerminology(
            String attributeName,
            NhsDDAttribute attribute,
            Folder attributeTerminologiesFolder,
            NhsDataDictionary dataDictionary
        ) {

        Folder subFolder = DDHelperFunctions.getSubfolderFromName(attributeTerminologiesFolder, attributeName)
        // attributeTerminologiesFolder.save()

        Terminology terminology = new Terminology(
                label: attributeName,
                folder: subFolder)
        subFolder.terminologies.add(terminology)
        if(attribute.codesVersion) {
            terminology.metadata("uk.nhs.datadictionary.terminology", "version", attribute.codesVersion)
        }

        attribute.codes.each { code ->
            Term term = new Term(
                    code: code.code,
                    definition: code.definition,
                    label: "${code.code} : ${code.definition}",
                    depth: 1,
                    terminology: terminology
            )

            code.propertiesAsMap().each { key, value ->
                if (value) {
                    term.metadata("uk.nhs.datadictionary.term", key, value)
                }
            }
            terminology.terms.add(term)
        }

        //nhsDataDictionary.attributeTerminologiesByName[name] = terminology
        return terminology
    }

    static DataType createAttributeTerminologyType(String name, Terminology terminology, DataModel classesDataModel) {
        DataType dataType = new DataType(label: "${name} Attribute Type",
                modelResourceDomainType: terminology.getDomainType(),
                modelResource: terminology,
                dataTypeKind: DataType.DataTypeKind.MODEL_TYPE)
        classesDataModel.dataTypes.add(dataType)
        return dataType
    }


}
