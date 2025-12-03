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
package uk.nhs.datadictionary.datasets.parser

import groovy.util.logging.Slf4j
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.utils.DDHelperFunctions

import java.time.Duration
import java.time.Instant

@Slf4j
class CDSDataSetParser {


    static void parseCDSDataSet(Node definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> dataClasses = []

        Instant startTime = Instant.now()
        dataClasses = parseCDSDataSetWithHeaderTables(definition, dataModel, dataDictionary)
        log.debug('Initial parse data set complete in {}', Duration.between(startTime, Instant.now()).toString())
        startTime = Instant.now()
        dataClasses.eachWithIndex {dataClass, index ->
            dataModel.childDataClasses.add(dataClass)
            if(!dataClass.label || dataClass.label == "") {
                log.error("Class with unset label: ${dataModel.label}")
                log.error(dataClass.toString())
                dataClass.label = "Test Class Label"
            }
            //DataSetParser.setOrder(dataClass, index + 1)
        }
        log.debug('Add to DCs data set complete in {}', Duration.between(startTime, Instant.now()).toString())
        startTime = Instant.now()
        DataSetParser.fixPotentialDuplicates(dataModel)
        log.debug('Fix duplicates complete in {}', Duration.between(startTime, Instant.now()).toString())
    }

    static List<DataClass> parseCDSDataSetWithHeaderTables(Node definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> returnDataClasses = []
        // log.debug(partitionByDuckBlueClasses(definition).size())
        List<List<Node>> sections = DataSetParser.partitionByDuckBlueClasses(definition)
        for (int sectIdx = 0; sectIdx < sections.size(); sectIdx++) {
            List<Node> sect = sections.get(sectIdx)
            List<Node> ors = sect.findAll {
                (it.name() == "table" && it.text().trim() == "OR") ||
                (it.name() == "strong" && it.text().trim() == "OR")
            }
            if (ors.size() == 1 && sect.size() == 3) {
                DataClass choiceClass = new DataClass(label: "Choice")
                DataSetParser.setChoice(choiceClass)
                List<DataClass> classes1 = parseCDSSection(sect, dataModel, dataDictionary)
                sectIdx++
                sect = sections.get(sectIdx)
                List<DataClass> classes2 = parseCDSSection(sect, dataModel, dataDictionary)

                if (classes1.size() != 1 || classes2.size() != 1) {
                    log.warn("Oh no!  More than one CDS class returned!")
                } else {
                    choiceClass.dataClasses.add(classes1.get(0))
                    choiceClass.dataClasses.add(classes2.get(0))
                }
                returnDataClasses.add(choiceClass)
            } else {
                List<DataClass> sectionClasses = parseCDSSection(sect, dataModel, dataDictionary)
                returnDataClasses.addAll(sectionClasses)
            }

        }
        return returnDataClasses
    }

