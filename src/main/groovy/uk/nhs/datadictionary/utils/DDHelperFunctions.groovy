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
package uk.nhs.datadictionary.utils

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.folder.FolderService
import org.maurodata.exception.MauroApplicationException

@Slf4j
class DDHelperFunctions {

    static String[] getPath(String input, String delimiter, String suffix) {
        String suffixRemoved = input.replaceAll(suffix, "")

        String[] baseURI = suffixRemoved.split(delimiter + "/")
        //log.debug(baseURI)
        String[] path = baseURI[1].split("/")
        return path
    }

    static String tidyLabel(String label) {
        label.replaceAll("_", " ")
                .replaceAll("%20", " ")
                .replaceAll("%2515", "")
                .trim()
    }

    static String parseHtml(String str) {
        String ret = str
        if (!ret) {
            return null
        }
        ret = ret.replaceAll(/<.xml.*?>/, "")
        ret = ret.replaceAll("<head>", "")
        ret = ret.replaceAll("</head>", "")
        ret = ret.replaceAll("<title>[^<]*</title>", "")
        ret = ret.replaceAll("<body>", "")
        ret = ret.replaceAll("</body>", "")
        ret = ret.replaceAll("\n", "")
        ret = ret.replaceAll("<definition>", "")
        ret = ret.replaceAll("</definition>", "")
        ret = ret.replaceAll("<definition/>", "")
        return ret.trim()

    }

    static String parseHtml(Node res) {
        if (!res || !res.toString().trim()) {
            return null
        }
        try {
            return parseHtml(XmlUtil.serialize(res))
        } catch (Exception e) {
            log.error(res.toString())
        }
        null
    }

/*    static void addMetadata(MetadataAware ci, String key, String value) {
        if (value) {

            Metadata metadata = new Metadata(
                namespace: metadataNamespace,
                key: key,
                value: value
            )
            ci.addToMetadata(metadata)
        }
    }

    static String getMetadataValue(MetadataAware ci, String key) {
        Metadata metadata = ci.getMetadata().find {md ->
            md.namespace.startsWith(metadataNamespace) && md.key == key
        }
        if (metadata) {
            return metadata.value
        }
        return null
    }
*/
    /*
        static void addDataElementAtPath(DataClass topLevelClass, String path, DataElement dataElement) {
            DataClass parentClass = getClassByPath(topLevelClass, path)
            parentClass.addToDataElements(dataElement)
        }

        static void addDataClassAtPath(DataClass topLevelClass, String path, DataClass dataClass) {
            DataClass parentClass = getClassByPath(topLevelClass, path)
            parentClass.addToDataClasses(dataClass)
        }
    */

    static void addDataElementAtPath(DataClass topLevelClass, List<String> path, DataElement dataElement) {
        DataClass parentClass = getClassByPath(topLevelClass, path)
        parentClass.addToDataElements(dataElement)
    }

    static void addDataClassAtPath(DataClass topLevelClass, List<String> path, DataClass dataClass) {
        DataClass parentClass = getClassByPath(topLevelClass, path)
        parentClass.addToDataClasses(dataClass)
    }

    /*
        static DataClass getClassByPath(DataClass topLevelClass, String path) {
            DataClass parentClass = topLevelClass
            String[] pathComponents = path.split("/")
            pathComponents.each { componentName ->
                if(componentName != pathComponents.last()) {
                    DataClass newParentClass = parentClass.getChildDataClasses().find {it.label == componentName}
                    if (!newParentClass) {
                        newParentClass = new DataClass(label: componentName)
                        parentClass.addToDataClasses(newParentClass)
                    }
                    parentClass = newParentClass
                }
            }
            return parentClass

        }
    */

    static DataClass getClassByPath(DataClass topLevelClass, List<String> path) {
        DataClass parentClass = topLevelClass
        path.each {componentName ->
            DataClass newParentClass = parentClass.getDataClasses().find {it.label == componentName}
            if (!newParentClass) {
                newParentClass = new DataClass(label: componentName)
                parentClass.addToDataClasses(newParentClass)
            }
            parentClass = newParentClass
        }
        return parentClass
    }

    static List<List<GPathResult>> splitByDuckBlueClasses(GPathResult dataSetDefinition) {

        List<List<GPathResult>> topLevelClasses = []
        List<GPathResult> list = []
        dataSetDefinition.table.each {table ->
            if (table instanceof Node && table.name() == "table" && tableIsClassHeader(table)) {
                if (list.size() > 0) {
                    topLevelClasses.add(list)
                }
                list = []
                list.add(table)
            } else {
                list.add(table)
            }
        }
        topLevelClasses.add(list)
        topLevelClasses.each {thisList ->
            //log.debug(thisList.size())
        }
        topLevelClasses
    }

