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

import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary

@Singleton
class DataSetsIncludeRetiredItem implements IntegrityCheck {

    String name = "Data Sets that include retired items"

    String description = "Check that a data set doesn't include any retired data elements in its definition"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        dataDictionary.dataSets.values()
            .findAll {component ->
                ((DataModel)component.catalogueItem).dataElements.find {dataElement ->
                    NhsDDElement foundElement = dataDictionary.elementsByCatalogueId[dataElement.id]
                    if(foundElement) {
                        return foundElement.isRetired()
                    } else {
                        // This must be one of those 'preview' elements, and so we'll assume it's not retired
                        return false
                    }
                }
            }
            .collect { component -> new IntegrityCheckError(component) }
    }

}
