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
import org.maurodata.dita.elements.langref.base.XRef
import org.maurodata.dita.helpers.HtmlHelper
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.SemanticLinkType
import org.maurodata.domain.terminology.Term
import uk.nhs.datadictionary.publish.structure.CodesRow
import uk.nhs.datadictionary.publish.structure.CodesSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.FormatLengthSection
import uk.nhs.datadictionary.publish.structure.ItemLink
import uk.nhs.datadictionary.publish.structure.ItemLinkListSection
import uk.nhs.datadictionary.services.MauroPersistenceService

@Slf4j
class NhsDDElement extends NhsDataDictionaryComponent <DataElement> {

    NhsDDElement(DataElement catalogueItem = null, UUID branchId = null) {
        super(catalogueItem, branchId)
    }

    DataElement newCatalogueItem(String name = null) {
        return new DataElement(label: name)
    }


    @Override
    String getStereotype() {
        "Data Element"
    }

    @Override
    String getStereotypeForPreview() {
        "element"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "data_elements"
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".element"
    }

    @JsonIgnore
    List<NhsDDCode> codes = []

    @JsonIgnore
    XRef formatLinkXref

    @JsonIgnore
    String codeSetVersion

    @JsonProperty('attributes')
    List<NhsDDAttribute> instantiatesAttributes = []

    String getFormatLength() {
        if (!otherProperties.containsKey("formatLength")) {
            return null
        }

        otherProperties["formatLength"]
    }

    boolean hasFormatLength() {
        otherProperties["formatLength"] || otherProperties["formatLink"]
    }

    String previewAttributeText

    boolean isEmptyDescription() {
        !description || description == "<div/>"
    }

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll { !it.isRetired() }
                String firstSentence = getFirstSentence()
                boolean missingDescription = isEmptyDescription()
                /*if (missingDescription && otherProperties["attributeText"]) {
                    return getSentence(otherProperties["attributeText"], 0)
                }
                else */
                if (missingDescription && activeAttributes.size() == 1) {
                    return activeAttributes[0].getShortDescription()
                }
                else if (firstSentence && firstSentence.toLowerCase().contains("is the same as") && activeAttributes.size() == 1) {
                    return activeAttributes[0].getShortDescription()
                }
                else if (firstSentence) {
                    return firstSentence
                }
                else {
                    System.err.println("Couldn't set short description: $stereotype $name")
                    System.err.println("$firstSentence")
                    System.err.println("$description")
                }
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: ${description}")
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDDataElement"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        super.fromXml(xml, dataDictionary)
        String capitalizedCodeSetName = xml.name[0].text()

        instantiatesAttributes.addAll(xml."link".collect {link ->
            link.participant.find{p -> p["@role"] == "Supplier"}["@referencedUin"]
        }.collect {
            dataDictionary.attributesByUin[it]
        })
        instantiatesAttributes.each {nhsDDAttribute ->
            nhsDDAttribute.instantiatedByElements.add(this)
        }


