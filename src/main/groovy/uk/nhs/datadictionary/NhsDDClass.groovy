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
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.meta.SpaceSeparatedStringList
import org.maurodata.domain.datamodel.DataClass
import uk.nhs.datadictionary.publish.structure.ClassAttributeRow
import uk.nhs.datadictionary.publish.structure.ClassAttributeSection
import uk.nhs.datadictionary.publish.structure.ClassRelationshipRow
import uk.nhs.datadictionary.publish.structure.ClassRelationshipSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.ItemLink

@Slf4j
@CompileDynamic
class NhsDDClass extends NhsDataDictionaryComponent <DataClass> {

    NhsDDClass(DataClass catalogueItem = null, UUID branchId = null) {
        super(catalogueItem, branchId)
    }

    DataClass newCatalogueItem(String name = null) {
        return new DataClass(label: name)
    }

    @Override
    String getStereotype() {
        'Class'
    }

    @Override
    String getStereotypeForPreview() {
        'class'
    }

    @Override
    String getPluralStereotypeForWebsite() {
        'classes'
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".class"
    }

    List<NhsDDClass> extendsClasses = []

    @JsonIgnore
    List<NhsDDAttribute> keyAttributes = []

    @JsonIgnore
    List<NhsDDAttribute> otherAttributes = []

    @JsonProperty('relationships')
    List<NhsDDClassRelationship> classRelationships = []

    List<NhsDDClassLink> classLinks = []

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                String firstSentence = getFirstSentence()
                if (firstSentence && firstSentence.toLowerCase().contains("a subtype of")) {
                    String secondSentence = getSentence(1)
                    return secondSentence
                } else {
                    return firstSentence
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + description)
                e.printStackTrace()
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDClass"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        super.fromXml(xml, dataDictionary)

        xml.property.each { property ->
            String attributeUin = property.referencedElement.text()
            NhsDDAttribute attribute = dataDictionary.attributesByUin[attributeUin]
            if(attribute) {
                if("true" == property.isUnique.text()) {
                    keyAttributes.add(attribute)
                } else {
                    otherAttributes.add(attribute)
                }

                // Must track in the DD attribute that this is the owner class, required by the
                // attribute to understand hierarchical information in Mauro
                attribute.parentClass = this
            } else {
                log.debug("Cannot find attributeElement with Uin: " + attributeUin)
            }
        }
        classLinks = xml.link.collect{it -> new NhsDDClassLink(it)}
        dataDictionary.classesByUin[getUin()] = this
    }


    List<NhsDDAttribute> getAttributes() {
        keyAttributes + otherAttributes
    }

    List<NhsDDClassRelationship> allRelationships() {
        classRelationships
            .findAll { it.targetClass.itemState != DictionaryItem.DictionaryItemState.RETIRED }
            .sort { a, b ->
                b.key <=> a.key ?: a.targetClass.name.toLowerCase() <=> b.targetClass.name.toLowerCase()
            }
    }

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:Retired|dc:${name}"
        } else {
            return "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:${name}"
        }
    }

    @Override
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = new DictionaryItem(this, this.branchId)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            addClassAttributeSection(dictionaryItem)
            addClassRelationshipSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addAliasesSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addClassAttributeSection(DictionaryItem dictionaryItem) {
        List<ClassAttributeRow> keyRows = keyAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name.toLowerCase() }
            .collect { attribute -> new ClassAttributeRow(attribute.key, ItemLink.create(attribute))}

        List<ClassAttributeRow> otherRows = otherAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name.toLowerCase() }
            .collect { attribute -> new ClassAttributeRow(attribute.key, ItemLink.create(attribute))}

        List<ClassAttributeRow> allRows = keyRows + otherRows

        dictionaryItem.addSection(new ClassAttributeSection(dictionaryItem, allRows))
    }

    void addClassRelationshipSection(DictionaryItem dictionaryItem) {
        if (classRelationships) {
            List<ClassRelationshipRow> rows = allRelationships().collect {relationship ->
                new ClassRelationshipRow(
                    relationship.key,
                    relationship.relationshipDescription,
                    ItemLink.create(relationship.targetClass))
            }

            dictionaryItem.addSection(new ClassRelationshipSection(dictionaryItem, rows))
        }
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            topics.add(attributesTopic())
            if (classRelationships) {
                topics.add(classRelationshipsTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    Topic attributesTopic() {
        List<NhsDDAttribute> attributes = getAttributes().findAll { !it.isRetired() }

        Topic.build (id: getDitaKey() + "_attributes") {
            title "Attributes"
            body {
                if (attributes.size() == 0) {
                    p "This class has no attributes"
                } else {
                    simpletable(relColWidth: new SpaceSeparatedStringList(["1*", "9*"]), outputClass: "table table-sm") {
                        stHead(outputClass: "thead-light") {
                            stentry "Key"
                            stentry "Attribute Name"
                        }
                        keyAttributes
                            .findAll { !it.isRetired() }
                            .sort { it.name.toLowerCase() }
                            .each { attribute ->
                                strow {
                                    stentry 'Key'
                                    stentry {
                                        xRef attribute.calculateXRef()
                                    }
                                }
                            }
                        otherAttributes
                            .findAll { !it.isRetired() }
                            .sort { it.name.toLowerCase() }
                            .each { attribute ->
                                strow {
                                    stentry ''
                                    stentry {
                                        xRef attribute.calculateXRef()
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    Topic classRelationshipsTopic() {
        Topic.build(id: getDitaKey() + "_relationships") {
            title "Relationships"
            body {
                p "Each $name:"
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "5*", "5*"]), outputClass: "table table-sm") {
                    stHead(outputClass: "thead-light") {
                        stentry "Key"
                        stentry "Relationship"
                        stentry "Class"
                    }
                    allRelationships().each {relationship ->
                        strow {
                            stentry relationship.key ? 'Key' : ''
                            stentry relationship.relationshipDescription
                            stentry {
                                xRef relationship.targetClass.calculateXRef()
                            }
                        }
                    }
                }
            }
        }
    }


}