    static void parseCDSElementTable(List<Node> tableRows, DataClass currentClass, DataModel dataModel, NhsDataDictionary dataDictionary,
                                     int position) {
        if (tableRows.size() == 0) {return} // We're done
        Node firstRow = tableRows[0]

        if (tableRows.size() == 1 && firstRow.td[0].@bgcolor == "#DCDCDC" && firstRow.td.size() == 3) {

            // Single row, pointer to data class
            //DataSetParser.setMRO(currentClass, firstRow.td[0].text())
            //DataSetParser.setGroupRepeats(currentClass, firstRow.td[1].text())
            DataSetParser.setDataSetReference(currentClass)
            if (firstRow.td[2].strong.a.size() > 0) {
                String dataSetName = dataDictionary.dataSetNamesByDDUrl[firstRow.td[2].strong.a[0].@href]
                DataSetParser.setDataSetReferenceTo(currentClass, dataSetName)
            } else {
                String dataSetName = dataDictionary.dataSetNamesByDDUrl[firstRow.td[2].a[0].@href]
                DataSetParser.setDataSetReferenceTo(currentClass, dataSetName )
            }

            DataSetParser.setMultiplicityText(currentClass, firstRow.td[2].text())
        } else if (firstRow.td.size() == 4 && firstRow.td[2].@bgcolor == "#DCDCDC") {
            // Let's start parsing a class
            DataClass childClass = getClassFromTD(firstRow.td[2], dataDictionary, dataModel)
            //DataClass childClass = new DataClass(label: "Option 2")
            DataSetParser.setMRO(childClass, firstRow.td[0].text())
            DataSetParser.setGroupRepeats(childClass, firstRow.td[1].text())
            DataSetParser.setOrder(childClass, position)
            currentClass.dataClasses.add(childClass)
            Integer noRows = 1
            if (firstRow.td[0].@rowspan) {
                noRows = Integer.parseInt(firstRow.td[0].@rowspan)
                //log.debug("noRows: " + noRows)
            }
            if (tableRows[noRows] && tableRows[noRows].td.size() == 6
                && (tableRows[noRows].td[4].text() == "PERSON BIRTH DATE" || tableRows[noRows].td[4].text() == "PERSON_BIRTH_DATE")) {
                noRows++
            }
            List<Node> classRows = tableRows.take(noRows).drop(1)
            parseCDSElementTable(classRows, childClass, dataModel, dataDictionary, position)
            List<Node> otherRows = tableRows.drop(noRows)
            parseCDSElementTable(otherRows, currentClass, dataModel, dataDictionary, position + 1)
        } else if ((firstRow.td.size() == 2 || firstRow.td.size() == 3)
            && firstRow.td[1].@bgcolor == "#DCDCDC") {
            // This is a class like that of "Patient Identity"
            DataClass childClass = getClassFromTD(firstRow.td[1], dataDictionary, dataModel)
            //DataSetParser.setMRO(childClass, firstRow.td[0].text())
            DataSetParser.setGroupRepeats(childClass, firstRow.td[0].text())
            DataSetParser.setOrder(childClass, position)
            currentClass.dataClasses.add(childClass)
            tableRows = tableRows.drop(1)

            parseCDSElementTable(tableRows, childClass, dataModel, dataDictionary, 1)

        } else if (firstRow.td.size() == 1 && tableRows.size() == 1 && firstRow.td.strong.size() == 1) {
            // This is probably "One of the following two options must be used",
            // "OR", or similar.
            log.error("Ignoring: " + firstRow.td.strong.text())
        } else if (firstRow.td.size() == 1 && firstRow.td.@bgcolor == "white"
            && firstRow.td.strong.size() == 0
            && tableRows[1].td.size() == 1) {
            // This is in the "BABY" groups, where we're starting a choice
            //log.debug("Blank line")
            //log.debug(tableRows.size())
            tableRows = tableRows.drop(1)

            DataClass choiceClass = getClassFromTD(tableRows[0].td[0], dataDictionary, dataModel)
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            currentClass.dataClasses.add(choiceClass)
            tableRows = tableRows.drop(1)
            List<List<Node>> partitions =
                DataSetParser.partitionList(tableRows,
                                            {it ->
                                                (it.td.size() == 1
                                                    && it.td.strong.size() == 1
                                                    && (it.td.text().trim() == "OR" || it.td.text().trim() == "and"))
                                            })
            //log.debug(partitions.size())
            partitions.eachWithIndex {part, idx ->
                parseCDSElementTable(part, choiceClass, dataModel, dataDictionary, idx + 1)
            }

        } else if (firstRow.td.size() == 4 &&
                   (firstRow.td[2].@bgcolor == "white" || !firstRow.td[2].@bgcolor)) {
            parseCDSElement(dataModel, currentClass, firstRow, dataDictionary, position)
            List<Node> nextRows = tableRows.drop(1)
            parseCDSElementTable(nextRows, currentClass, dataModel, dataDictionary, position + 1)
        } else if (firstRow.td.size() == 6 &&
                   (firstRow.td[4].@bgcolor == "white" || !firstRow.td[4].@bgcolor)
            && (firstRow.td[4].text() == "PERSON BIRTH DATE" || firstRow.td[4].text() == "PERSON_BIRTH_DATE" || firstRow.td[4].text() ==
                "PROFESSIONAL_REGISTRATION_ISSUER_CODE")) {
            parseCDSElement(dataModel, currentClass, firstRow, dataDictionary, position)
            List<Node> nextRows = tableRows.drop(1)
            parseCDSElementTable(nextRows, currentClass, dataModel, dataDictionary, position + 1)
        } else {
            log.error("Cannot pattern match row: {}\n{}\n{}",
                      dataModel.label,
                      currentClass.label,
                      firstRow.toString())
        }


    }

    static String cleanWhiteSpace(String content) {
        if (!content) return null
        // Replace then trim as NBSP is not whitespace and could be removed to result in WS left over
        String trimmed = content.replaceAll(' ', '').trim()
        trimmed ?: null
    }

    static DataClass getClassFromTD(Node td, NhsDataDictionary dataDictionary, DataModel dataModel) {
        DataClass dataClass = new DataClass(label: "")
        dataModel.childDataClasses.add(dataClass)

        // Issue where the trimmed content == NBSP so we need to trim it then check that for NBSP
        def firstStringNode = td.depthFirst().find {
            it instanceof String ||
            (it instanceof Node && it.localText().size() > 0 &&
             it.localText().any {cleanWhiteSpace(it)})
        }

        String label

        if (firstStringNode instanceof String) {
            label = firstStringNode
        } else {
            label = (firstStringNode as Node).localText().find {cleanWhiteSpace(it)}
        }
        /*
        dataClass.description = XmlUtil.serialize(tdNode).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
        dataClass.description = dataClass.description.replaceFirst(dataClass.label, "")
            .replaceFirst("<td[^>]*>", "")
            .replaceFirst("</td>","")

         */
        dataClass.label = label.replaceFirst("DATA GROUP:", '').trim()

        if (dataClass.label == " ") {
            log.debug("NBSP Found for DataClass name. Extracted Label:[{}]\nXML: {}", label, td)
        }

        return dataClass

    }

