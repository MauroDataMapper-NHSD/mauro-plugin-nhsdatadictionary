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
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SemanticLinkType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.datamodel.DataElementRepository
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.services.profiles.DDCodeSetProfileProviderService
import uk.nhs.datadictionary.utils.DDHelperFunctions

@Slf4j
@Singleton
class ElementService extends DataDictionaryComponentService<DataElement, NhsDDElement> {

    @Inject DataElementRepository dataElementRepository

    @Inject
    DDCodeSetProfileProviderService ddCodeSetProfileProviderService

    String getStereotype() {
        "element"
    }


    @Override
    NhsDDElement show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary(versionedFolderId)

        DataElement elementElement = dataElementRepository.loadWithContent(id)
        NhsDDElement element = new NhsDDElement().fromMauroItem(dataDictionary, mauroPersistenceService, elementElement) as NhsDDElement
        element.instantiatesAttributes.addAll(getAllAttributesForElement(dataDictionary, element))
        element.definition = convertLinksInDescription(versionedFolderId, element.getDescription())
        String attributeText = element.getAttributeTextAsHtml()
        if (attributeText) {
            element.previewAttributeText = convertLinksInDescription(versionedFolderId, attributeText)
        }
        element.codes.each {code ->
            if(code.webPresentation) {
                code.webPresentation = convertLinksInDescription(versionedFolderId, code.webPresentation)
            }
        }
        return element
    }

    @Override
    Set<DataElement> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        DataModel coreModel = nhsDataDictionaryService.getElementsModel(versionedFolderId)
        return coreModel.dataElements.findAll {dataElement ->
            includeRetired || !catalogueItemIsRetired(dataElement)
        }
    }

    Set<NhsDDAttribute> getAllAttributesForElement(NhsDataDictionary dataDictionary, NhsDDElement nhsDDElement) {
        nhsDDElement.catalogueItem.semanticLinks.findAll {semanticLink ->
            semanticLink.linkType == SemanticLinkType.REFINES
        }.collect {semanticLink ->
            DataElement dataElement = dataElementRepository.readById(semanticLink.targetMultiFacetAwareItemId)
            new NhsDDAttribute().fromMauroItem(dataDictionary, mauroPersistenceService, dataElement)
        }.findAll{
            !it.isRetired()
        }.sort {it.name} as Set<NhsDDAttribute>

    }


    @Deprecated
    boolean attributeListIncludesName(AdministeredItem catalogueItem, String name) {
        List<String> linkedAttributeList = getLinkedAttributes(catalogueItem)
        if (!linkedAttributeList) {
            return false
        }
        return linkedAttributeList.contains(name)
    }

    List<String> getLinkedAttributes(AdministeredItem catalogueItem) {
        catalogueItem.semanticLinks.collect {link ->
            DataElement.get(link.targetMultiFacetAwareItemId).label
        }
    }

    void persistElements(NhsDataDictionary dataDictionary,
                         Folder dictionaryFolder, DataModel elementsDataModel,
                         Map<String, Terminology> attributeTerminologiesByName) {

        DataType stringDataType = new DataType(label: "String", dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)
        elementsDataModel.dataTypes.add(stringDataType)


        Folder dataElementCodeSetsFolder =
            new Folder(label: "Data Element CodeSets")
        dictionaryFolder.childFolders.add(dataElementCodeSetsFolder)

        DataClass retiredElementsClass = new DataClass(label: "Retired")
        elementsDataModel.dataClasses.add(retiredElementsClass)


        Map<String, Folder> folders = [:]
        int idx = 0
        // Would sort, but assume already sorted
        dataDictionary.elements.each {name, element ->
            DataType dataType
            if(name.contains("OXFORD")) {
                System.err.println(name)
            }
            if (element.codes.size() > 0 && !element.isRetired()) {
                Folder subFolder = DDHelperFunctions.getSubfolderFromName(dataElementCodeSetsFolder, name)

                CodeSet codeSet = new CodeSet(
                    label: name,
                    folder: subFolder)
                subFolder.codeSets.add(codeSet)
                if(element.codeSetVersion) {
                    codeSet.metadata(ddCodeSetProfileProviderService.metadataNamespace, "version", element.codeSetVersion)
                }


                // String terminologyUin = ddDataElement.link.participant.find {it -> it.@role == 'Supplier'}.@referencedUin
                // Terminology attributeTerminology = dataDictionary.attributeTerminologiesByName[terminologyUin]
                element.codes.each {code ->
                    Terminology attributeTerminology = attributeTerminologiesByName[code.owningAttribute.name]
                    if (!attributeTerminology) {
                        log.error("No terminology with name ${code.owningAttribute.name} found for element ${name}")
                    } else {
                        Term t = attributeTerminology.terms.find{term -> term.code == code.code}
                        if (!t) {
                            log.error("Cannot find term: ${code.code}")
                        } else {
                            codeSet.terms.add(t)
                        }
                    }

                }
                dataType = new DataType(label: "${name} Element Type",
                                             modelResourceDomainType: codeSet.getDomainType(),
                                             modelResource: codeSet,
                                             dataTypeKind: DataType.DataTypeKind.MODEL_TYPE)
                elementsDataModel.dataTypes.add(dataType)
            } else {
                // no "value-set" nodes
                dataType = stringDataType
            }
            DataElement elementDataElement = new DataElement(
                label: name,
                description: element.definition,
                dataType: dataType,
                order: idx++)

            addMetadataFromComponent(elementDataElement, element)


            element.instantiatesAttributes.each {attribute ->
                if(attribute.catalogueItem) {
                    SemanticLink semanticLink = new SemanticLink(
                            target: attribute.catalogueItem,
                            linkType: SemanticLinkType.REFINES,
                            multiFacetAwareItem: elementDataElement
                    )
                    elementDataElement.semanticLinks.add(semanticLink)
                }
            }

            //String elementAttributes = StringUtils.join(element.instantiatesAttributes.collect {it.name}, ";")
            //addToMetadata(elementDataElement, "linkedAttributes", elementAttributes, currentUserEmailAddress)

            DataClass parentClass
            if (element.isRetired()) {
                retiredElementsClass.dataElements.add(elementDataElement)
            } else {
                parentClass = DDHelperFunctions.getChildClassFromName(elementsDataModel, name)
                parentClass.dataElements.add(elementDataElement)
            }
            dataDictionary.elementsByUrl[element.otherProperties["ddUrl"]] = elementDataElement
        }

    }

    NhsDDElement getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.elements.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}

