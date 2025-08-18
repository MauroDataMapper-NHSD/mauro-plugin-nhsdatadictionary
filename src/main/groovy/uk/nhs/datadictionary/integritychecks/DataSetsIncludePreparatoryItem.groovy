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

import org.maurodata.domain.datamodel.DataModel
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent

class DataSetsIncludePreparatoryItem implements IntegrityCheck {

    String name = "Data Sets that include preparatory items"

    String description = "Check that a data set doesn't include any preparatory data elements in its definition"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        List<NhsDataDictionaryComponent> prepItems = dataDictionary.elements.values().findAll {it.isPreparatory()}

        errors = dataDictionary.dataSets.values()
            .findAll {component ->
                ((DataModel)component.catalogueItem).allDataElements.find {dataElement ->
                    prepItems.find {it.name == dataElement.label}
                }
            }
            .collect { component -> new IntegrityCheckError(component) }

        return errors
    }

}
