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

import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDataDictionary


class AllClassesHaveRelationships implements IntegrityCheck {

    String name = "Class Relationships Defined"

    String description = "Check that all live classes have relationships to other classes defined"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.classes.values()
            .findAll{ddClass -> !ddClass.isRetired() && !classHasRelationships(ddClass) }
            .collect { component -> new IntegrityCheckError(component) }

        return errors
    }

    boolean classHasRelationships(NhsDDClass ddClass) {
        if( ddClass.classRelationships.size() > 0) {
            return true
        } else {
            boolean found = false
            ddClass.extendsClasses.each {extendedClass ->
                if(classHasRelationships(extendedClass)) {
                    found = true
                }
            }
            return found
        }
    }


}
