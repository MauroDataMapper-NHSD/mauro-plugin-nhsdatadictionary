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
//@CompileStatic
class CDSDataSetToHtml {

    MarkupBuilder markupBuilder

    CDSDataSetToHtml(MarkupBuilder markupBuilder1) {
        this.markupBuilder = markupBuilder1
    }


    void outputAsCdsHtml(NhsDDDataSet ddDataSet) {
        ddDataSet.dataSetClasses.sort {it.webOrder}.each {dataSetClass ->
            outputCDSClassAsHtml(dataSetClass)
        }
    }

    void outputCDSClassAsHtml(NhsDDDataSetClass dataClass) {

        int totalDepth = calculateClassDepth(dataClass)
        if(dataClass.isChoice && dataClass.name.startsWith("Choice")) {
            markupBuilder.p {
                b 'One of the following options must be used:'
            }
            dataClass.dataSetClasses.sort {it.webOrder}.eachWithIndex {childDataClass, idx ->
                if(idx != 0) {
                    markupBuilder.p { b 'Or'}
                }
                outputCDSClassAsHtml(childDataClass)
            }

        } else if(!dataClass.dataSetClasses && !dataClass.dataSetElements && dataClass.isDataSetReference) {
            // We've got a pointer to another dataset
            //String linkedDataSetUrl = DataSetParser.getDataSetReferenceTo(dataClass)
            //System.err.println(dataClass.label.replaceFirst("DATA GROUP: ", ""))
            String linkedDataSetName = dataClass.dataSetReferenceTo

            markupBuilder.table {
                addTopLevelCDSHeader(dataClass, linkedDataSetName, dataClass.description, totalDepth, false)
                tbody {
                    tr {
                        td (class: 'mandation', dataClass.mandation)
                        td (class: 'groupRepeats', dataClass.groupRepeats)
                        //new Html(content: "<p> <b>Data Group:</b>${linkedDataSet.getXRef().outputAsString(linkedDataSet.name)}</p>" +
                        //                                  "<p>${replaceDescriptionLinks(DataSetParser.getMultiplicityText(dataClass))}</p>")
                        td(class: 'dataGroup', colspan: (totalDepth*2 - 2)) {
                            p {
                                b 'Data Group:'
                                a (href: '', dataClass.dataSetReferenceTo)

                            }
                            p dataClass.multiplicityText
                        }
                    }
                }

            }

        } else {
            String label = dataClass.name
            if(label.endsWith(" 1")) {
                label = label.replace(" 1", "")
            }
            markupBuilder.table {
                addTopLevelCDSHeader(dataClass, label, dataClass.description, totalDepth)
                addCDSClassContents(dataClass, totalDepth, 0)
            }

        }
    }


    int calculateClassDepth(NhsDDDataSetClass dataClass) {
        int maxDepth = 0
        if(dataClass.dataSetClasses) {
            maxDepth = dataClass.dataSetClasses.collect {calculateClassDepth(it) }.max()
        }
        if(dataClass.isAnd || dataClass.isChoice) {
            return maxDepth + 1
        } else {
            return maxDepth + 1
        }

    }

    void addTopLevelCDSHeader(NhsDDDataSetClass dataClass, String groupName, String function, int totalDepth, boolean includeMRO = true) {
        /*List<String> columnWidths = []
        for(int i=0;i<totalDepth*2;i++) {
            columnWidths.add("1*")
        }
        columnWidths.addAll(["1*", "1*", "8*", "1*"])

         */
        int noColumns = totalDepth*2 + 4
        /*for(int i=0;i<totalDepth*2+4;i++) {
            table.tGroups[0].colSpecs[i].align = "center"
        }
        table.tGroups[0].colSpecs[totalDepth*2+2].align = "left"
        */
        markupBuilder.thead {
            tr {
                th(class: 'notation', colspan: 2) {
                    p {
                        a(href: "supporting_definition_commissioning_data_set_notation", 'Notation')
                    }
                }
                th(class: 'thead-light', colspan: noColumns - 2) {
                    p {
                        b 'Data Group:'
                        mkp.yield groupName
                    }
                }
            }
            tr(class: 'thead-light') {
                th {
                    p 'Group Status'
                    if (includeMRO) {
                        p dataClass.getMandation()
                    }

                }
                th {
                    p 'Group Repeats'
                    if (includeMRO) {
                        p dataClass.getGroupRepeats()
                    }
                }
                th(colspan: noColumns - 2) {
                    p {
                        b 'Function:'
                        mkp.yieldUnescaped(function)
                    }
                }
            }
        }
    }