    static String getFirstStringNode(Node node) {
        if (node.children()) {
            node.children().each {it ->
                if ((it instanceof Node)) {
                    return getFirstStringNode(it)
                }

            }

        }
    }


    static List<DataClass> parseCDSSection(List<Node> components, DataModel dataModel, NhsDataDictionary dataDictionary) {
        Instant startTime = Instant.now()
        List<DataClass> returnClasses = []
        DataClass currentClass = null
        int classWebOrder = 0
        int ors = components.count {
            (it.name() == "table" && it.text().trim() == "OR") ||
            (it.name() == "strong" && it.text().trim() == "OR")
        }

        components.each {component ->
            if (component instanceof Node && component.name() == "table" && DDHelperFunctions.tableIsClassHeader(component)) {
                currentClass = new DataClass(label: component.tbody.tr[0].td[1].text())
                dataModel.childDataClasses.add(currentClass)
                currentClass.label = currentClass.label.replaceFirst("DATA GROUP:", "").trim()

                Node tdNode = null
                if (component.tbody.tr[1].td.size() == 3) {
                    tdNode = component.tbody.tr[1].td[2]
                } else {
                    tdNode = component.tbody.tr[1].td[0]
                }

                currentClass.description = tdNode.text()
                                               .replaceFirst("FUNCTION:", "")
                                               .trim() ?: null


                String mro = component.tbody.tr[1].td[0].text()
                mro = mro.replaceAll("Group", "")
                mro = mro.replaceAll("Status", "")
                DataSetParser.setMRO(currentClass, mro.trim())
                if(!component.tbody.tr[1].td[1]) {
                    log.error("unexpected row:")
                    log.error(component.tbody.tr[1].toString())
                } else {
                    String groupRepeats = component.tbody.tr[1].td[1].text()
                    groupRepeats = groupRepeats.replaceAll("Group", "")
                    groupRepeats = groupRepeats.replaceAll("Repeats", "")
                    DataSetParser.setGroupRepeats(currentClass, groupRepeats.trim())
                }

                if (ors > 0) {
                    DataSetParser.setChoice(currentClass)
                }

                if (component.tbody.tr.size() > 2) {
                    //log.debug("Got it!")
                    //log.debug(component.tbody.tr[1].td.text())

                    List<Node> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    tableRows = tableRows.drop(2)
                    parseCDSElementTable(tableRows, currentClass, dataModel, dataDictionary, classWebOrder)
                }
                DataSetParser.setOrder(currentClass, classWebOrder)
                classWebOrder++
                returnClasses.add(currentClass)


            } else if (component.name() == "table" && component.tbody.tr.size() >= 1 && currentClass) {
                if (ors > 0 && component.tbody.tr.count {tr -> tr.td[0].@bgcolor == "#DCDCDC"} > 1) {
                    log.error("Found an exception: " + dataModel.label)
                    log.error("" + component.tbody.tr[0].td[1])
                    log.error("" + component.tbody.tr[0].td[2])
                    component.tbody.tr.findAll {tr -> tr.td[0].@bgcolor == "#DCDCDC"}.each {tr ->
                        log.error("" + tr.td[1] + " " + tr.td[2])
                    }
                    DataClass andClass = new DataClass(label: "And")
                    //dataModel.childDataClasses.add(andClass)
                    DataSetParser.setAnd(andClass)
                    currentClass.dataClasses.add(andClass)
                    List<Node> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    parseCDSElementTable(tableRows, andClass, dataModel, dataDictionary, classWebOrder)
                    DataSetParser.setOrder(andClass, classWebOrder)
                    classWebOrder++

                } else {
                    List<Node> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    parseCDSElementTable(tableRows, currentClass, dataModel, dataDictionary, classWebOrder)
                    //DataSetParser.setOrder(currentClass, classWebOrder)
                    classWebOrder++
                }
            }

        }
        log.info('Parse CDS Section complete in {}', Duration.between(startTime, Instant.now()).toString())
        return returnClasses

    }

