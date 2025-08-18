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

import org.maurodata.domain.datamodel.DataElement

class NhsDDClassLink {
    static final String IS_KEY_METADATA_KEY = "isKey"
    static final String IS_CHOICE_METADATA_KEY = "isChoice"
    static final String DIRECTION_METADATA_KEY = "direction"

    static final String CLIENT_DIRECTION = "client"
    static final String SUPPLIER_DIRECTION = "supplier"

    String uin
    String metaclass
    String clientRole
    String supplierRole
    String clientCardinality
    String supplierCardinality
    String name
    String partOfClientKey
    String partOfSupplierKey
    String supplierUin
    String clientUin
    String relationSupplierExclusivity
    String relationClientExclusivity
    String direction

    NhsDDClass clientClass
    NhsDDClass supplierClass



    NhsDDClassLink(def link) {
        uin = link."@uin"
        metaclass = link.metaclass.text()
        clientRole = link.clientRole.text()
        supplierRole = link.supplierRole.text()
        clientCardinality = link.clientCardinality.text()
        supplierCardinality = link.supplierCardinality.text()
        name = link.name.text()
        partOfClientKey = link.partOfClientKey.text()
        partOfSupplierKey = link.partOfSupplierKey.text()
        relationSupplierExclusivity = link.relationSupplierExclusivity.text()
        relationClientExclusivity = link.relationClientExclusivity.text()
        supplierUin = link.participant.find {it."@role" == "Supplier"}."@referencedUin"
        clientUin = link.participant.find {it."@role" == "Client"}."@referencedUin"
    }

    boolean isPartOfClientKey() {
        partOfClientKey == "true"
    }

    boolean isPartOfSupplierKey() {
        partOfSupplierKey == "true"
    }

    boolean hasRelationClientExclusivity() {
        relationClientExclusivity != null && !relationClientExclusivity.empty
    }

    boolean hasRelationSupplierExclusivity() {
        relationSupplierExclusivity != null && !relationSupplierExclusivity.empty
    }

/*    ClassLink(DataElement dataElement) {
        uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        metaclass = DDHelperFunctions.getMetadataValue(dataElement, "metaclass")
        clientRole = DDHelperFunctions.getMetadataValue(dataElement, "clientRole")
        supplierRole = DDHelperFunctions.getMetadataValue(dataElement, "supplierRole")
        clientCardinality = DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality")
        supplierCardinality = DDHelperFunctions.getMetadataValue(dataElement, "supplierCardinality")
        name = DDHelperFunctions.getMetadataValue(dataElement, "name")
        partOfClientKey = DDHelperFunctions.getMetadataValue(dataElement, "partOfClientKey")
        partOfSupplierKey = DDHelperFunctions.getMetadataValue(dataElement, "partOfSupplierKey")
        supplierUin = DDHelperFunctions.getMetadataValue(dataElement, "supplierUin")
        clientUin = DDHelperFunctions.getMetadataValue(dataElement, "clientUin")
        relationSupplierExclusivity = DDHelperFunctions.getMetadataValue(dataElement, "relationSupplierExclusivity")
        relationClientExclusivity = DDHelperFunctions.getMetadataValue(dataElement, "relationClientExclusivity")
        direction = DDHelperFunctions.getMetadataValue(dataElement, "direction")
        //        this.dataElement = dataElement
    }
*/
    String toTableText() {
        String cardinality = clientCardinality
        if(direction == "client") {
            cardinality = supplierCardinality
        }
        String role = clientRole
        if(direction == "client") {
            role = supplierRole
        }
        return getCardinalityText(cardinality, role)

    }

/*    void addMetadata(DataElement dataElement) {
        DDHelperFunctions.addMetadata(dataElement, "uin", uin)
        DDHelperFunctions.addMetadata(dataElement, "metaclass", metaclass)
        DDHelperFunctions.addMetadata(dataElement, "clientRole", clientRole)
        DDHelperFunctions.addMetadata(dataElement, "supplierRole", supplierRole)
        DDHelperFunctions.addMetadata(dataElement, "clientCardinality", clientCardinality)
        DDHelperFunctions.addMetadata(dataElement, "supplierCardinality", supplierCardinality)
        DDHelperFunctions.addMetadata(dataElement, "name", name)
        DDHelperFunctions.addMetadata(dataElement, "partOfClientKey", partOfClientKey)
        DDHelperFunctions.addMetadata(dataElement, "partOfSupplierKey", partOfSupplierKey)
        DDHelperFunctions.addMetadata(dataElement, "supplierUin", supplierUin)
        DDHelperFunctions.addMetadata(dataElement, "clientUin", clientUin)
        DDHelperFunctions.addMetadata(dataElement, "relationSupplierExclusivity", relationSupplierExclusivity)
        DDHelperFunctions.addMetadata(dataElement, "relationClientExclusivity", relationClientExclusivity)
        DDHelperFunctions.addMetadata(dataElement, "direction", direction)


    }
*/
    static String getCardinalityText(String cardinality, String role) {
        switch(cardinality) {
            case "0..1":
                return "may be ${role} one and only one "
            case "0..*":
                return "may be ${role} one or more "
            case "1..*":
                return "must be ${role} one or more "
            case "1":
                return "must be ${role} one and only one "
            default: // we'll assume 1..1
                return "must be ${role} one and only one "
        }
    }

    static void setMultiplicityToDataElement(DataElement dataElement, String cardinality) {
        switch (cardinality) {
            case "0..1":
                dataElement.minMultiplicity = 0
                dataElement.maxMultiplicity = 1
                break
            case "0..*":
                dataElement.minMultiplicity = 0
                dataElement.maxMultiplicity = -1
                break
            case "1..*":
                dataElement.minMultiplicity = 1
                dataElement.maxMultiplicity = -1
                break
            default: // we'll assume 1..1
                dataElement.minMultiplicity = 1
                dataElement.maxMultiplicity = 1
                break
        }
    }

    /*
    void findTargetClass(uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary nhsDataDictionary) {
        supplierClass = nhsDataDictionary.classesByUin[supplierUin]
        if(!supplierClass) {
            log.error("Cannot match supplier class relationship: {} :: SupplierUin: {}\n{}", uin, supplierUin, this.toString())
        }
        clientClass = nhsDataDictionary.classesByUin[clientUin]
        if(!clientClass) {
            log.error("Cannot match client class relationship: {} ClientUin: {}\n{}", uin, clientUin, this.toString())
        }

    }
*/

}
