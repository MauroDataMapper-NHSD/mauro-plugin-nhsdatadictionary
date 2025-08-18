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
package uk.nhs.datadictionary.publish

import groovy.util.logging.Slf4j
import org.maurodata.dita.elements.langref.base.Prolog
import org.maurodata.dita.elements.langref.base.Simpletable
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.elements.langref.base.XRef
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.utils.DDHelperFunctions

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class DitaHelper {

    static Simpletable getAliasesTable(NhsDataDictionaryComponent component) {
        Simpletable.build(relColWidth: ["1*", "2*"]) {
            stHead {
                stentry "Context"
                stentry "Alias"
            }
            component.aliases.each {key, value ->
                strow {
                    stentry key
                    stentry value
                }
            }
        }
    }

    static Topic getAliasTopic(NhsDataDictionaryComponent component) {

        if(!component.aliases) {
            return null
        }
        Topic aliasTopic = createSubTopic(component, "Also Known As")
        aliasTopic.body {
            p "This ${component.getStereotypeForPreview()} is also known by these names: "
            simpletable (getAliasesTable(component))
        }
        return aliasTopic
    }


    static Topic createSubTopic(NhsDataDictionaryComponent component, String pageTitle) {
        String ditaPageTitle = DDHelperFunctions.makeValidDitaName(pageTitle)
        Topic.build(
            id: "${component.getDitaKey()}.${ditaPageTitle}",
            searchTitle: "${component.stereotypeForPreview}: ${component.getNameWithRetired()} - ${pageTitle}"
        ) {
            title pageTitle
        }
    }

    static Topic generateWhereUsedTopic(NhsDataDictionaryComponent component) {

        Set<NhsDataDictionaryComponent> whereMentionedNotRetired = component.whereUsed.keySet().findAll {!it.isRetired()}

        if (whereMentionedNotRetired.size() == 0) {
            return null
        }

        Simpletable whereUsedTable = Simpletable.build(
            relColWidth: ["1*", "1*"]
        ) {
            stHead {
                stentry "Type"
                stentry "Link"
            }
            whereMentionedNotRetired.sort {it.name}.each {whereMentionedComponent ->
                strow {
                    stentry whereMentionedComponent.stereotype
                    stentry {
                        xRef getXRef(whereMentionedComponent)
                    }
                }
            }
        }
        Topic whereUsedTopic = createSubTopic(component, "Where Used")
        whereUsedTopic.body {
            simpletable(whereUsedTable)
        }

        return whereUsedTopic
    }

        /*        if (this instanceof DDElement) {
                    dataDictionary.dataSets.values().each {dataSet ->
                        if (dataSet.catalogueItem.getAllDataElements().find {elem -> elem.label == this.name}) {
                            whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: dataSet.outputClassForTable),
                                                                            new STEntry(xref: dataSet.getXRef()),
                                                                            new STEntry(value: "references in description ${DDHelperFunctions.tidyLabel(name)}")
                            ]))
                        }

                    }
                }

                if (this instanceof DDAttribute) {
                    dataDictionary.classes.values().each {clazz ->
                        if (clazz.attributes.keySet().find {att -> att.name == this.name}) {
                            whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: clazz.outputClassForTable),
                                                                            new STEntry(xref: clazz.getXRef()),
                                                                            new STEntry(value: "has an attribute ${name} of type ${name}")
                            ]))
                        }

                    }

                    dataDictionary.elements.values().each {element ->
                        if (element.stringValues["element_attributes"] && element.stringValues["element_attributes"].contains(this.uin)) {
                            whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: element.outputClassForTable),
                                                                            new STEntry(xref: element.getXRef()),
                                                                            new STEntry(value: "is the data element of ${name}")
                            ]))
                        }

                    }
                }
        */

    static Pattern pattern = Pattern.compile("<a[\\s]*href=\"([^\"]*)\"[\\s]*>([^<]*)</a>")


    static String replaceXRefString(NhsDataDictionary dataDictionary, NhsDataDictionaryComponent component, String description) {

        String newDescription = description
        if (newDescription) {
            Matcher matcher = pattern.matcher(newDescription)
            while (matcher.find()) {
                String linkToFind = matcher.group(1)
                NhsDataDictionaryComponent matchedComponent = dataDictionary.internalLinks[linkToFind]
                if(!matchedComponent) {
                    log.debug("cannot match: " + linkToFind)
                    /*dataDictionary.internalLinks.each {
                        log.debug(it)
                    }*/
                }
                else {
                    String replacement =
                        XRef.build (
                            outputClass: matchedComponent.getStereotype(),
                            keyRef: matchedComponent.getDitaKey()
                        ) {
                            txt matcher.group(2)
                        }.toXmlString()
                    newDescription = newDescription.replace(matcher.group(0), replacement)
                    matchedComponent.whereUsed[component] = "References in description ${component.name}".toString()
                }
            }
        }
        return newDescription
    }


    static XRef getXRef(NhsDataDictionaryComponent component) {
        XRef.build (
            outputClass: component.getOutputClass(),
            keyRef: component.getDitaKey()
        ) {
            txt component.getNameWithRetired()
        }
    }

    static Prolog getProlog(NhsDataDictionaryComponent component) {
        Prolog.build {
            metadata {
                otherMeta(name: "name", content: component.getNameWithRetired())
                otherMeta(name: "stereotype", content: component.getStereotype())
                otherMeta(name: "sourceVersion", content: new Date().toString())
                otherMeta(name: "sourceType", content: "DerivedDD_html")
                if(component.otherProperties["baseVersion"]) {
                    otherMeta(name: "Data_Dictionary_Profile_x__y_DD_Model_x__y_baseVersion", content: component.otherProperties["baseVersion"])
                }
                otherMeta(name: "originalCase", content: component.name)
                otherMeta(name: "retired", content: component.isRetired().toString())
                keywords {
                    component.name.split("\\W+").each {
                        keyword it
                    }
                }
            }
        }
    }


    static Topic getParentTopic(NhsDataDictionaryComponent component) {
        Topic.build (id: component.getDitaKey()) {
            title (outputClass: component.stereotype) {
                txt component.getNameWithRetired()
            }
            prolog getProlog(component)
            shortdesc component.getShortDescription()

        }
    }

    static Topic createDescriptionTopic(NhsDataDictionaryComponent component) {

        Topic descriptionTopic = createSubTopic(component, "Description")

        descriptionTopic.body {
            if (component.isPreparatory()) {
                p {
                    b "This item is being used for development purposes and has not yet been approved."
                }
            } else {
                div {
                    ditaContent DDHelperFunctions.parseHtml(component.description)
                }
            }

        }
        return descriptionTopic
    }

}
