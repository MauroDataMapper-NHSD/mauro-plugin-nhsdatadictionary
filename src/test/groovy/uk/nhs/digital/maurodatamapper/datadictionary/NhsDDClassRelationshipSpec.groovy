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
package uk.nhs.digital.maurodatamapper.datadictionary

import spock.lang.Specification
import uk.nhs.datadictionary.NhsDDClassRelationship

class NhsDDClassRelationshipSpec extends Specification {
    void "should build the correct description"(
        String role,
        boolean hasMultiple,
        int minMultiplicity,
        int maxMultiplicity,
        boolean isChoice,
        String expected) {
        given: "a class relationship"
        def classRelationship = new NhsDDClassRelationship(
            role: role,
            hasMultiple: hasMultiple,
            minMultiplicity: minMultiplicity,
            maxMultiplicity: maxMultiplicity,
            isChoice: isChoice)

        when: "building the description"
        String actual = classRelationship.buildDescription()

        then: "the expected description is returned"
        verifyAll {
            actual == expected
        }

        where:
        role | hasMultiple | minMultiplicity | maxMultiplicity | isChoice | expected
        "supplied by" | false | 1 | 1 | false | "must be supplied by one and only one"
        "for" | false | 1 | 1 | false | "must be for one and only one"
        "located at" | false | 1 | 1 | true | "must be located at one and only one"
        "located at" | true | 1 | 1 | true | "or must be located at one and only one"
        "associated with" | false | 1 | -1 | false | "must be associated with one or more"
        "the subject of" | false | 0 | -1 | false | "may be the subject of one or more"
        "the source of" | false | 0 | 1 | false | "may be the source of one and only one"
    }
}
