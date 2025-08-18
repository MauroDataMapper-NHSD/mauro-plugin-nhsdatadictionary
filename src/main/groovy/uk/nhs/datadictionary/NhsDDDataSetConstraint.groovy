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

import groovy.util.logging.Slf4j
import org.maurodata.domain.terminology.Term

@Slf4j
class NhsDDDataSetConstraint implements NhsDataDictionaryComponent <Term> {

    @Override
    String getStereotype() {
        "Data Set Constraint"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSetConstraint"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "xml_schema_constraints"
    }

    @Override
    String calculateShortDescription() {
        if (isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                return getFirstSentence()
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDXmlSchemaConstraint"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }

    String getMauroPath() {
        return "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}|tm:${name}"
    }
}
