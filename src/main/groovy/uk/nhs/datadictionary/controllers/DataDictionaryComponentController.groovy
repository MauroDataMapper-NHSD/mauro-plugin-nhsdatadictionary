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
package uk.nhs.datadictionary.controllers

import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import uk.nhs.datadictionary.services.AttributeService
import uk.nhs.datadictionary.services.BusinessDefinitionService
import uk.nhs.datadictionary.services.ClassService
import uk.nhs.datadictionary.services.DataDictionaryComponentService
import uk.nhs.datadictionary.services.DataSetConstraintService
import uk.nhs.datadictionary.services.DataSetFolderService
import uk.nhs.datadictionary.services.DataSetService
import uk.nhs.datadictionary.services.ElementService
import uk.nhs.datadictionary.services.NhsDataDictionaryService
import uk.nhs.datadictionary.services.SupportingInformationService
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

abstract class DataDictionaryComponentController<T extends AdministeredItem> {
	static responseFormats = ['json', 'xml']

    @Inject DataSetService dataSetService
    @Inject ElementService elementService
    @Inject ClassService classService
    @Inject AttributeService attributeService
    @Inject BusinessDefinitionService businessDefinitionService
    @Inject SupportingInformationService supportingInformationService
    @Inject DataSetConstraintService dataSetConstraintService
    @Inject DataSetFolderService dataSetFolderService

    @Inject NhsDataDictionaryService nhsDataDictionaryService

    abstract DataDictionaryComponentService getService()

    abstract String getParameterIdKey()


    def index(UUID dictionaryId, Boolean includeDeleted = false) {
        return getService().index(UUID.fromString(params.versionedFolderId), params.boolean('includeRetired')?:false)
    }

    def show() {
        respond getService().show(UUID.fromString(params.versionedFolderId), params.id)
    }

    def whereUsed() {
        List<StereotypedCatalogueItem> whereUsed =  getService().getWhereUsed(UUID.fromString(params.versionedFolderId), params[getParameterIdKey()])
        respond whereUsed
    }

}
