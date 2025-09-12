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
package uk.nhs.datadictionary.services

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class DataSetFolderService extends DataDictionaryComponentService<Folder, NhsDDDataSetFolder> {

    @Inject
    DataSetService dataSetService

    @Override
    NhsDDDataSetFolder show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        Folder folderFolder
        if(id && id != "root") {
            folderFolder = folderService.get(id)
        } else {
            Folder vf = versionedFolderService.get(versionedFolderId)
            folderFolder = vf.childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}
        }
        NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder().fromMauroItem(dataDictionary, mauroPersistenceService, folderFolder)
        dataSetFolder.definition = convertLinksInDescription(versionedFolderId, dataSetFolder.getDescription())
        if(id && id != 'root') {
            List<String> folderPath = [folderFolder.label]
            Folder parentFolder = (Folder) folderFolder.getParent()
            while(parentFolder.label != "Data Sets") {
                folderPath.add(0, parentFolder.label)
                parentFolder = (Folder) parentFolder.getParent()
            }
            dataSetFolder.folderPath = folderPath
        }
        folderFolder.childFolders.each { it ->
            NhsDDDataSetFolder childFolder = new NhsDDDataSetFolder().fromMauroItem(dataDictionary, mauroPersistenceService, it)
            dataSetFolder.childFolders[it.label] = childFolder
        }
        dataModelService.findAllByFolderId(folderFolder.id).each {
            NhsDDDataSet childDataSet = new NhsDDDataSet().fromMauroItem(dataDictionary, mauroPersistenceService, it)
            if (!childDataSet.isRetired()) {
                dataSetFolder.dataSets[it.label] = childDataSet
            }
        }

        return dataSetFolder
    }

    @Override
    Set<Folder> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        Folder dataSetsFolder = nhsDataDictionaryService.getDataSetsFolder(versionedFolderId)

        Map<List<String>, Set<Folder>> allFolders = getAllFolders([], dataSetsFolder, includeRetired)

        Set<Folder> returnFolders = [] as Set


        allFolders.values().each {folders ->
            folders.each {folder ->
                if(folder.label != "Retired" && (
                    includeRetired || !catalogueItemIsRetired(folder))) {
                    returnFolders.add(folder)
                }
            }

        }
        return returnFolders
    }

    Map<List<String>, Set<Folder>> getAllFolders(List<String> currentPath, Folder dataSetsFolder, boolean includeRetired = false) {
        Map<List<String>, Set<Folder>> returnFolders = [:]
        folderService.findAllByParentId(dataSetsFolder.id).each {
            if(returnFolders[currentPath]) {
                returnFolders[currentPath].add(it)
            } else {
                returnFolders[currentPath] = ([it] as Set)
            }
        }
        dataSetsFolder.childFolders.each {childFolder ->
            List<String> newPath = []
            newPath.addAll(currentPath)
            newPath.add(childFolder.label)
            returnFolders.putAll(getAllFolders(newPath, childFolder, includeRetired))
        }
        return returnFolders
    }




    void persistDataSetFolders(NhsDataDictionary dataDictionary, Folder dictionaryFolder) {

        Folder dataSetsFolder = new Folder(label: NhsDataDictionary.DATA_SETS_FOLDER_NAME)
        dictionaryFolder.childFolders.add(dataSetsFolder)


        dataDictionary.dataSetFolders.each {path, folders ->
            //System.err.println("Persisting: ${dataSetFolder.name}")

            Folder newFolder = getFolderAtPath(dataSetsFolder, path)
            folders.each {dataSetFolder ->
                if(dataSetFolder.definition) {
                    newFolder.description = dataSetFolder.definition
                }
                addMetadataFromComponent(newFolder, dataSetFolder)
            }
        }
    }



    NhsDDDataSetFolder getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        (NhsDDDataSetFolder) nhsDataDictionary.dataSetFolders.values().flatten().find { NhsDDDataSetFolder folder -> folder.catalogueItem.id == catalogueItemId }
    }

}