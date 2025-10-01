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

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.terminology.Term

@Slf4j
class NhsDDBusinessDefinition implements NhsDataDictionaryComponent <Term> {

    @Override
    String getStereotype() {
        "NHS Business Definition"
    }

    @Override
    String getStereotypeForPreview() {
        "businessDefinition"
    }


    @JsonIgnore
    @Override
    String getPluralStereotypeForWebsite() {
        "nhs_business_definitions"
    }

    @JsonIgnore
    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".NHS business definition"
    }


    @Override
    String calculateShortDescription() {
        if (isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                String firstSentence = getFirstSentence()
                if (firstSentence.toLowerCase().contains("is a")) {
                    String secondSentence = getSentence(1)
                    return secondSentence
                } else {
                    return firstSentence
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @JsonIgnore
    @Override
    String getXmlNodeName() {
        "DDBusinessDefinition"
    }

    @JsonIgnore
    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }

    @JsonIgnore
    String getMauroPath() {
        return "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}|tm:${name}"
    }
}
