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
package uk.nhs.datadictionary.integritychecks

import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

class IntegrityCheckError {
    final StereotypedCatalogueItem component
    final List<String> details

    IntegrityCheckError(StereotypedCatalogueItem component) {
        this(component, [])
    }

    IntegrityCheckError(NhsDataDictionaryComponent component) {
        this(new StereotypedCatalogueItem(component), [])
    }

    IntegrityCheckError(StereotypedCatalogueItem component, List<String> details) {
        this.component = component
        this.details = details
    }

    IntegrityCheckError(NhsDataDictionaryComponent component, List<String> details) {
        this(new StereotypedCatalogueItem(component), details)
    }

}