    void addCDSClassContents(NhsDDDataSetClass dataClass, int totalDepth, int currentDepth) {
        if (dataClass.dataSetElements) {
            if(dataClass.isChoice) {
                addCDSChildRow(dataClass, totalDepth, currentDepth)
            } else {
                dataClass.sortedChildren.each { child ->
                    addCDSChildRow(child, totalDepth, currentDepth)
                }
            }
        } else { // no dataElements
            if (dataClass.dataSetClasses) {
                if (dataClass.isChoice) {
                    markupBuilder.tr {
                        td (class: 'choiceText', colspan: (totalDepth*2+4) - (currentDepth*2+1)) {
                            p {
                                b 'One of the following DATA GROUPS must be used:'
                            }
                        }
                    }
                }
                dataClass.sortedChildren.each { NhsDDDataSetClass childDataClass ->
                    if(childDataClass.name.startsWith("And") && childDataClass.isAnd) {
                        childDataClass.sortedChildren.each {it ->
                            addCDSSubClass(it, totalDepth, currentDepth + 1)
                        }
                    } else {
                        int newCurrentDepth = Integer.min(currentDepth+1, totalDepth)
                        addCDSSubClass(childDataClass, totalDepth, newCurrentDepth)
                    }
                    if (dataClass.isChoice && childDataClass != dataClass.sortedChildren.last()) {
                        markupBuilder.tr {
                            td (class: 'or-header', colspan: (totalDepth * 2 + 4) - (currentDepth * 2 + 1)) {
                                p {
                                    b 'Or'
                                }
                            }
                        }
                    }
                }
            } else {
                log.error("No data elements or data classes... ")
                log.error(dataClass.name)
            }
        }
    }

    void addCDSSubClass(NhsDDDataSetClass dataClass, int totalDepth, int currentDepth) {
        addCDSClassHeader(dataClass, totalDepth, currentDepth)
        addCDSClassContents(dataClass, totalDepth, currentDepth)
    }

    void addCDSClassHeader(NhsDDDataSetClass dataClass, int totalDepth, int currentDepth) {
        if(dataClass.name.startsWith("Choice") || dataClass.name.startsWith("And")) {
            return
        }
        int moreRows = calculateClassRows(dataClass) + 1

        String name = dataClass.name
        if(name.endsWith(" 1") || name.endsWith(" 2") || name.endsWith(" 3")) {
            name = name.replace(" 1", "").replace(" 2", "").replace(" 3", "")
        }
        markupBuilder.tr(class: 'thead-light table-primary') {
            td(class: 'mandation', rowspan: moreRows, dataClass.mandation)
            td(class: 'groupRepeats', rowspan: moreRows, dataClass.groupRepeats)
            td(class: 'cds-element-header', colspan: (totalDepth*2+3) - (currentDepth*2+1)) {
                b name
                if (dataClass.description) {
                    markupBuilder.p {
                        mkp.yieldUnescaped(dataClass.description)
                    }
                }
            }
            td (class: 'rules') {
                a (href: '', 'Rules' ) // TODO
            }
        }
    }

    static int calculateClassRows(NhsDDDataSetClass dataClass) {

        if((dataClass.isChoice || dataClass.isAnd) && (!dataClass.dataSetClasses || dataClass.dataSetClasses.size() == 0)) {
            return 0 // nominal header row will be added later on
        }
        // Specific rule for the 'Patient Identity' blocks
        if(dataClass.isChoice &&
           dataClass.dataSetClasses.size() == 1 &&
           (!dataClass.dataSetClasses.first().dataSetClasses || dataClass.dataSetClasses.first().dataSetClasses.size() == 0) &&
           dataClass.dataSetClasses.first().isAnd  /* &&
                (!dataClass.childDataClasses.first().childDataClasses || dataClass.childDataClasses.first().childDataClasses.size() == 0) &&
                dataClass.childDataClasses.first().childDataElements.size() == 1 &&
                (!dataClass.childDataClasses[1].childDataClasses || dataClass.childDataClasses[1].childDataClasses.size() == 0) &&
                DataSetParser.isAnd(dataClass.childDataClasses[1] */)
        {
            return 1
        }
        int total = 0
        if(dataClass.dataSetElements) {
            total += dataClass.dataSetElements.size()
        }

        dataClass.dataSetClasses.each {
            total += calculateClassRows(it) + 1
        }
        if(dataClass.isChoice && dataClass.dataSetClasses && dataClass.dataSetClasses.size() > 0) {
            // To cope with the 'or' rows
            total += dataClass.dataSetClasses.size()
        }

        log.debug("Data Class Rows: ${dataClass.name} - ${total}")
        return total

    }

