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

import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.terminology.TerminologyRepository
import uk.nhs.datadictionary.NhsDataDictionary

@Bean
class MauroCatalogueItemPathResolver implements PathResolver<UUID> {
    UUID versionedFolderId

    @Inject TerminologyCacheableRepository terminologyCacheableRepository
    @Inject TerminologyRepository terminologyRepository
    @Inject TermCacheableRepository termCacheableRepository
    @Inject DataModelCacheableRepository dataModelCacheableRepository
    @Inject DataClassCacheableRepository dataClassCacheableRepository
    @Inject DataElementCacheableRepository dataElementCacheableRepository
    @Inject FolderCacheableRepository folderCacheableRepository
    @Inject PathRepository pathRepository

    MauroCatalogueItemPathResolver () {

    }
    /*
    MauroCatalogueItemPathResolver (Folder versionedFolder) {
        this.versionedFolderId = versionedFolder.id
    }

    MauroCatalogueItemPathResolver (UUID versionedFolderId) {
        this.versionedFolderId = versionedFolderId
    }
*/
    @Override
    UUID get(String path) {
        String[] pathParts = path.split("\\|")
        AdministeredItem catalogueItem = getByPath(pathParts)
        if (!catalogueItem) {
            return null
        }
        pathRepository.readParentItems(catalogueItem)
        catalogueItem.id
    }

    private AdministeredItem getByPath(String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyRepository.findAllByFolderId(versionedFolderId).find {
                (NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME == it.label)
            }
            String termLabel = path[1].replace("tm:", "")
            Term t = termCacheableRepository.findAllByTerminologyAndCode(terminology, termLabel)
            return t
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyCacheableRepository.findAllByFolderId(versionedFolderId).find {
               it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME
            }
            String termLabel = path[1].replace("tm:", "")
            Term t = termCacheableRepository.findAllByTerminologyAndCode(terminology, termLabel)
            return t
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyCacheableRepository.findAllByFolderId(versionedFolderId).find {
                it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME
            }
            String termLabel = path[1].replace("tm:", "")
            Term t = termCacheableRepository.findAllByTerminologyAndCode(terminology, termLabel)
            return t
        }
        if (path[0] == "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}".toString()) {
            DataModel dm = dataModelCacheableRepository.findAllByFolderId(versionedFolderId).find {
                it.label == NhsDataDictionary.CLASSES_MODEL_NAME
            }
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassCacheableRepository.readByDataModelAndLabelAndParentDataClassIsNull(dm, "Retired")
                DataClass dc2 = dataClassCacheableRepository.readByParentDataClassAndLabel(dc1, path[2].replace("dc:", ""))
                if(path.length > 3) {
                    DataElement de = dataElementCacheableRepository.readByDataClassAndLabel(dc2, path[3].replace("de:", ""))
                    return de
                } else {
                    return dc2
                }
            } else {
                DataClass dc1 = dataClassCacheableRepository.readByDataModelAndLabelAndParentDataClassIsNull(dm, path[1].replace("dc:", ""))
                if(path.length > 2) {
                    DataElement de = dataElementCacheableRepository.readByDataClassAndLabel(dc1, path[2].replace("de:", ""))
                    return de
                } else {
                    return dc1
                }
            }
        }
        if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
            DataModel dm = dataModelCacheableRepository.findAllByFolderId(versionedFolderId).find {
                it.label == NhsDataDictionary.ELEMENTS_MODEL_NAME
            }
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassCacheableRepository.readByDataModelAndLabelAndParentDataClassIsNull(dm, "Retired")
                DataElement de = dataElementCacheableRepository.readByDataClassAndLabel(dc1, path[2].replace("de:", ""))
                return de
            } else {
                DataClass dc = dataClassCacheableRepository.readByDataModelAndLabelAndParentDataClassIsNull(dm, path[1].replace("dc:", ""))
                DataElement de = dataElementCacheableRepository.readByDataClassAndLabel(dc, path[2].replace("de:", ""))
                return de
            }
        }
        if (path[0].startsWith("fo:${NhsDataDictionary.DATA_SETS_FOLDER_NAME}")) {
            UUID parentId = versionedFolderId
            int i = 0
            AdministeredItem returnItem = null
            while (path.length > i) {
                if (path[i].startsWith("fo:")) {
                    String label = path[i].replace("fo:", "")
                    returnItem = folderCacheableRepository.findAllByFolderId(parentId).find {
                        it.label == label
                    }
                    parentId = returnItem.id
                } else if (path[i].startsWith("dm:")) {
                    String label = path[i].replace("dm:", "")
                    returnItem = dataModelCacheableRepository.findAllByFolderId(parentId).find {
                        it.label == label
                    }
                    parentId = returnItem.id
                }
                i++
            }
            return returnItem
        }

        return null
    }
}
