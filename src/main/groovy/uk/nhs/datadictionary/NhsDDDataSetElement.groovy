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


import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.P
import org.maurodata.dita.elements.langref.base.Row
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.facet.Metadata
import uk.nhs.datadictionary.publish.structure.ItemLink
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetItemLinkCell
import uk.nhs.datadictionary.services.MauroPersistenceService

class NhsDDDataSetElement implements NhsDDDataSetComponent {

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    UUID elementId
    Integer webOrder
    boolean retired
    String groupRepeats
    String rules
    String uin
    String branchId

    NhsDDElement reuseElement
    NhsDataDictionary dataDictionary

    // For testing only
    NhsDDDataSetElement() {
    }

    NhsDDDataSetElement(DataElement dataElement) {
        this.name = dataElement.label
        if(this.name.endsWith(" 1")) {
            this.name = this.name.replace(" 1", "")
        }
        if(this.name.endsWith(" 2")) {
            this.name = this.name.replace(" 2", "")
        }
        //this.elementId = dataElement.getSemanticLinks().first().multiFacetAwareItemId
        this.minMultiplicity = dataElement.minMultiplicity
        this.maxMultiplicity = dataElement.maxMultiplicity

    }
    NhsDDDataSetElement(DataElement dataElement, NhsDataDictionary dataDictionary, MauroPersistenceService mauroPersistenceService, UUID branchId) {
        this(dataElement)
        this.branchId = branchId
        if(dataDictionary) {
            this.dataDictionary = dataDictionary
            if(dataDictionary.elements[name]) {
                this.reuseElement = dataDictionary.elements[name]
            } else {
                System.err.println("Cannot find element: ${dataElement.label}, $name!")
            }
        } else {
            if(dataElement.semanticLinks.size() == 1) {
                DataElement targetDataElement = mauroPersistenceService.dataElementCacheableRepository.findById(dataElement.semanticLinks[0].targetMultiFacetAwareItemId)
                reuseElement = new NhsDDElement(targetDataElement).fromMauroItem(dataDictionary, mauroPersistenceService, targetDataElement)
                reuseElement.branchId = branchId
                reuseElement.codes = []
            }
        }

        mandation = dataElement.metadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_MRO }?.value
        if(mandation && !mandation.isEmpty()) {
            mandation = mandation.trim().toUpperCase().subSequence(0,1)
        }
        constraints = dataElement.metadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_RULES }?.value
        groupRepeats = dataElement.metadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_GROUP_REPEATS }?.value
        rules = dataElement.metadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_RULES }?.value
        uin = dataElement.metadata.find { it.key == "uin" }?.value

        Metadata md = dataElement.metadata.find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_WEB_ORDER }
        if(md) {
            webOrder = Integer.parseInt(md.value)
        } else {
            webOrder = dataElement.order
        }
    }

    def createHtmlLink(MarkupBuilder markupBuilder) {
        String link = "#/preview/${dataDictionary.containingVersionedFolder.id.toString()}/element/${elementId.toString()}"
        markupBuilder.a (href: link, class: "element") {
            mkp.yield(name)
        }

    }

    def createLink(MarkupBuilder markupBuilder) {
        String link
        if(this.reuseElement) {
            link = reuseElement.getMauroPath()
        } else {
            link = NhsDDElement.getPathFromName(name, false)
        }

        markupBuilder.a (href: link) {
            mkp.yield(name)
        }

    }

    Row createDita(NhsDataDictionary dataDictionary) {
        Row.build {
            entry {
                p mandation
            }
            entry {
                createEntry().each { paragraph ->
                    p paragraph
                }
            }
        }
    }

    // Remove one day, replaced with buildOtherDataSetItemLinkCell()
    @Deprecated
    List<P> createEntryParagraphs() {
        List<P> response = []
        if(reuseElement) {
            response.add(P.build {
                xRef (outputClass: "element", keyRef: reuseElement.getDitaKey(), scope: Scope.LOCAL) {
                    txt name
                }
            })
        } else {
            response.add(P.build {
                text name
            })
        }
        if(maxMultiplicity == "-1") {
            response.add(P.build {
                text "Multiple occurrences of this item are permitted"
            })
        }
        return response
    }

    OtherDataSetItemLinkCell buildOtherDataSetItemLinkCell() {
        ItemLink itemLink = reuseElement ? ItemLink.create(reuseElement) : null
        new OtherDataSetItemLinkCell(this.name, itemLink, maxMultiplicity == "-1")
    }

    Row addCDSChildRow(int totalDepth, int currentDepth){
        Row.build {
            entry(align: Align.CENTER) {
                if(mandation) {
                    mandation.split(";").each {
                        p it
                    }
                }
            }
            entry(align: Align.CENTER) {
                if(groupRepeats) {
                    groupRepeats.split(";").each {
                        p it
                    }
                }
            }
            entry(namest: "col${currentDepth*2+3}", nameend: "col${totalDepth*2+3}") {
                if(reuseElement) {
                    xRef DitaHelper.getXRef(reuseElement)
                } else {
                    p name
                }
            }
            entry(align: Align.CENTER) {
                if(rules) {
                    rules.split(";").each {
                        p it
                    }
                }
            }
        }
    }

}
