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
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.folder.FolderRepository
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class DataSetFolderService extends DataDictionaryComponentService<Folder, NhsDDDataSetFolder> {

    @Inject
    DataSetService dataSetService

    @Inject ModelCacheableRepository.FolderCacheableRepository folderCacheableRepository
    @Inject ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    String getStereotype() {
        "dataSet"
    }


    @Override
    NhsDDDataSetFolder show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        Folder folderFolder
        if(id) {
            folderFolder = folderCacheableRepository.findById(id)
        } else {
            folderFolder = folderCacheableRepository.findAllByFolderId(versionedFolderId).find {
                it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME
            }
        }
        NhsDDDataSetFolder dataSetFolder = initialiseComponent(new NhsDDDataSetFolder(), folderFolder, versionedFolderId, nhsDataDictionaryService)
        if(id) {
            List<String> folderPath = [folderFolder.label]
            Folder parentFolder = folderCacheableRepository.findById(folderFolder.getParentFolder().id)
            while(parentFolder && parentFolder.label != "Data Sets") {
                folderPath.add(0, parentFolder.label)
                parentFolder = folderCacheableRepository.findById(parentFolder.getParentFolder().id)
            }
            dataSetFolder.folderPath = folderPath
        }
        folderCacheableRepository.findAllByFolderId(folderFolder.id).sort {it.label}.each {
            NhsDDDataSetFolder childFolder = initialiseComponent(new NhsDDDataSetFolder(), it, versionedFolderId, nhsDataDictionaryService)
            dataSetFolder.childFolders.add(childFolder)
        }

        dataModelCacheableRepository.findAllByFolderId(folderFolder.id).sort {it.label}.each {
            NhsDDDataSet childDataSet = dataSetService.initialiseComponent(new NhsDDDataSet(), it, versionedFolderId, nhsDataDictionaryService)
            if (!childDataSet.isRetired()) {
                dataSetFolder.dataSets.add(childDataSet)
            }
        }
        dataSetFolder.dataSets = dataSetFolder.dataSets.sort {it.name}

        return dataSetFolder
    }

    @Override
    Set<Folder> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
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

    Map<List<String>, Set<Folder>> getAllFolders(List<String> currentPath, Folder dataSetsFolder, Boolean includeRetired = false) {
        Map<List<String>, Set<Folder>> returnFolders = [:]
        dataSetsFolder.childFolders.each {subFolder ->
            if(returnFolders[currentPath]) {
                returnFolders[currentPath].add(subFolder)
            } else {
                returnFolders[currentPath] = ([subFolder] as Set)
            }
            List<String> newPath = []
            newPath.addAll(currentPath)
            newPath.add(subFolder.label)
            returnFolders.putAll(getAllFolders(newPath, subFolder, includeRetired))
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
                if(dataSetFolder.description) {
                    newFolder.description = dataSetFolder.description
                }
                addMetadataFromComponent(newFolder, dataSetFolder)
            }
        }
    }



    NhsDDDataSetFolder getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        (NhsDDDataSetFolder) nhsDataDictionary.dataSetFolders.values().flatten().find { NhsDDDataSetFolder folder -> folder.catalogueItem.id == catalogueItemId }
    }

}