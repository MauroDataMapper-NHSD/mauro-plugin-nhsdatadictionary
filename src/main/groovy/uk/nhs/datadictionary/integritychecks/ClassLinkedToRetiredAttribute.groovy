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
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDataDictionary

@Singleton
class ClassLinkedToRetiredAttribute implements IntegrityCheck {

    String name = "Classes Linked to Retired Attributes"

    String description = "Check that all live classes do not contain attributes that are retired"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {
        List<IntegrityCheckError> foundErrors = []

        dataDictionary.classes.values()
            .findAll { ddClass -> !ddClass.isRetired() }
            .forEach { ddClass ->
                List<NhsDDAttribute> retiredAttributes = ddClass.getAttributes().findAll { it.isRetired() }
                if (!retiredAttributes.empty) {
                    List<String> attributeNames = retiredAttributes.collect { it.name }
                    foundErrors.add(new IntegrityCheckError(ddClass, attributeNames))
                }
            }

        foundErrors
    }

}