    static boolean tableIsClassHeader(Node table) {
        if (table.tbody.tr.size() == 1 &&
                table.tbody.tr[0].th.size() == 1 &&
                table.tbody.tr[0].th[0].@class == "duckblue") {
            return true
        }
        // For CDS
        if (/*table.tbody.tr.size() == 2 && */ table.tbody.tr[0] &&
                table.tbody.tr[0].td.size() == 2 &&
                table.tbody.tr[0].td[1].@class == "duckblue") {
            return true
        }
        return false
    }

    /*
    static void splitGreyClasses(GPathResult table, DataModel dataSetDataModel, DataClass parentClass, boolean useParentClass,
                                 DataDictionary dataDictionary, Integer index) {

        DataClass currentClass = parentClass
        if (parentClass == null && useParentClass) {
            currentClass = new DataClass(label: dataSetDataModel.label)
            addMetadata(currentClass, "item_order", "" + index)
        }
        int classIndex = index
        int itemIndex = 0
        table.tbody.tr.each {tr ->
            String classDesc = dataSetRowIsClassHeader(tr, dataSetDataModel.label)
            if (!useParentClass && classDesc != null) {
                String className, description = ""

                String[] classNameComps = classDesc.split(":")
                if (classNameComps.size() > 1) {
                    className = classNameComps[0].trim()
                    description = classNameComps[1].trim()
                } else {
                    classNameComps = classDesc.split("-")
                    if (classNameComps.size() > 1) {
                        className = classNameComps[0].trim()
                        description = classNameComps[1].trim()
                    } else {
                        className = classDesc
                    }
                }
                currentClass = new DataClass(label: className, description: description)
                addMetadata(currentClass, "item_order", "" + classIndex)
                classIndex++
                //log.debug("    ${className}")
                if (parentClass) {
                    parentClass.addToDataClasses(currentClass)
                } else {
                    dataSetDataModel.addToDataClasses(currentClass)
                }
            } else if (!dataSetRowIsIgnorable(tr)) {
                itemIndex += addDataSetElements(dataSetDataModel, currentClass, tr, dataDictionary, itemIndex)
            } else {
                if (tr.th.size() == 1 && tr.th.@class == "skyblue") {
                    currentClass.description = tr.th.text()
                }
                if (tr.th.size() == 2 && tr.th[0].@class == "skyblue") {
                }
                if ((tr.td.size() == 1 && tr.td.@class == "skyblue")) {
                    currentClass.description = tr.td.text()
                }
                if ((tr.td.size() == 2 && tr.td[0].@class == "skyblue")) {
                }
            }
            currentClass.description = currentClass.description ?: null
        }
    }

*/

    static boolean dataSetRowIsIgnorable(GPathResult row) {
        if (row.th.size() == 1 && row.th.@class == "skyblue") {
            return true
        }
        if (row.th.size() == 2 && row.th[0].@class == "skyblue") {
            return true
        }
        if ((row.td.size() == 1 && row.td.@class == "skyblue")) {
            return true
        }
        if ((row.td.size() == 2 && row.td[0].@class == "skyblue")) {
            return true
        }
        return false
    }

    static String dataSetRowIsClassHeader(GPathResult row, String dataModelLabel) {
        if (row.td.size() == 1 && row.td.@class == "skyblue") {
            return row.td.text()
        }
        if (row.th.size() == 1 && row.th.@class == "skyblue") {
            return row.th.text()
        }
        if (dataModelLabel == "National Joint Registry Data Set - Common Details" && row.td.size() == 2 && row.td[1].@class == "skyblue") {
            return row.td[1].text()
        }
        return null

    }

