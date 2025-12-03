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
class AttributesLinkedToAClass implements IntegrityCheck {

    String name = "Attributes linked to a class"

    String description = "Check that all live attributes are linked to a class"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        Map<String, NhsDDAttribute> allUnusedAttributesMap = new HashMap<String, NhsDDAttribute>(dataDictionary.attributes)

        dataDictionary.classes.values().each {dataClass ->
            dataClass.allAttributes().each {attribute ->
                allUnusedAttributesMap.remove(attribute.name)
            }
        }

        allUnusedAttributesMap.removeAll {name, attribute ->
            attribute.isRetired()
        }

        allUnusedAttributesMap
            .values()
            .collect { component -> new IntegrityCheckError(component) }
    }

}
