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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.util.logging.Slf4j
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import uk.nhs.datadictionary.publish.structure.CodesRow
import uk.nhs.datadictionary.publish.structure.CodesSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.ItemLink
import uk.nhs.datadictionary.publish.structure.ItemLinkListSection
import uk.nhs.datadictionary.services.MauroPersistenceService

@Slf4j
class NhsDDAttribute extends NhsDataDictionaryComponent <DataElement> {



    NhsDDAttribute(DataElement catalogueItem = null, UUID branchId = null) {
        super(catalogueItem, branchId)
    }

    DataElement newCatalogueItem(String name = null) {
        return new DataElement(label: name)
    }


    @Override
    String getStereotype() {
        "Attribute"
    }

    @Override
    String getStereotypeForPreview() {
        "attribute"
    }

    @Override
    String getPluralStereotypeForWebsite() {
        "attributes"
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".attribute"
    }

    /**
     * The {@link NhsDDClass} that this attribute belongs under. Must be set during ingest
     * once all the attributes are mapped to the {@link NhsDataDictionary} and the classes
     * are loaded.
     */
    NhsDDClass parentClass

    @JsonIgnore
    List<NhsDDCode> codes = []

    String codesVersion

    @JsonProperty('dataElements')
    Set<NhsDDElement> instantiatedByElements = [] as Set<NhsDDElement>

    boolean isKey() {
        if (!otherProperties.containsKey("isKey")) {
            return false
        }

        Boolean.parseBoolean(otherProperties["isKey"])
    }

    @Override
    String calculateShortDescription() {
        if (isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                return getFirstSentence()
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        super.fromXml(xml, dataDictionary)

        if (xml."code-system".size() > 0) {
            codesVersion = xml."code-system"[0].Bundle.entry.resource.CodeSystem.version."@value".text()
            xml."code-system"[0].Bundle.entry.resource.CodeSystem.concept.each {Node concept ->
                NhsDDCode code = new NhsDDCode(this)

                code.code = concept.code[0].@value.toString()
                code.definition = concept.display[0].@value.toString()

                code.publishDate = concept.property.find {it.code[0].@value == "Publish Date"}?.valueDateTime[0].@value

                code.isRetired = false
                if(concept.property.find {it.code[0].@value == "Status"}) {
                    code.isRetired = concept.property.find {it.code[0].@value == "Status"}?.valueString[0]?.@value == "retired"
                } else if(concept.property.find {it.code[0].@value == "status"}) {
                    code.isRetired = concept.property.find {it.code[0].@value == "status"}?.valueCode[0]?.@value == "retired"
                }
                if(code.isRetired) {
                    code.retiredDate = concept.property.find {it.code[0].@value == "Retired Date"}?.valueDateTime[0]?.@value
                }
                code.webOrder = Integer.parseInt(concept.property.find {it.code[0].@value == "Web Order"}?.valueInteger[0]?.@value)
                code.webPresentation = unquoteString(concept.property.find {it.code[0].@value == "Web Presentation"}?.valueString?[0]?.@value)
                code.isDefault = (code.webOrder == null || code.webOrder == 0)


                codes.add(code)
            }
            // now clear the web order field if we don't need it
            if(codes.findAll { it.webOrder }.sort {it.code }.code == codes.findAll { it.webOrder }.sort {it.webOrder}.code) {
                codes.each {
                    it.webOrder == null
                }
            }

        }
        dataDictionary.attributesByUin[getUin()] = this
    }

    @Override
    String getXmlNodeName() {
        "DDAttribute"
    }

    static String unquoteString(String input) {
        if(input) {
            return input.
                replaceAll("&quot;", "\"").
                replaceAll("&gt;", ">").
                replaceAll("&lt;", "<").
                replaceAll("&apos;", "'")
        } else {
            return null
        }
    }

    String getMauroPath() {
        if (isRetired()) {
            "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:Retired|de:${name}"
        } else {
            String parentClassName = parentClass ? "dc:${parentClass.name}|" : ""
            return "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|${parentClassName}de:${name}"
        }
    }

    @Override
    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        super.replaceLinksInDefinition(pathLookup)
        codes.each { code ->
            code.webPresentation = replaceLinksInString(code.webPresentation, pathLookup)
        }
    }