    static void parseCDSElement(DataModel dataModel, DataClass currentClass, Node tr, NhsDataDictionary dataDictionary, int position) {
        if (tr.td[2].a.size() == 6) {
            //
            DataSetParser.setChoice(currentClass)
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, currentClass)

            DataSetParser.setOrder(dataElement1, 1)


            DataClass andClass = new DataClass(label: "And")
            //dataModel.allDataClasses.add(andClass)
            DataSetParser.setOrder(andClass, 2)
            DataSetParser.setAnd(andClass)
            List<DataElement> deList = [dataElement1]
            for (int i = 1; i < 6; i++) {
                DataElement dataElement = DataSetParser.getElementFromText(tr.td[2].a[i], dataModel, dataDictionary, andClass)
                DataSetParser.setOrder(dataElement, i)
                deList.add(dataElement)
            }
            currentClass.dataClasses.add(andClass)
            DataSetParser.getAndSetMRO(tr.td[0], deList, currentClass)
            DataSetParser.getAndSetRepeats(tr.td[1], deList, currentClass)
            DataSetParser.getAndSetRules(tr.td[3], deList, currentClass)
        } else if (tr.td[2].a.size() == 4) {
            // Address
            DataClass choiceClass = new DataClass(label: "Choice")
            dataModel.childDataClasses.add(choiceClass)
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            if (tr.td[2].a[0].text().contains("NAME")) {
                DataSetParser.setNameChoice(choiceClass)
            } else {
                DataSetParser.setAddress(choiceClass)
            }
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, choiceClass)
            DataSetParser.getAndSetMRO(tr.td[0], [dataElement1], choiceClass)
            DataSetParser.getAndSetRepeats(tr.td[1], [dataElement1], choiceClass)
            DataSetParser.getAndSetRules(tr.td[3], [dataElement1], choiceClass)

            //DataElement dataElement2 = getElementFromText(tr.td[2].a[2], dataModel, dataDictionary, currentClass.label)
            DataSetParser.setOrder(dataElement1, 1)
            //setOrder(dataElement2, 2)
            //choiceClass.addToImportedDataElements(dataElement2)
            currentClass.dataClasses.add(choiceClass)


        } else if ((tr.td[2].em && tr.td[2].em.text().equalsIgnoreCase("Or")) ||
                   (tr.td[2].strong && tr.td[2].strong.text().equalsIgnoreCase("OR"))) {
            DataClass choiceClass = new DataClass(label: "Choice")
            //dataModel.allDataClasses.add(choiceClass)
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, choiceClass)
            DataElement dataElement2 = DataSetParser.getElementFromText(tr.td[2].a[1], dataModel, dataDictionary, choiceClass)

            DataSetParser.getAndSetMRO(tr.td[0], [dataElement1, dataElement2], choiceClass)
            DataSetParser.getAndSetRepeats(tr.td[1], [dataElement1, dataElement2], choiceClass)
            DataSetParser.getAndSetRules(tr.td[3], [dataElement1, dataElement2], choiceClass)

            DataSetParser.setOrder(dataElement1, 1)
            DataSetParser.setOrder(dataElement2, 2)
            currentClass.dataClasses.add(choiceClass)
        } else {
            if (tr.td.size() == 6) {
                // rare occurrence with unmerged first columns
                DataElement dataElement = DataSetParser.getElementFromText(tr.td[4].a[0], dataModel, dataDictionary, currentClass)
                DataSetParser.getAndSetMRO(tr.td[2], [dataElement])
                DataSetParser.getAndSetRepeats(tr.td[3], [dataElement])
                DataSetParser.getAndSetRules(tr.td[5], [dataElement])
                DataSetParser.setOrder(dataElement, position)

            } else {
                DataElement dataElement = null
                if (tr.td[2].p.size() == 1 && tr.td[2].p[0].a.size() == 1) {
                    // There's one example where the element is wrapped in a <p>
                    dataElement = DataSetParser.getElementFromText(tr.td[2].p[0].a[0], dataModel, dataDictionary, currentClass)
                } else if(tr.td[2].p.size() == 1 && tr.td[2].p[0].a.size() == 0) {
                    dataElement = DataSetParser.getElementFromText(tr.td[2].p[0], dataModel, dataDictionary, currentClass)
                } else if(tr.td[2].a[0]) {
                    dataElement = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, currentClass)
                } else if(tr.td[2].p.size() == 0 && tr.td[2].a.size() == 0 && tr.td[2].text()) {
                    dataElement = DataSetParser.getElementFromText(tr.td[2], dataModel, dataDictionary, currentClass)
                } else {
                    log.error("Cannot match pattern:")
                    log.error(tr.toString())
                }
                // assume 4
                if(dataElement) {
                    DataSetParser.getAndSetMRO(tr.td[0], [dataElement])
                    DataSetParser.getAndSetRepeats(tr.td[1], [dataElement])
                    DataSetParser.getAndSetRules(tr.td[3], [dataElement])
                    DataSetParser.setOrder(dataElement, position)
                }
            }
        }
    }
}