        if (xml."value-set".size() > 0 && !isRetired()) {
            String codeSetVersion = xml."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.version."@value".text()
            NhsDDAttribute linkedAttribute = instantiatesAttributes.find {it.codes.size() > 0 && !it.isRetired()}
            if(!linkedAttribute) {
                log.error("No suitable linked attribute found for element ${name}")
            } else {
                xml."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.concept.each {concept ->
                    if (concept.property.find {Node property ->
                        property.code[0].attribute('value') == "Data Element" &&
                        property.valueString[0].attribute('value') == capitalizedCodeSetName
                    }) {
                        NhsDDCode code = linkedAttribute.codes.find {it -> it.code == concept.code[0].attribute('value')}
                        codes.add(code)
                    }
                }
            }

        }
        if(xml.DefaultCode.size() > 0 && !isRetired()) {
            NhsDDAttribute linkedAttribute = instantiatesAttributes.find { !it.isRetired()}
            if (!linkedAttribute) {
                log.error("No suitable linked attribute found for element ${name}")
            } else {
                xml.DefaultCode.each {defaultCode ->

                    NhsDDCode code = linkedAttribute.codes.find {it -> it.code == defaultCode.code.text()}
                    if (!code) {
                        String definition = defaultCode.description.text()
                        String webPresentation = ""
                        if(definition.contains("&lt;")) {
                            definition = defaultCode.code.text()
                            webPresentation = NhsDDAttribute.unquoteString(defaultCode.description.text())
                        }
                        code = new NhsDDCode(
                            isDefault: true,
                            code: defaultCode.code.text(),
                            definition: definition,
                            webPresentation: webPresentation,
                            webOrder: Integer.parseInt(defaultCode.webOrder.text() ?: ''),
                            owningAttribute: linkedAttribute
                        )
                        linkedAttribute.codes.add(code)

                    } else {
                        if((!code.webPresentation && code.definition != defaultCode.description.text()) || (code.webPresentation && code.webPresentation != defaultCode.description.text()))
                        {
                            System.err.println("Invalid default code!")
                            System.err.println("Element: ${name}")
                            System.err.println("Code: ${defaultCode.code.text()} - ${defaultCode.description.text()}")
                            System.err.println("Attribute: ${linkedAttribute.name}")
                            System.err.println("Code: ${code.code} - ${code.definition}")
                        }
                    }
                    codes.add(code)
                }
            }
        }

        if(!isRetired()) {
            if (description.find(regex)) {
                catalogueItem.description = description.replaceFirst(regex, "").trim()
                otherProperties["suppressFirstSentence"] = 'false'
            } else {
                //Node definitionXml = HtmlHelper.tidyAndConvertToNode("<p>" + definition + "<p>")
                //Node firstParagraph = definitionXml.children().find{it instanceof Node && it.name() == 'p'}
                //firstParagraph.parent().remove(firstParagraph)
                otherProperties["suppressFirstSentence"] = 'true'
                //otherProperties["attributeText"] = XmlUtil.serialize(firstParagraph).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
                //definition = XmlUtil.serialize(definitionXml).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")

            }
        }

    }

    final String regex = "<a[^>]*>[^<]*</a>\\W+is\\s+the\\s+same\\s+as\\s+attribute\\W+<a[^>]*>[^<]*</a>\\s*\\."

    String getMauroPath() {
        return getPathFromName(name, isRetired())
    }

    static String getPathFromName(String name, Boolean retired = false) {
        if(retired) {
            "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}|dc:Retired|de:${name}"
        } else {
            // Match ingest structure of organising DD Elements alphabetically
            String className = name.substring(0, 1).toUpperCase()
            return "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}|dc:${className}|de:${name}"
        }


    }

    boolean hasNationalCodes() {
        this.codes.find { !it.isDefault}
    }

    boolean hasDefaultCodes() {
        this.codes.find { it.isDefault }
    }

    @Override
    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        super.replaceLinksInDefinition(pathLookup)
        codes.each { code ->
            code.webPresentation = replaceLinksInString(code.webPresentation, pathLookup)
        }
        if(otherProperties["formatLink"] && pathLookup[otherProperties["formatLink"]]) {
            formatLinkXref = pathLookup[otherProperties["formatLink"]].calculateXRef()
        }
        /*
        if(otherProperties["attributeText"] && otherProperties["attributeText"] != "") {
            otherProperties["attributeText"] = replaceLinksInString(otherProperties["attributeText"], pathLookup)
        }
        */
    }

    String getDescription() {
        if(dataDictionary && isRetired()) {
            return dataDictionary.retiredItemText
        } else if(dataDictionary && isPreparatory()) {
            return dataDictionary.preparatoryItemText
        } else {
            List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll {!it.isRetired()}
            if (activeAttributes.size() == 1 && otherProperties["suppressFirstSentence"] != 'true') {
                NhsDDAttribute attribute = activeAttributes[0]
                String ret = "<a href=\"${this.getMauroPath()}\">${this.name}</a> is the same as attribute <a href=\"${attribute.getMauroPath()}\">${attribute.name}</a>. "
                System.err.println(ret)
                if (catalogueItem) {
                    ret += catalogueItem.description
                }
                return ret
            }
            return catalogueItem.description
        }
    }

    @Override
    Topic descriptionTopic() {
        Topic.build (id: getDitaKey() + "_description") {
            title "Description"
            body {
                if (isActivePage() && otherProperties["suppressFirstSentence"] != 'true') {
                    List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll {!it.isRetired() }
                    if (activeAttributes.size() == 1) {
                        p {
                            xRef this.calculateXRef()
                            text " is the same as attribute "
                            xRef activeAttributes[0].calculateXRef()
                            text "."
                        }
                    }
                }

                if (description) {
                    div HtmlHelper.replaceHtmlWithDita(description.replace('<table', '<table class=\"table-striped\"'))
                }
            }
        }
    }

    @Override
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = new DictionaryItem(this, this.branchId)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            addFormatLengthSection(dictionaryItem)
        }

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            if (hasNationalCodes()) {
                List<CodesRow> nationalCodes = createCodesSectionRows(getNationalCodes())
                dictionaryItem.addSection(CodesSection.createNationalCodes(dictionaryItem, nationalCodes))
            }
            if (hasDefaultCodes()) {
                List<CodesRow> defaultCodes = createCodesSectionRows(getDefaultCodes())
                dictionaryItem.addSection(CodesSection.createDefaultCodes(dictionaryItem, defaultCodes))
            }
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addLinkedAttributesSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    static List<CodesRow> createCodesSectionRows(List<NhsDDCode> codesList) {
        codesList.collect {code ->
            new CodesRow(code.code, code.description, code.hasWebPresentation())
        }
    }

    void addFormatLengthSection(DictionaryItem dictionaryItem) {
        dictionaryItem.addSection(new FormatLengthSection(dictionaryItem, new NhsDDFormatLength(this)))
    }

    void addLinkedAttributesSection(DictionaryItem dictionaryItem) {
        if (!instantiatesAttributes) {
            return
        }

        List<NhsDDAttribute> attributes = instantiatesAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }

        if (attributes.empty) {
            return
        }

        List<ItemLink> itemLinks = attributes.collect {attribute -> ItemLink.create(attribute) }
        dictionaryItem.addSection(ItemLinkListSection.createAttributesListSection(dictionaryItem, itemLinks))
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []

        if (isActivePage() && hasFormatLength()) {
            topics.add(getFormatLengthTopic())
        }

        topics.add(descriptionTopic())

        if (isActivePage()) {
            if (hasNationalCodes()) {
                topics.add(getNationalCodesTopic())
            }
            if (hasDefaultCodes()) {
                topics.add(getDefaultCodesTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (instantiatesAttributes) {
                Topic linkedAttributesTopic = getLinkedAttributesTopic()
                if (linkedAttributesTopic) {
                    topics.add(linkedAttributesTopic)
                }
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    @JsonIgnore
    Topic getFormatLengthTopic() {
        Topic.build (id: getDitaKey() + "_formatLength") {
            title "Format / Length"
            body {
                if(formatLinkXref) {
                    p {
                        txt "See "
                        xRef formatLinkXref
                    }
                } else {
                    p otherProperties["formatLength"]
                }
            }
        }
    }

    List<NhsDDCode> getNationalCodes() {
        NhsDDCode.sortCodes(codes.findAll { !it.isDefault })
    }

    List<NhsDDCode> getDefaultCodes() {
        NhsDDCode.sortCodes(codes.findAll { it.isDefault })
    }

    @JsonIgnore
    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = getNationalCodes()
        String topicTitle = "National Codes"
        if(instantiatesAttributes) {
            if(orderedCodes.size() < instantiatesAttributes[0].codes.findAll { !it.isDefault }.size()) {
                topicTitle = "Permitted National Codes"
            }
        }

        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", topicTitle, orderedCodes)
    }

    @JsonIgnore
    Topic getDefaultCodesTopic() {
        List<NhsDDCode> orderedCodes = getDefaultCodes()
        NhsDDCode.getCodesTopic(getDitaKey() + "_defaultCodes", "Default Codes", orderedCodes)
    }

    @JsonIgnore
    Topic getLinkedAttributesTopic() {
        List<NhsDDAttribute> attributes = instantiatesAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }

        if (attributes.empty) {
            return null
        }

        String attributesTitle = attributes.size() > 1 ? "Attributes" : "Attribute"
        Topic.build (id: getDitaKey() + "_attributes") {
            title attributesTitle
            body {
                ul {
                    attributes.each { attribute ->
                        li {
                            xRef (attribute.calculateXRef())
                        }
                    }
                }
            }
        }
    }

    void updateWhereUsed() {
        dataDictionary.dataSets.values().each {dataSet ->
            if(dataSet.allElements.reuseElement.contains(this)) {
                whereUsed[dataSet] = "references in description $name".toString()
            }
        }
    }

    @Override
    @JsonIgnore
    String getDiscriminator() {
        name
    }

    @Override
    NhsDDElement fromMauroItem(NhsDataDictionary dataDictionary, MauroPersistenceService mauroPersistenceService, DataElement catalogueItem) {
        super.fromMauroItem(dataDictionary, mauroPersistenceService, catalogueItem)
        catalogueItem.dataType = mauroPersistenceService.dataTypeCacheableRepository.findById(catalogueItem.dataType.id)
        if(dataDictionary) {
            catalogueItem.semanticLinks.each {
                if (it.linkType == SemanticLinkType.REFINES) {
                    NhsDDAttribute linkedAttribute = dataDictionary.attributesByCatalogueId[it.targetMultiFacetAwareItemId]
                    if (linkedAttribute) {
                        instantiatesAttributes.add(linkedAttribute)
                        linkedAttribute.instantiatedByElements.add(this)
                    }
                }
            }
        }
        if(catalogueItem.dataType.dataTypeKind == DataType.DataTypeKind.MODEL_TYPE) {
            if (dataDictionary) {
                codes = dataDictionary.elementCodeSetCodes[catalogueItem.dataType.modelResourceId]
            } else {
                Set<Term> terms = mauroPersistenceService.termCacheableRepository.findAllByCodeSetsIdIn([catalogueItem.dataType.modelResourceId])
                System.err.println("Terms size: ${terms.size()}")
                codes = terms.collect {new NhsDDCode(it)}
            }
            codes.each {code ->
                code.usedByElements.add(this)
            }
        }
        return this
    }

    Set<NhsDDAttribute> getAllAttributesForElement() {
        catalogueItem.semanticLinks.findAll {semanticLink ->
            semanticLink.linkType == SemanticLinkType.REFINES
            && semanticLink.target
        }.collect {semanticLink ->
            if(dataDictionary) {
                return dataDictionary.attributesByCatalogueId[semanticLink.targetMultiFacetAwareItemId] as NhsDDAttribute
            } else {
                NhsDDAttribute attribute = new NhsDDAttribute(semanticLink.target as DataElement, branchId)
                attribute.dataDictionaryComponentService = this.dataDictionaryComponentService
                return attribute
            }
        }.findAll{
            !it.isRetired()
        }.sort {it.name} as Set<NhsDDAttribute>
    }


}
