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

import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent

class ReusedItemNames implements IntegrityCheck {

    String name = "Re-used item names"

    String description = "Check that item names of retired classes, attributes and elements have not been re-used"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        Set<String> duplicateAttributes = findDuplicates(dataDictionary.attributes.values().collect {it.name})
        Set<String> duplicateElements = findDuplicates(dataDictionary.elements.values().collect {it.name})
        Set<String> duplicateClasses = findDuplicates(dataDictionary.classes.values().collect {it.name})

        Set<NhsDataDictionaryComponent> foundDuplicates = []
        if (duplicateAttributes.size() > 0) {
            duplicateAttributes.each {attName ->
                foundDuplicates.addAll(dataDictionary.attributes.values().findAll {it.name == attName})
            }
        }
        if (duplicateElements.size() > 0) {
            duplicateElements.each {elemName ->
                foundDuplicates.addAll(dataDictionary.elements.values().findAll {it.name == elemName})
            }
        }
        if (duplicateClasses.size() > 0) {
            duplicateClasses.each {className ->
                foundDuplicates.addAll(dataDictionary.classes.values().findAll {it.name == className})
            }
        }

        errors = foundDuplicates
            .toList()
            .collect { component -> new IntegrityCheckError(component) }

        return errors
    }

    private static <T> Set<T> findDuplicates(Collection<T> collection) {

        Set<T> duplicates = new HashSet<>(1000);
        Set<T> uniques = new HashSet<>();

        for (T t : collection) {
            if (!uniques.add(t)) {
                duplicates.add(t)
            }
        }

        return duplicates;
    }

}
