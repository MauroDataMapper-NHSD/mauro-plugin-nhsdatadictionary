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
import uk.nhs.datadictionary.NhsDataDictionary

@Singleton
class DataSetsHaveAnOverview implements IntegrityCheck {

    String name = "Data Sets have an overview"

    String description = "Check that all live data sets have an overview field"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        dataDictionary.dataSets.values()
            .findAll {ddDataSet ->
                // log.debug(ddAttribute.classLinks.size())
                !ddDataSet.isRetired() &&
                (ddDataSet.description == null || ddDataSet.description == "")
            }
            .collect { component -> new IntegrityCheckError(component) }
    }

}