    void addCDSChildRow(NhsDDDataSetClass dataClass, int totalDepth, int currentDepth){

        if(dataClass.isChoice || dataClass.isAnd) {
           // && dataClass.name.startsWith("Choice") || dataClass.name.startsWith("And")

            List<String> mro = []
            List<String> groupRepeats = []
            List<Object> xRefs = [] // mixture of elements and strings
            List<String> rules = []
            boolean rulesSet = false
            if(dataClass.rules) {
                if(dataClass.rules) {
                    dataClass.rules.split(";").each {
                        rules.add(it)
                    }
                }
                rulesSet = true
            }

            dataClass.sortedChildren.eachWithIndex {childCatalogueItem, idx ->
                if(childCatalogueItem instanceof NhsDDDataSetElement) {
                    NhsDDDataSetElement childDataElement = (NhsDDDataSetElement) childCatalogueItem
                    if (idx != 0) {
                        if(dataClass.isChoice) {
                            xRefs.add('Or')
                            mro.add('Or')
                            groupRepeats.add('Or')
                            if(!rulesSet) {
                                rules.add('&nbsp;')
                            }
                        } else if(dataClass.isAnd) {
                            xRefs.add('And')
                            mro.add('And')
                            groupRepeats.add('And')
                            if (!rulesSet) {
                                rules.add('&nbsp;')
                            }
                        }
                    }
                    mro.add(childDataElement.mandation)
                    groupRepeats.add(childDataElement.groupRepeats)
                    if(!rulesSet && childDataElement.rules) {
                        childDataElement.rules.split(";").each {
                            rules.add(it)
                        }
                    }
                    xRefs.add(childDataElement)
                } else {
                    NhsDDDataSetClass childDataClass = (NhsDDDataSetClass) childCatalogueItem
                    childDataClass.getSortedChildren().eachWithIndex { NhsDDDataSetElement childDataElement, idx2 ->
                        if (idx != 0 && idx2 == 0) {
                            xRefs.add('OR')
                            mro.add('OR')
                            groupRepeats.add('OR')
                            if(!rulesSet) {
                                rules.add('&nbsp;')
                            }
                        }
                        if (idx2 != 0) {
                            xRefs.add('And')
                            mro.add('And')
                            groupRepeats.add('And')
                            if(!rulesSet) {
                                rules.add('&nbsp;')
                            }
                        }
                        mro.add(childDataElement.mandation)
                        groupRepeats.add(childDataElement.groupRepeats)
                        if(!rulesSet && childDataElement.rules) {
                            childDataElement.rules.split(";").each {
                                rules.add(it)
                            }
                        }
                        xRefs.add(childDataElement)
                    }
                }
            }
            markupBuilder.tr {
                td (class: 'mandation') {
                    mro.each {
                        p it
                    }
                }
                td (class: 'groupRepeats') {
                    groupRepeats.each {
                        p it
                    }
                }
                td (class: 'elementBox', colspan: (totalDepth*2+3) - (currentDepth*2+3)) {
                    xRefs.each { elementObject ->
                        p {
                            if(elementObject instanceof String) {
                                mkp.yield(elementObject)
                            } else {
                                ((NhsDDDataSetElement) elementObject).createLink(markupBuilder)
                            }
                        }
                    }
                }
                td (class: 'rules') {
                    rules.each {
                        p it
                    }
                }
            }
        }
    }

    void addCDSChildRow(NhsDDDataSetElement dataElement, int totalDepth, int currentDepth) {
        markupBuilder.tr {
            td (class: 'mandation') {
                p dataElement.mandation

            }
            td (class: 'groupRepeats') {
                p dataElement.groupRepeats

            }
            td (class: 'elementBox', colspan: (totalDepth*2+3) - (currentDepth*2+3)) {
                p {
                    dataElement.createLink(markupBuilder)
                }
            }
            td (class: 'rules') {
                if(dataElement.rules) {
                    dataElement.rules.split(";").each {
                        p it
                    }
                }
            }
        }
    }

}
