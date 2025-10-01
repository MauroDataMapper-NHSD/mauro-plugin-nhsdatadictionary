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
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
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
                return dc2
            } else {
                DataClass dc1 = dataClassCacheableRepository.readByDataModelAndLabelAndParentDataClassIsNull(dm, path[1].replace("dc:", ""))
                return dc1
            }
        } else if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
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

        if (path.length == 1 && path[0].startsWith("dm:")) {
            DataModel dm = dataModelCacheableRepository.findAllByFolderId(versionedFolderId).find {
                it.label == path[0].replace("dm:", "")
            }
            return dm
        }

        return null
    }
}