    static DataClass dataClassFromTable(GPathResult table, DataModel dataSetDataModel) {
        DataClass currentClass = new DataClass(label: table.tbody.tr.th.text())
        dataSetDataModel.addToDataClasses(currentClass)
        return currentClass
    }
/*
    static int addDataSetElements(DataModel dataSetDataModel, DataClass currentClass, GPathResult tr, DataDictionary dataDictionary,
                                  int index) {

        GPathResult correctCell, multiplicityCell = null
        String multiplicityText = ""
        if (tr.td.size() == 2) {
            correctCell = tr.td[1]
            multiplicityCell = tr.td[0]
            multiplicityText = multiplicityCell.text().trim()
        } else if (tr.td.size() == 1) {
            correctCell = tr.td
        } else {
            log.error("Cannot interpret row: ${dataSetDataModel.label}, ${currentClass.label}, ${tr}")
            return
        }
        int idx = 0
        correctCell.a.each {a ->
            String elementLabel = ((String) a.@href).replaceAll(" ", "%20")
            DDElement de = dataDictionary.elements.values().find {it.ddUrl == elementLabel}
            if (!de) {
                log.error("Cannot find element: ${a.text()} - ${elementLabel}")
                log.error("In class: ${currentClass.label}, model: ${dataSetDataModel.label}")
            } else {
                DataElement newDataElement = de.toCatalogueItem(dataDictionary)
                addMetadata(newDataElement, "item_order", "" + (index + idx))
                if (multiplicityText) {
                    addMetadata(newDataElement, "item_mro", multiplicityText)
                    if (multiplicityText == "R" || multiplicityText == "M") {
                        newDataElement.minMultiplicity = 1
                        newDataElement.maxMultiplicity = 1
                    } else if (multiplicityText == "O" || multiplicityText == "P") {
                        newDataElement.minMultiplicity = 0
                        newDataElement.maxMultiplicity = 1
                    } else {
                        log.error("Unknown multiplicity: ${multiplicityText}")
                        log.error("In class: ${currentClass.label}, model: ${dataSetDataModel.label}")
                    }
                }


                if (currentClass.dataElements.find {it.label == newDataElement.label}) {
                    log.error("data element already exists in class: ")
                    log.error("      ${currentClass.label} - ${newDataElement.label}")
                } else {
                    currentClass.addToDataElements(newDataElement)
                }
            }
            idx++
        }
        return idx
    }
*/

    /*    static void writeDataModelAtPath(BindingMauroDataMapperClient mdmClient, UUID parentFolderId, List<String> path, DataModel dataModel) {
            UUID folderId = parentFolderId
            path.each { componentName ->
                if(componentName != path.last()) {
                    folderId = mdmClient.findOrCreateFolderByName(componentName, folderId)
                }
            }
            log.info("Uploading: ${dataModel.label}")
            mdmClient.importDataModel(dataModel, folderId, dataModel.label, FINALISED, false)
        }
    */

    static String makeValidDitaName(String oldFilename) {

        String ret = oldFilename.toLowerCase()
        [" ", "'", "/", "(", ")", ",", "+", ":", "%20", "%2515", "%2506", "%2507", "%2508", "%e2", "%80", "%93", "%15", "&apos;"].each {
            ret = ret.replace(it, "_")
        }
        return ret

    }

    static String findFromMetadata(def catalogueXml, String key) {
        return catalogueXml.metadata.find {it.namespace == metadataNamespace && it.key == key}?.value
    }

    static String findFromMetadataXml(def catalogueXml, String key) {
        return catalogueXml.metadata.metadata.find {it.namespace == metadataNamespace && it.key == key}?.value

    }

    static String translateMRO(String mro_in, String datasetName) {
        String mro = mro_in?.trim()
        if (!mro || mro == "") {
            return ""
        } else if (mro.trim().startsWith("R")) {
            return "Required"
        } else if (mro.trim().startsWith("O")) {
            return "Optional"
        } else if (mro.trim().startsWith("M")) {
            return "Mandatory"
        } else if (mro.trim().startsWith("P")) {
            return "Pilot*"
        } else {
            log.error("Unknown multiplicity mro: " + mro + " : " + datasetName)
            return ""
        }
    }

    static List<String> getPrefixes(String path) {
        int slashIndex = path.indexOf('/')
        if (slashIndex < 0) {
            return []
        } else {
            String head = path.substring(0, slashIndex)
            String tail = path.substring(slashIndex)
            List ret = [head]
            ret.addAll(getPrefixes(tail).collect {it -> head + "/" + it})
            return ret
        }
    }

    static String unquoteString(String input) {
        return input.
                replaceAll("&quot;", "\"").
                replaceAll("&gt;", ">").
                replaceAll("&lt;", "<").
                replaceAll("&apos;", "'")
    }

    static Folder getSubfolderFromName(FolderService folderService, Folder parent, String name, String currentUserEmailAddress) {
        String folderName = name.substring(0, 1).toUpperCase()
        Folder subFolder = parent.childFolders.find {it.label == folderName}
        if (!subFolder) {
            subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
            parent.addToChildFolders(subFolder)
            if (!folderService.validate(subFolder)) {
                throw new MauroApplicationException('NHSDD', 'Invalid model', subFolder.errors)
            }
            folderService.save(subFolder)
            //existingFolders[folderName] = subFolder
        }
        subFolder
    }

    static DataClass getChildClassFromName(DataModel parent, String name, String currentUserEmailAddress) {
        String className = name.substring(0, 1).toUpperCase()
        DataClass childClass = parent.childDataClasses.find {it.label == className}
        if (!childClass) {
            childClass = new DataClass(label: className, createdBy: currentUserEmailAddress)
            parent.addToDataClasses(childClass)
        }
        childClass
    }



}