/*    @Override
    def show(UUID versionedFolderId, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)

        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataElement, null))
        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "element",
                shortDescription: shortDesc,
                description: description,
                alsoKnownAs: getAliases(dataElement)
        ]
        String formatLength = DDHelperFunctions.getMetadataValue(dataElement, "format-length")

        if(!isRetired(dataElement) && formatLength) {
            result["formatLength"] = formatLength
        }

        List<DataElement> elementAttributes = getElementAttributes(dataElement, dataDictionary)

       if (dataElement.dataType instanceof EnumerationType) {
            List<EnumerationValue> enumValues = ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                enumValue ->
                    String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                    if (webOrderString) {
                        return Integer.parseInt(webOrderString)
                    } else return 0
            }
            List<EnumerationValue> permittedNationalCodes = enumValues.findAll {
                DDHelperFunctions.getMetadataValue(it, "Permitted National Code") == "true"
            }
            if (permittedNationalCodes.size() > 0) {
                String nationalCodesTitle = "permittedNationalCodes"
                if(elementAttributes.size() == 1 &&
                        elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                    List<EnumerationValue> attributeValues =
                            ((EnumerationType)elementAttributes.get(0).dataType).getEnumerationValues()
                    if(attributeValues.size() == permittedNationalCodes.size()) {
                        nationalCodesTitle = "nationalCodes"
                    }
                }

                result[nationalCodesTitle] = generateCodeList(permittedNationalCodes)
            }
            List<EnumerationValue> nationalCodes = enumValues.findAll {
                !DDHelperFunctions.getMetadataValue(it, "Permitted National Code") &&
                        DDHelperFunctions.getMetadataValue(it, "Web Order") != "0"
            }
            if (nationalCodes.size() > 0) {
                String nationalCodesTitle = "permittedNationalCodes"
                if(elementAttributes.size() == 1 &&
                        elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                    Set<EnumerationValue> attributeValues =
                            ((EnumerationType)getElementAttributes(dataElement, dataDictionary).get(0).dataType).getEnumerationValues()
                    if(attributeValues.size() == nationalCodes.size()) {
                        nationalCodesTitle = "nationalCodes"
                    }
                }
                result[nationalCodesTitle] = generateCodeList(nationalCodes)
            }
            List<EnumerationValue> defaultCodes = enumValues.findAll {
                //DDHelperFunctions.getMetadataValue(it, "Default Code") == "true"
                DDHelperFunctions.getMetadataValue(it, "Web Order") == "0"
            }
            if (defaultCodes.size() > 0) {
                result["defaultCodes"] = generateCodeList(defaultCodes)
            }
        }

        if(elementAttributes.size() > 0) {
            List<Map> elementAttributesList = []
            elementAttributes.each {attribute ->
                Map attributeMap = [
                    catalogueId: attribute.id.toString(),
                    name: attribute.label,
                    stereotype: "attribute",
                ]
                elementAttributesList.add(attributeMap)
            }
            result["attributes"] = elementAttributesList
        }
        return result
    }
*/

