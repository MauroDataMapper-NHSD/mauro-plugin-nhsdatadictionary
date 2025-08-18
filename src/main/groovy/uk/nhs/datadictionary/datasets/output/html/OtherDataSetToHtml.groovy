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
package uk.nhs.datadictionary.datasets.output.html


import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetClass
import uk.nhs.datadictionary.NhsDDDataSetElement

@Slf4j
class OtherDataSetToHtml {

    MarkupBuilder markupBuilder

    OtherDataSetToHtml(MarkupBuilder markupBuilder1) {
        this.markupBuilder = markupBuilder1
    }

    void outputAsHtml(NhsDDDataSet dataSet) {
        dataSet.dataSetClasses.sort {it.webOrder}.each {ddDataClass ->
            outputClassAsHtml(ddDataClass)
        }
    }

    void outputClassAsHtml(NhsDDDataSetClass ddDataClass) {
        if (ddDataClass.isChoice && ddDataClass.name.startsWith("Choice")) {
            markupBuilder.b 'One of the following options must be used:'
            ddDataClass.dataSetClasses.sort {it.webOrder}.
                eachWithIndex{childDataClass, int idx ->
                    if (idx != 0) {
                        markupBuilder.b 'Or'
                    }
                    outputClassAsHtml(childDataClass)
                }
        } else {
            markupBuilder.table (class: "simpletable table table-sm") {
                thead {
                    tr {
                        th(colspan: 2, class: "thead-light", 'text-align': 'center') {
                            b ddDataClass.name
                            if (ddDataClass.description) {
                                p {
                                    mkp.yieldUnescaped(ddDataClass.description)
                                }
                            }
                        }
                    }
                }
                tbody {
                    if (ddDataClass.dataSetElements) {
                        markupBuilder.tr {
                            td(width: '20%', class: 'mandation-header') {
                                b "Mandation"

                            }
                            td(width: '80%', class: 'elements-header') {
                                b "Data Elements"
                            }
                        }
                    }

                    addAllChildRows(ddDataClass)
                }
            }
        }

    }

    void addAllChildRows(NhsDDDataSetClass ddDataClass) {
        if(ddDataClass.dataSetElements && ddDataClass.dataSetElements.size() > 0) {
            ddDataClass.getSortedChildren().each {classOrElement ->
                addChildRow(classOrElement)
            }
        } else { // No data elements
            ddDataClass.dataSetClasses.sort {it.webOrder}.eachWithIndex {childClass, idx ->
                if (ddDataClass.isChoice && idx != 0) {
                    markupBuilder.tr {
                        td(colspan: 2, class: 'or-header') {
                            b 'Or'
                        }
                    }
                } else if (idx != 0) {
                    markupBuilder.tr {
                        td(colspan: 2) { }
                    }
                }
                addSubClass(childClass)
            }
        }
    }

    void addChildRow(def classOrElement) {
        markupBuilder.tr {
            td (width: '20%', class: 'mandation') {
                p classOrElement.mandation
            }
            td (width: '80%') {
                addDataSetElementContent(classOrElement)
            }
        }
    }

    // Replicate the logic of NhsDDDataSetClass.getXrefStringFromItem()
    void addDataSetElementContent(def classOrElement) {
        if (classOrElement instanceof NhsDDDataSetElement) {
            addBasicDataSetElementContent((NhsDDDataSetElement) classOrElement)
        }
        else { //if (catalogueItem instanceof NhsDDDataSetClass)
            addComplexDataSetElementContent((NhsDDDataSetClass) classOrElement)
        }
    }

    void addBasicDataSetElementContent(NhsDDDataSetElement dataSetElement) {
        dataSetElement.createLink(markupBuilder)

        if (dataSetElement.maxMultiplicity == '-1') {
            markupBuilder.p 'Multiple occurrences of this item are permitted'
        }
    }

    void addComplexDataSetElementContent(NhsDDDataSetClass dataSetClass) {
        if (dataSetClass.dataSetElements.size() == 0) {
            return
        }

        if (dataSetClass.isAddress) {
            // See COSD Pathology for an example of this
            NhsDDDataSetElement dataSetElement = dataSetClass.dataSetElements[0]

            dataSetElement.createLink(markupBuilder)
            markupBuilder.getMkp().yield(" - ")
            markupBuilder.a (href: dataSetClass.address1, class: "class") {
                mkp.yield("ADDRESS STRUCTURED")
            }

            markupBuilder.p('Or')

            dataSetElement.createLink(markupBuilder)
            markupBuilder.getMkp().yield(" - ")
            markupBuilder.a (href: dataSetClass.address2, class: "class") {
                mkp.yield("ADDRESS UNSTRUCTURED")
            }
        }
        else if (dataSetClass.isChoice && dataSetClass.name.startsWith("Choice")) {
            dataSetClass.getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    markupBuilder.p "Or"
                }
                addDataSetElementContent(childItem)
            }
        }
        else if (dataSetClass.isAnd && (dataSetClass.name.startsWith("Choice") || dataSetClass.name.startsWith("And"))) {
            dataSetClass.getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    markupBuilder.p "And"
                }
                addDataSetElementContent(childItem)
            }
        }
        else if (dataSetClass.isInclusiveOr && dataSetClass.name.startsWith("Choice")) {
            dataSetClass.getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    markupBuilder.p "And/Or"
                }
                addDataSetElementContent(childItem)
            }
        }
        else {
            NhsDDDataSetElement dataSetElement = dataSetClass.dataSetElements[0]
            dataSetElement.createLink(markupBuilder)
        }
    }

    void addSubClass(NhsDDDataSetClass dataSetClass) {
        if(dataSetClass.name != 'Choice') {
            markupBuilder.tr(class: 'table-primary') {
                td(width: '20%', class: 'mandation-header') {
                    b 'Mandation'
                }
                td(width: '80%') {
                    p {
                        b dataSetClass.name
                    }
                    if(dataSetClass.description) {
                        markupBuilder.p {
                            mkp.yieldUnescaped(dataSetClass.description)
                        }
                    }
                }
            }
        }
        addAllChildRows(dataSetClass)
    }

}
