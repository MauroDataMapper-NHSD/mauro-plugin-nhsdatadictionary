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

import groovy.transform.Sortable
import org.maurodata.domain.model.AdministeredItem
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDataDictionaryComponent

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'name')
class StereotypedCatalogueItem {

    String name
    String stereotype
    Boolean retired
    String key
    String description
    UUID catalogueItemId

    List<StereotypedCatalogueItem> childFolders
    List<StereotypedCatalogueItem> dataSets

    // Default constructor for Jackson
    StereotypedCatalogueItem() { }

    StereotypedCatalogueItem(AdministeredItem catalogueItem, String stereotype) {
        this.stereotype = stereotype
        this.retired = catalogueItem.metadata.any {
            it.key == "isRetired" &&
            it.value == "true"
        }
        this.key = catalogueItem.metadata.any { it.key == "isKey" && it.value == "true" } ? "Key" : ""
        this.description = catalogueItem.description
        this.name = catalogueItem.label
        this.catalogueItemId = catalogueItem.id
    }

    StereotypedCatalogueItem(NhsDataDictionaryComponent component, String description = null) {
        this.stereotype = component.stereotypeForPreview
        this.retired = component.isRetired()
        this.key = component.otherProperties.any { it.key == "isKey" && it.value == "true" } ? "Key" : ""
        this.name = component.getNameWithRetired()
        this.description = description
        this.catalogueItemId = component.catalogueItem.id
        if(component instanceof NhsDDDataSetFolder) {
            this.childFolders = component.childFolders.collect {
                new StereotypedCatalogueItem(it)
            }
            this.dataSets = component.dataSets.collect {
                new StereotypedCatalogueItem(it)
            }
        }
    }

}
