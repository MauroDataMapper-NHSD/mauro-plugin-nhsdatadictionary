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
import uk.nhs.datadictionary.NhsDataDictionaryComponent

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'label')
class StereotypedCatalogueItem {
    AdministeredItem catalogueItem
    String label
    String stereotype
    Boolean isRetired
    String key
    String description

    StereotypedCatalogueItem(AdministeredItem catalogueItem, String stereotype) {
        this.catalogueItem = catalogueItem
        this.stereotype = stereotype
        this.isRetired = catalogueItem.metadata.any {
            it.key == "isRetired" &&
            it.value == "true"
        }
        this.key = catalogueItem.metadata.any { it.key == "isKey" && it.value == "true" } ? "Key" : ""
        this.label = catalogueItem.label
        this.description = ""
    }

    StereotypedCatalogueItem(NhsDataDictionaryComponent component, String description = null) {
        this.catalogueItem = component.catalogueItem
        this.stereotype = component.stereotypeForPreview
        this.isRetired = component.isRetired()
        this.key = component.otherProperties.any { it.key == "isKey" && it.value == "true" } ? "Key" : ""
        this.label = component.getNameWithRetired()
        this.description = description
    }

    String getId() {
        catalogueItem.id.toString()
    }

}
