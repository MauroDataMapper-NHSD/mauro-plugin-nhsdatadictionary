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
package uk.nhs.datadictionary

import com.fasterxml.jackson.annotation.JsonProperty
import org.maurodata.domain.datamodel.DataElement
import uk.nhs.datadictionary.publish.changePaper.ChangeAware

class NhsDDClassRelationship implements ChangeAware {

    @JsonProperty('relationship')
    String relationshipDescription
    NhsDDClass targetClass
    String role
    boolean hasMultiple

    @JsonProperty('key')
    boolean key

    @JsonProperty('choice')
    boolean isChoice

    int minMultiplicity
    int maxMultiplicity

    String getName() {
        targetClass.name
    }

    String getStereotype() {
        targetClass.stereotypeForPreview
    }

    /**
     * This describes the full description of the relationship, including relationship label, target class and key. This should
     * be used in comparisons.
     */
    @Override
    String getDiscriminator() {
        "${key ? "Key: " : ""}$relationshipDescription $targetClass.name"
    }

    NhsDDClassRelationship() {
    }

    NhsDDClassRelationship(DataElement relationshipElement, NhsDDClass targetClass) {
        this.targetClass = targetClass
        // Assume that the relationshipElement label may have a multiple suffix e.g. "categorised by (1)" or "categorised by (10)"
        // This is because the label has to be unique but the relationship label may be used to relate a class to
        // different classes
        this.role = relationshipElement.label.replaceAll("\\(\\d+\\)", "")
        // TODO: can't think of a better way right now to work out if the description needs to contain "or"
        this.hasMultiple = !relationshipElement.label.findAll("\\(\\d+\\)").empty
        this.minMultiplicity = relationshipElement.minMultiplicity ?: 0
        this.maxMultiplicity = relationshipElement.maxMultiplicity ?: 0
        this.key = relationshipElement.metadata.find {it.key == NhsDDClassLink.IS_KEY_METADATA_KEY }?.value == "true" ?: false
        this.isChoice = relationshipElement.metadata.find { it.key == NhsDDClassLink.IS_CHOICE_METADATA_KEY }?.value == "true" ?: false
        this.relationshipDescription = this.buildDescription()
    }

    String buildDescription() {
        String prefix = hasMultiple && isChoice ? "or " : ""

        if (minMultiplicity == 0 && maxMultiplicity == 1) {
            return "${prefix}may be ${role} one and only one"
        }

        if (minMultiplicity == 0 && maxMultiplicity == -1) {
            return "${prefix}may be ${role} one or more"
        }

        if (minMultiplicity == 1 && maxMultiplicity == -1) {
            return "${prefix}must be ${role} one or more"
        }

        return "${prefix}must be ${role} one and only one"
    }
}