    @Override
    @JsonIgnore
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = new DictionaryItem(this, this.branchId)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            addNationalCodesSection(dictionaryItem)
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addLinkedElementsSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addNationalCodesSection(DictionaryItem dictionaryItem) {
        if (!this.codes) {
            return
        }

        List<NhsDDCode> orderedCodes = getNationalCodes()
        List<CodesRow> rows = orderedCodes.collect {code ->
            new CodesRow(code.code, code.description, code.hasWebPresentation())
        }

        dictionaryItem.addSection(CodesSection.createNationalCodes(dictionaryItem, rows))
    }

    void addLinkedElementsSection(DictionaryItem dictionaryItem) {
        if (!instantiatedByElements) {
            return
        }

        List<NhsDDElement> elements = instantiatedByElements
            .<NhsDDElement>findAll { element -> !element.isRetired() }
            .sort { element -> element.name }

        if (elements.empty) {
            return
        }

        List<ItemLink> itemLinks = elements.collect {element -> ItemLink.create(element) }
        dictionaryItem.addSection(ItemLinkListSection.createElementsListSection(dictionaryItem, itemLinks))
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            if (this.codes) {
                topics.add(getNationalCodesTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (instantiatedByElements) {
                Topic linkedElementsTopic = getLinkedElementsTopic()
                if (linkedElementsTopic) {
                    topics.add(linkedElementsTopic)
                }
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    @JsonIgnore
    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = getNationalCodes()
        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", "National Codes", orderedCodes)
    }

    List<NhsDDCode> getNationalCodes() {
        NhsDDCode.sortCodes(codes.findAll { !it.isDefault })
    }

    @JsonIgnore
    Topic getLinkedElementsTopic() {
        def elements = instantiatedByElements
            .<NhsDDElement>findAll { element -> !element.isRetired() }
            .sort { element -> element.name }

        if (elements.empty) {
            return null
        }

        Topic.build (id: getDitaKey() + "_dataElements") {
            title "Data Elements"
            body {
                ul {
                    elements.each { element ->
                        li {
                            xRef (element.calculateXRef())
                        }
                    }
                }
            }
        }
    }

    @Override
    void updateWhereUsed() {
        instantiatedByElements.each { NhsDDElement element ->
            whereUsed[element] = "is the data element of $name".toString()
        }

        whereUsed[this.parentClass] = "has an attribute $name of type $name".toString()
//        dataDictionary.classes.values().each {clazz ->
//            if(clazz.allAttributes().contains(this)) {
//            }
//        }
    }

    @Override
    String getDiscriminator() {
        return name
    }

    @Override
    NhsDDAttribute fromMauroItem(NhsDataDictionary dataDictionary, MauroPersistenceService mauroPersistenceService, DataElement catalogueItem) {
        super.fromMauroItem(dataDictionary, mauroPersistenceService, catalogueItem)
        catalogueItem.dataType = mauroPersistenceService.dataTypeCacheableRepository.findById(catalogueItem.dataType.id)
        if(!dataDictionary) {
            catalogueItem.dataClass = mauroPersistenceService.dataClassCacheableRepository.findById(catalogueItem.dataClass.id)
        }
        if(catalogueItem.dataType.dataTypeKind == DataType.DataTypeKind.MODEL_TYPE) {
            if (dataDictionary) {
                codes = dataDictionary.attributeTerminologyCodes[catalogueItem.dataType.modelResourceId]
            } else {
                List<Term> terms = mauroPersistenceService.termCacheableRepository.findAllByTerminology(new Terminology(id: catalogueItem.dataType.modelResourceId))
                codes = terms.collect {new NhsDDCode(it)}
            }
            codes.each {code ->
                code.owningAttribute = this
            }
        }
        parentClass = new NhsDDClass()
        parentClass.fromMauroItem(dataDictionary, mauroPersistenceService, catalogueItem.dataClass)
        return this
    }

}