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
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.exception.MauroApplicationException
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDCode
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement, NhsDDAttribute> {

    ElementService elementService
    ClassService classService

    @Override
    NhsDDAttribute show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        DataElement attributeElement = dataElementService.get(id)
        NhsDDAttribute attribute = getNhsDataDictionaryComponentFromCatalogueItem(attributeElement, dataDictionary)
        attribute.instantiatedByElements.addAll (elementService.getAllForAttribute(dataDictionary, attribute))
        attribute.definition = convertLinksInDescription(versionedFolderId, attribute.getDescription())
        attribute.codes.each {code ->
            if(code.webPresentation) {
                code.webPresentation = convertLinksInDescription(versionedFolderId, code.webPresentation)
            }
        }
        return attribute
    }

    Set<NhsDDAttribute> getAllForElement(NhsDataDictionary dataDictionary, NhsDDElement nhsDDElement) {
        List<String> linkedAttributeList = elementService.getLinkedAttributes(nhsDDElement.catalogueItem)

        nhsDDElement.catalogueItem.semanticLinks
            .collect {link -> DataElement.get(link.targetMultiFacetAwareItemId) }
            .collect {dataElement ->
                dataElement.getMetadata().size() // For later getting retired property
                getNhsDataDictionaryComponentFromCatalogueItem(dataElement, dataDictionary)
            }
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }
    }


    @Override
    Set<DataElement> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        DataModel classesModel = nhsDataDictionaryService.getClassesModel(versionedFolderId)
        List<DataElement> attributes = DataElement.byDataModelId(classesModel.id).list()
        attributes.findAll {dataElement ->
            //dataElement.metadata.size()
            !(dataElement.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE) &&
                    (includeRetired || !catalogueItemIsRetired(dataElement))
        }

    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".attribute"
    }

    @Override
    NhsDDAttribute getNhsDataDictionaryComponentFromCatalogueItem(DataElement catalogueItem, NhsDataDictionary dataDictionary, List<Metadata> metadata = null) {
        NhsDDAttribute attribute = new NhsDDAttribute()
        nhsDataDictionaryComponentFromItem(dataDictionary, catalogueItem, attribute, metadata)
        if (catalogueItem.dataType.dataTypeKind == DataType.DataTypeKind.MODEL_TYPE) {
            List<Term> terms = termService.findAllByTerminologyId(((DataType) catalogueItem.dataType).modelResourceId)
            List<NhsDDCode> codes = getCodesForTerms(terms, dataDictionary)
            codes.each {code ->
                code.owningAttribute = attribute
                attribute.codes.add(code)
            }
        }
        attribute.parentClass = classService.getNhsDataDictionaryComponentFromCatalogueItem(catalogueItem.dataClass, dataDictionary, metadata)
        attribute.dataDictionary = dataDictionary
        return attribute

    }

    void persistAttributes(NhsDataDictionary dataDictionary,
                           Folder dictionaryFolder, DataModel classesDataModel, String currentUserEmailAddress,
                           Map<String, Terminology> attributeTerminologiesByName, Map<String, DataClass> attributeClassesByUin, Set<String> attributeUinIsKey) {

        Folder attributeTerminologiesFolder =
            new Folder(label: "Attribute Terminologies", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(attributeTerminologiesFolder)

        if (!folderService.validate(attributeTerminologiesFolder)) {
            throw new MauroApplicationException('NHSDD', 'Invalid model', attributeTerminologiesFolder.errors)
        }
        folderService.save(attributeTerminologiesFolder)

        DataType stringDataType = classesDataModel.getPrimitiveTypes().find {it.label == "String"}
        if (!stringDataType) {
            stringDataType = new DataType(label: "String", createdBy: currentUserEmailAddress)
            classesDataModel.addToDataTypes(stringDataType)
        }
        DataClass retiredDataClass = classesDataModel.childDataClasses.find { it.label == "Retired"}

        List<Terminology> terminologies = []
        dataDictionary.attributes.each { name, attribute ->

            if (attribute.codes.size() > 0) {
                Terminology terminology = createAttributeTerminology(
                        name, attribute,
                        attributeTerminologiesFolder,
                        currentUserEmailAddress,
                        dataDictionary
                )
                terminologies.add(terminology)
            }
        }
        terminologies = terminologyService.saveModelsWithContent(terminologies, 1000)
        attributeTerminologiesByName.putAll(terminologies.collectEntries{ [it.label, it]})

        //int idx = 0
        dataDictionary.attributes.each { name, attribute ->
            DataType dataType = stringDataType

            if (attribute.codes.size() > 0) {
                dataType = createAttributeTerminologyType(
                        name,
                        attributeTerminologiesByName[name],
                        currentUserEmailAddress,
                        classesDataModel)
            }

            DataElement attributeDataElement = new DataElement(
                label: name,
                description: attribute.definition,
                createdBy: currentUserEmailAddress,
                dataType: dataType)

            addMetadataFromComponent(attributeDataElement, attribute, currentUserEmailAddress)
            DataClass parentClass = attributeClassesByUin[attribute.uin]
            if(!parentClass) {
                if(!attribute.isRetired()) {
                    log.error("Attribute ${name} is not retired, but doesn't have a class!")
                }
                parentClass = retiredDataClass
            }
            parentClass.addToDataElements(attributeDataElement)

            if(attributeUinIsKey.contains(attribute.uin)) {
                addToMetadata(attributeDataElement, "isKey", attributeUinIsKey.contains(attribute.uin).toString(), currentUserEmailAddress)
            }

            // Reload all terms into the session
            attributeTerminologiesByName.each {k, v ->
                v.terms.size()
            }
            attribute.catalogueItem = attributeDataElement
        }

    }

    NhsDDAttribute getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.attributes.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

    Terminology createAttributeTerminology(
            String attributeName,
            NhsDDAttribute attribute,
            Folder attributeTerminologiesFolder,
            String currentUserEmailAddress,
            NhsDataDictionary dataDictionary
        ) {

        Folder subFolder = DDHelperFunctions.getSubfolderFromName(folderService, attributeTerminologiesFolder, attributeName, currentUserEmailAddress)
        // attributeTerminologiesFolder.save()

        Terminology terminology = new Terminology(
                label: attributeName,
                folder: subFolder,
                createdBy: currentUserEmailAddress,
                authority: authorityService.defaultAuthority,
                branchName: dataDictionary.branchName)
        terminology.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.terminology", key: "version", value: attribute.codesVersion))

        attribute.codes.each { code ->
            Term term = new Term(
                    code: code.code,
                    definition: code.definition,
                    createdBy: currentUserEmailAddress,
                    label: "${code.code} : ${code.definition}",
                    depth: 1,
                    terminology: terminology
            )

            code.propertiesAsMap().each { key, value ->
                if (value) {
                    term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                            key: key,
                            value: value,
                            createdBy: currentUserEmailAddress))
                }
            }
            terminology.addToTerms(term)

        }
        if (!terminologyService.validate(terminology)) {
            GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
            log.error("Cannot save terminology: ${attributeName}")
        }

        //nhsDataDictionary.attributeTerminologiesByName[name] = terminology
        return terminology
    }

    DataType createAttributeTerminologyType(String name, Terminology terminology, String currentUserEmailAddress, DataModel classesDataModel) {
        DataType dataType = new DataType(label: "${name} Attribute Type",
                modelResourceDomainType: terminology.getDomainType(),
                modelResourceId: terminology.id,
                dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                createdBy: currentUserEmailAddress)
        classesDataModel.addToDataTypes(dataType)
        return dataType
    }


}
