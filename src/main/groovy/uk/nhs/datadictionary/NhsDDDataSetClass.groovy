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

import groovy.util.logging.Slf4j
import org.maurodata.dita.elements.langref.base.Div
import org.maurodata.dita.elements.langref.base.Entry
import org.maurodata.dita.elements.langref.base.P
import org.maurodata.dita.elements.langref.base.Row
import org.maurodata.dita.elements.langref.base.Table
import org.maurodata.dita.elements.langref.base.XRef
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.facet.Metadata
import uk.nhs.datadictionary.publish.structure.ExternalLink
import uk.nhs.datadictionary.publish.structure.ItemLink
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetAddressCell
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetCell
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetChoiceCell
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetGroup
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetHeader
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetHeaderType
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetItemLinkCell
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetRow
import uk.nhs.datadictionary.publish.structure.datasets.other.OtherDataSetTable

@Slf4j
class NhsDDDataSetClass implements NhsDDDataSetComponent {

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    Boolean isChoice
    Boolean isAnd
    Boolean isInclusiveOr
    Integer webOrder
    Boolean isAddress
    Boolean isDataSetReference
    String dataSetReferenceTo
    String groupRepeats
    String multiplicityText
    String rules
    String address1
    String address2

    List<NhsDDDataSetClass> dataSetClasses = []
    List<NhsDDDataSetElement> dataSetElements = []

    NhsDataDictionary dataDictionary

    // For testing only
    NhsDDDataSetClass() {
    }

    NhsDDDataSetClass(DataClass dataClass) {
        this.name = dataClass.label
        this.description = dataClass.description

    }


