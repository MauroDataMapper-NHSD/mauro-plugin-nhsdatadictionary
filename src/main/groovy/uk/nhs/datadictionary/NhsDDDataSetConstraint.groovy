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
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.terminology.Term

@Slf4j
class NhsDDDataSetConstraint extends NhsDataDictionaryComponent <Term> {

    NhsDDDataSetConstraint(Term catalogueItem = null, UUID branchId = null) {
        super(catalogueItem, branchId)
    }

    Term newCatalogueItem(String name = null) {
        return new Term(code: name)
    }

    @Override
    String getName() {
        catalogueItem.code
    }

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
        "data_set_constraints"
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".Data set constraint"
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
                log.error("Couldn't parse: " + description)
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
        super.fromXml(xml, dataDictionary)
    }

    String getMauroPath() {
        return "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}|tm:${name}"
    }
}