    NhsDDDataSetClass(DataClass dataClass, NhsDataDictionary dataDictionary) {
        this(dataClass)
        this.dataDictionary = dataDictionary

        List<Metadata> thisClassMetadata
        if(dataDictionary && !dataDictionary.dataSetsMetadata.isEmpty()) {
             thisClassMetadata = dataDictionary.dataSetsMetadata[dataClass.id]
        } else {
            thisClassMetadata = Metadata.byMultiFacetAwareItemId(dataClass.id).list()
        }

        isChoice = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_CHOICE
                && it.value == "true")
        }
        isAnd = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_AND
                && it.value == "true")
        }
        isInclusiveOr = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_INCLUSIVE_OR
                && it.value == "true")
        }
        isAddress = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
                && it.key == NhsDataDictionary.DATASET_TABLE_KEY_ADDRESS_CHOICE
                && it.value == "true")
        }
        address1 = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                && it.key == "Address 1")
        }?.value ?: ""
        address2 = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                && it.key == "Address 2")
        }?.value ?: ""

        mandation = thisClassMetadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_MRO  }?.value
        constraints = thisClassMetadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_RULES  }?.value
        isDataSetReference = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_DATASET_TABLE_NAMESPACE
                    && it.key == NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE
                    && it.value == "true")
        }
        dataSetReferenceTo = thisClassMetadata.find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_DATA_SET_REFERENCE_TO }?.value
        groupRepeats = thisClassMetadata.find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_GROUP_REPEATS }?.value
        multiplicityText = thisClassMetadata.find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_MULTIPLICITY_TEXT }?.value
        rules = thisClassMetadata.find { it.key == NhsDataDictionary.DATASET_TABLE_KEY_RULES  }?.value

        Metadata md = thisClassMetadata.find {it.key == NhsDataDictionary.DATASET_TABLE_KEY_WEB_ORDER }
        if(md) {
            webOrder = Integer.parseInt(md.value)
        } else {
            webOrder = dataClass.idx
        }

        dataClass.dataClasses.each {childDataClass ->
            dataSetClasses.add(new NhsDDDataSetClass(childDataClass, dataDictionary))
        }
        dataSetClasses = dataSetClasses.sort { it.webOrder }
        /*
        dataClass.getImportedDataElements().each {dataElement ->
            dataSetElements.add(new NhsDDDataSetElement(dataElement, dataDictionary))
        }
         */
        dataClass.getDataElements().each { dataElement ->
            dataSetElements.add(new NhsDDDataSetElement(dataElement, dataDictionary))
        }

        dataSetElements = dataSetElements.sort {it.webOrder}

    }

    List<NhsDDDataSetComponent> getSortedChildren() {
        List<NhsDDDataSetComponent> children = []
        children.addAll(dataSetClasses)
        children.addAll(dataSetElements)
        children = children.sort { it.webOrder }
        return children
    }

    Set<NhsDDDataSetElement> getAllElements() {
        Set<NhsDDDataSetElement> returnElements = [] as Set<NhsDDDataSetElement>
        dataSetClasses.each {dataSetClass ->
            returnElements.addAll(dataSetClass.getAllElements())
        }
        returnElements.addAll(dataSetElements)
        return returnElements
    }

    OtherDataSetTable buildOtherDataSetTable() {
        OtherDataSetHeader header = new OtherDataSetHeader(
            OtherDataSetHeaderType.TABLE,
            this.name,
            this.description)

        List<OtherDataSetGroup> groups = buildOtherDataSetGroups()

        // If this table class is a choice, then add an "Or" separator between groups
        new OtherDataSetTable(header, groups, isChoice ? "Or" : "")
    }

    List<OtherDataSetGroup> buildOtherDataSetGroups(OtherDataSetHeader header = null) {
        List<OtherDataSetGroup> groups = []

        if (dataSetElements && dataSetElements.size() > 0) {
            List<OtherDataSetRow> rows = getSortedChildren().collect {dataSetComponent ->
                buildOtherDataSetRow(dataSetComponent)
            }
            groups.add(new OtherDataSetGroup(rows, header))
        }
        else {
            dataSetClasses.each { dataSetClass ->
                groups.addAll(dataSetClass.buildOtherDataSetGroup())
            }
        }

        groups
    }

    List<OtherDataSetGroup> buildOtherDataSetGroup() {
        OtherDataSetHeader groupHeader = this.name != "Choice"
            ? new OtherDataSetHeader(OtherDataSetHeaderType.GROUP, this.name, this.description)
            : null

        buildOtherDataSetGroups(groupHeader)
    }

    static OtherDataSetRow buildOtherDataSetRow(NhsDDDataSetComponent dataSetComponent) {
        new OtherDataSetRow(dataSetComponent.mandation, buildOtherDataSetCell(dataSetComponent))
    }

    static OtherDataSetCell buildOtherDataSetCell(NhsDDDataSetComponent dataSetComponent) {
        if (dataSetComponent instanceof NhsDDDataSetClass) {
            NhsDDDataSetClass dataSetClass = dataSetComponent as NhsDDDataSetClass
            return dataSetClass.isAddress ? dataSetClass.buildOtherDataSetAddressCell() : dataSetClass.buildOtherDataSetChoiceCell()
        }

        NhsDDDataSetElement dataSetElement = dataSetComponent as NhsDDDataSetElement
        dataSetElement.buildOtherDataSetItemLinkCell()
    }

    OtherDataSetCell buildOtherDataSetAddressCell() {
        // See COSDS Pathology for an example of this
        // We store "Address 1" and "Address 2" as values in the metadata, but given that all occurrences of this
        // are currently the same, we'll hard-code the links in for now
        NhsDDDataSetElement dataSetElement = dataSetElements.first()

        new OtherDataSetAddressCell(
            ItemLink.create(dataSetElement.reuseElement),
            new ExternalLink("ADDRESS STRUCTURED", this.address1, "class"),
            new ExternalLink("ADDRESS UNSTRUCTURED", this.address2, "class"))
    }

    OtherDataSetCell buildOtherDataSetChoiceCell() {
        String operator = ""
        if (isChoice && name.startsWith("Choice")) {
            operator = OtherDataSetChoiceCell.OR_OPERATOR
        }
        else if (isAnd && (name.startsWith("Choice") || name.startsWith("And"))) {
            operator = OtherDataSetChoiceCell.AND_OPERATOR
        }
        else if (isInclusiveOr && name.startsWith("Choice")) {
            operator = OtherDataSetChoiceCell.AND_OR_OPERATOR
        }

        List<OtherDataSetItemLinkCell> itemLinkCells = []

        List<NhsDDDataSetComponent> children = this.getSortedChildren()
        children.each { childItem ->
            if (childItem instanceof NhsDDDataSetElement) {
                NhsDDDataSetElement dataSetElement = childItem as NhsDDDataSetElement
                itemLinkCells.add(dataSetElement.buildOtherDataSetItemLinkCell())
            }
        }

        new OtherDataSetChoiceCell(operator, itemLinkCells)
    }

    // Remove this one day, "Other" data set render replaced with buildOtherDataSetTable()
    @Deprecated
    Div outputClassAsDita(NhsDataDictionary dataDictionary) {
        Div.build {
            if (isChoice && name.startsWith("Choice")) {
                b "One of the following options must be used:"
                dataSetClasses.eachWithIndex { childDataClass, idx ->
                    if (idx != 0) {
                        b "Or"
                    }
                    div childDataClass.outputClassAsDita(dataDictionary)
                }
            } else {
                table(outputClass: "table table-striped table-bordered table-sm") {
                    tgroup(cols: 2) {
                        colspec(colnum: 1, colName: "col1", colwidth: "2*", align: Align.CENTER)
                        colspec(colnum: 2, colName: "col2", colwidth: "8*")
                        tHead {
                            row(outputClass: "thead-light") {
                                entry(namest: "col1", nameend: "col2") {
                                    b name
                                    if (description) {
                                        p description
                                    }
                                }
                            }
                            if (dataSetElements && dataSetElements.size() > 0) {
                                row {
                                    entry {
                                        p "Mandation"
                                    }
                                    entry {
                                        p "Data Elements"
                                    }
                                }
                            }

                        }
                        tBody {
                            addAllChildRows().each {childRow ->
                                row childRow
                            }
                        }
                    }
                }
            }
        }

    }

    List<Row> addAllChildRows() {
        List<Row> rows = []
        if(dataSetElements && dataSetElements.size() > 0) {
            getSortedChildren().each {classOrElement ->
                rows.add(addChildRow(classOrElement))
            }
        } else {
            dataSetClasses.eachWithIndex { dataSetClass, idx ->
                if(isChoice && idx != 0) {
                    rows.add(Row.build {
                        entry (namest: "col1", nameend: "col2") {
                            b 'Or'
                        }
                    })

                } else if(idx != 0) {
                    rows.add(Row.build {
                        entry (namest: "col1", nameend: "col2") {
                            p ''
                        }

                    })
                }
                rows.addAll(dataSetClass.addAsSubClass())
            }
        }
        return rows
    }

    static Row addChildRow(def classOrElement) {
        String mro = classOrElement.mandation
        Row.build {
            entry {
                p mro
            }
            entry {
                getXRefStringFromItem(classOrElement).each {paragraph ->
                    p paragraph
                }
            }
        }
    }

    static List<P> getXRefStringFromItem(def classOrElement) {
        if (classOrElement instanceof NhsDDDataSetElement) {
            return ((NhsDDDataSetElement) classOrElement).createEntryParagraphs()
        } else { //if (catalogueItem instanceof NhsDDDataSetClass)
            return ((NhsDDDataSetClass) classOrElement).createEntryParagraphs()
        }
    }

    List<P> createEntryParagraphs() {
        if(isAddress) { // See COSD Pathology for an example of this
            // We store "Address 1" and "Address 2" as values in the metadata, but given that all occurrences of this
            // are currently the same, we'll hard-code the links in for now

            List<P> paragraphs = dataSetElements[0].createEntryParagraphs()
            XRef firstLink = paragraphs[0].getXRefs()[0]
            paragraphs.add(1, P.build {text "Or"})
            paragraphs[0].text " - "
            paragraphs[0].xRef (outputClass: "class", keyRef: "class_address_structured")
            paragraphs.add(P.build {
                xRef firstLink
                text " - "
                xRef (outputClass: "class", keyRef: "class_address_unstructured")
            })
            return paragraphs
        } else if (isChoice && name.startsWith("Choice")) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "Or"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        } else if (isAnd && (name.startsWith("Choice") || name.startsWith("And"))) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "And"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        } else if (isInclusiveOr && name.startsWith("Choice")) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "And/Or"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        }
        return []
    }

    List<Row> addAsSubClass() {
        List<Row> rows = []
        if(name != "Choice") {
            rows.add(Row.build(outputClass: "table-primary") {
                entry(namest: "col1", nameend: "col1") {
                    p {
                        b "Mandation"
                    }
                }
                entry(namest: "col2", nameend: "col2") {
                    b name
                    if(description) {
                        p description
                    }
                }
            })
        }
        rows.addAll(addAllChildRows())
        return rows
    }

    Div outputCDSClassAsDita(NhsDataDictionary dataDictionary) {

        int totalDepth = calculateClassDepth()
        /*if(totalDepth > 2) {
            totalDepth = 2
        } */

        return Div.build {

            if (isChoice && name.startsWith("Choice")) {
                p {
                    b "One of the following options must be used:"
                }

                dataSetClasses
                        .eachWithIndex { childDataClass, idx ->
                            if (idx != 0) {
                                p {
                                    b "Or"
                                }
                            }
                            div childDataClass.outputCDSClassAsDita(dataDictionary)
                        }
            } else if (!dataSetClasses && !dataSetElements && isDataSetReference) {
                // We've got a pointer to another dataset
                //String linkedDataSetUrl = DataSetParser.getDataSetReferenceTo(dataClass)
                //System.err.println(dataClass.label.replaceFirst("DATA GROUP: ", ""))
                NhsDDDataSet linkedDataSet = dataDictionary.dataSets[dataSetReferenceTo]
                if (!linkedDataSet) {
                    this.log.error("Cannot link dataset: ${dataSetReferenceTo}")
                } else {
                    Table headerTable = addTopLevelCDSHeader(linkedDataSet.name, totalDepth, false)
                    headerTable.tgroups[0].tBody {
                        row {
                            entry(align: Align.CENTER) {
                                p mandation
                            }
                            entry(align: Align.CENTER) {
                                p groupRepeats
                            }
                            entry(namest: "col3", nameend: "col${totalDepth * 2 + 4}") {
                                p {
                                    b "Data Group:"
                                    xRef DitaHelper.getXRef(linkedDataSet)
                                }
                                p multiplicityText
                            }
                        }
                    }
                    table headerTable
                }

            } else {
                String label = name
                if (label.endsWith(" 1")) {
                    label = label.replaceAll(" 1", "")
                }
                Table headerTable = addTopLevelCDSHeader(label, totalDepth, true)

                headerTable.getTgroups()[0].tBody {
                    addCDSClassContents(dataDictionary, totalDepth, 0).each {classRow ->
                        row classRow
                    }
                }
                table headerTable
            }
        }
    }

    Table addTopLevelCDSHeader(String groupName, int totalDepth, boolean includeMRO = true) {
        List<String> columnWidths = []
        for(int i=0;i<totalDepth*2;i++) {
            columnWidths.add("1*")
        }
        columnWidths.addAll(["1*", "1*", "8*", "1*"])

        return Table.build(outputClass: "table table-striped table-bordered") {
            tgroup(cols: columnWidths.size()) {
                columnWidths.eachWithIndex{ String colWidth, int i ->
                    colspec(colnum: i+1, colName: "col${i+1}", colwidth: colWidth)
                }
                tHead {
                    row {
                        entry(namest: "col1", nameend: "col2", align: Align.CENTER) {
                            xRef(keyRef: "supporting_information_commissioning_data_set_notation"){text "Notation" }
                        }
                        entry(namest: "col3", nameend: "col${totalDepth*2+4}", outputClass: "thead-light") {
                            p {
                                b "Data Group:"
                                text groupName
                            }
                        }
                    }
                    row(outputClass: "thead-light") {
                        entry(namest: "col1", nameend: "col1", align: Align.CENTER) {
                            p "Group Status"
                            if(includeMRO) {
                                p mandation
                            }
                        }
                        entry(namest: "col2", nameend: "col2", align: Align.CENTER) {
                            p NhsDataDictionary.DATASET_TABLE_KEY_GROUP_REPEATS
                            if(includeMRO) {
                                p groupRepeats
                            }
                        }
                        entry(namest: "col3", nameend: "col${totalDepth*2+4}") {
                            p {
                                b "Function: "
                                text description
                            }
                        }
                    }
                }
            }
        }
    }


    List<Row> addCDSClassContents(NhsDataDictionary dataDictionary, int totalDepth, int currentDepth) {
        List<Row> rows = []
        if (dataSetElements) {
            if(isChoice) {
                Row row = addCDSChildRow(totalDepth, currentDepth)
                rows.add(row)
            } else {
                sortedChildren.each { child ->
                    Row row = child.addCDSChildRow(totalDepth, currentDepth)
                    rows.add(row)
                }
            }
        } else { // no dataElements
            if (dataSetClasses) {
                if (isChoice) {
                    rows.add(Row.build {
                        entry(namest: "col${currentDepth * 2 + 1}", nameend: "col${totalDepth * 2 + 4}") {
                            p {
                                b "One of the following DATA GROUPS must be used:"
                            }
                        }
                    })
                }
                sortedChildren.each { NhsDDDataSetClass childComponent ->
                    if (childComponent instanceof NhsDDDataSetClass && childComponent.name.startsWith("And") && childComponent.isAnd) {
                        childComponent.sortedChildren.each { NhsDDDataSetClass subChild ->
                            rows.addAll(subChild.addCDSSubClass(dataDictionary, totalDepth, currentDepth + 1))
                        }
                    } else {
                        int newCurrentDepth = currentDepth + 1
                        if (newCurrentDepth > totalDepth) {
                            newCurrentDepth = totalDepth
                        }
                        rows.addAll(childComponent.addCDSSubClass(dataDictionary, totalDepth, newCurrentDepth))
                    }
                    if (isChoice && childComponent != sortedChildren.last()) {
                        rows.add(Row.build {
                            entry (namest: "col${currentDepth * 2 + 1}",nameend: "col${totalDepth * 2 + 4}") {
                                p {
                                    b "Or"
                                }
                            }
                        })
                    }
                }
            } else {
                log.error("No data elements or data classes... ")
                log.error(name)
            }
        }
        return rows
    }

    Row addCDSChildRow(int totalDepth, int currentDepth){
        Row row = Row.build {
            if (isChoice || isAnd) {
                Entry mandationEntry = Entry.build(align: Align.CENTER) {}
                Entry groupRepeatsEntry = Entry.build(align: Align.CENTER) {}
                Entry xRefsEntry = Entry.build(namest: "col${currentDepth * 2 + 3}", nameend: "col${totalDepth * 2 + 3}") {}
                Entry rulesEntry = Entry.build(align: Align.CENTER) {}
                boolean rulesSet = false
                if (this.rules) {
                    rules.split(';').each {
                        rulesEntry.p it
                    }
                    rulesSet = true
                }
                sortedChildren.eachWithIndex { childComponent, idx ->
                    if (childComponent instanceof NhsDDDataSetElement) {
                        NhsDDDataSetElement childDataElement = (NhsDDDataSetElement) childComponent
                        if (idx != 0) {
                            if (isChoice) {
                                xRefsEntry.p "Or"
                                mandationEntry.p "Or"
                                groupRepeatsEntry.p "Or"
                                if (!rulesSet) {
                                    rulesEntry.p "&nbsp;"
                                }
                            } else if (isAnd) {
                                xRefsEntry.p "And"
                                mandationEntry.p "And"
                                groupRepeatsEntry.p "And"
                                if (!rulesSet) {
                                    rulesEntry.p "&nbsp;"
                                }

                            }
                        }
                        if(childDataElement.mandation) {
                            childDataElement.mandation.split(';').each {
                                mandationEntry.p it
                            }
                        }
                        if(childDataElement.groupRepeats) {
                            childDataElement.groupRepeats.split(';').each {
                                groupRepeatsEntry.p it
                            }
                        }
                        if (!rulesSet) {
                            if(childDataElement.rules) {
                                childDataElement.rules.split(';').each {
                                    rulesEntry.p it
                                }
                            }
                        }
                        if(childDataElement.reuseElement) {
                            xRefsEntry.xRef DitaHelper.getXRef(childDataElement.reuseElement)
                        } else {
                             xRefsEntry.p childDataElement.name
                        }
                    } else {
                        NhsDDDataSetClass childDataClass = (NhsDDDataSetClass) childComponent

                        childDataClass.sortedChildren.eachWithIndex { NhsDDDataSetElement childDataElement, idx2 ->
                            // Assume there are only children here
                            if (idx != 0 && idx2 == 0) {
                                xRefsEntry.p "OR"
                                mandationEntry.p "OR"
                                groupRepeatsEntry.p "OR"
                                if (!rulesSet) {
                                    rulesEntry.p "&nbsp;"
                                }
                            }

                            if (idx2 != 0) {
                                xRefsEntry.p "AND"
                                mandationEntry.p "AND"
                                groupRepeatsEntry.p "AND"
                                if (!rulesSet) {
                                    rulesEntry.p "&nbsp;"
                                }
                            }
                            if(childDataElement.mandation) {
                                childDataElement.mandation.split(";").each {
                                    mandationEntry.p it
                                }
                            }
                            if(childDataElement.groupRepeats) {
                                childDataElement.groupRepeats.split(";").each {
                                    groupRepeatsEntry.p it
                                }
                            }
                            if (!rulesSet && rules) {
                                childDataElement.rules.split(";").each {
                                    rulesEntry.p it
                                }
                            }
                            if(childDataElement.reuseElement) {
                                xRefsEntry.xRef DitaHelper.getXRef(childDataElement.reuseElement)
                            } else {
                                xRefsEntry.p childDataElement.name
                            }

                        }
                    }
                }
                entry mandationEntry
                entry groupRepeatsEntry
                entry xRefsEntry
                entry rulesEntry
            }
        }
        return row
    }

    List<Row> addCDSSubClass(NhsDataDictionary dataDictionary, int totalDepth, int currentDepth) {
        List<Row> rows = []
        Row row = addCDSClassHeader(dataDictionary, totalDepth, currentDepth)
        if(row) {
            rows.add(row)
        }
        rows.addAll(addCDSClassContents(dataDictionary, totalDepth, currentDepth))
        return rows
    }

    Row addCDSClassHeader(NhsDataDictionary dataDictionary, int totalDepth, int currentDepth) {
        if(name.startsWith("Choice") || name.startsWith("And")) {
            return null
        }
        int moreRows = calculateClassRows()
        return Row.build(outputClass: "thead-light table-primary") {
            entry(align: Align.CENTER, morerows: moreRows) {
                p mandation
            }
            entry(align: Align.CENTER, morerows: moreRows) {
                p groupRepeats
            }
            entry(namest: "col${currentDepth*2+1}", nameend: "col${totalDepth*2+3}") {
                p {
                    b name.replace([" 1": "", " 2":"", " 3":""])
                }
                if(description) {
                    p description
                }
            }
            entry(align: Align.CENTER) {
                p {
                    xRef(keyRef: "supporting_information_commissioning_data_set_business_rules"){
                        text "Rules"
                    }
                }
            }
        }
    }

    int calculateClassRows() {

        if((isAnd || isChoice) && (!dataSetClasses || dataSetClasses.size() == 0)) {
            return 0 // nominal header row will be added later on
        }
        // Specific rule for the 'Patient Identity' blocks
        if(isChoice &&
                dataSetClasses.size() == 1 &&
                (!dataSetClasses.first().dataSetClasses || dataSetClasses.first().dataSetClasses.size() == 0) &&
                (dataSetClasses.first().isAnd))
        {
            log.info("Found special case! " + name)
            return 1
        }
        int total = 0
        if(dataSetElements) {
            total += dataSetElements.size()
        }

        dataSetClasses.each {
            total += it.calculateClassRows() + 1
        }
        if(isChoice && dataSetClasses && dataSetClasses.size() > 0) {
            // To cope with the 'or' rows
            total += dataSetClasses.size()
        }

        log.debug("Data Class Rows: ${name} - ${total}")
        return total
    }


    int calculateClassDepth() {
        int maxDepth = 0
        if(dataSetClasses) {
            maxDepth = dataSetClasses.collect {it.calculateClassDepth()}.max()
        }
        if(isAnd || isChoice) {
            return maxDepth
        } else {
            return maxDepth + 1
        }

    }

}
