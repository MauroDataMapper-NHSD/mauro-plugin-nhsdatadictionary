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

import org.maurodata.domain.terminology.Term

class NhsDDWebPage extends NhsDataDictionaryComponent <Term> {

    List<String> path = []

    @Override
    String getStereotype() {
        "Web Page"
    }

    @Override
    String getStereotypeForPreview() {
        "webPage"
    }

    @Override
    String getPluralStereotypeForWebsite() {
        "web_pages"
    }

    @Override
    String getMetadataNamespace() {
        return NhsDataDictionary.METADATA_NAMESPACE + '.webpage'
    }

    @Override
    boolean isValidXmlNode(def xmlNode) {
        return !xmlNode."base-uri".text().contains("Supporting_Definitions") &&
               !xmlNode."base-uri".text().contains("Supporting_Information") /* &&
               !xmlNode."base-uri".text().contains("CDS_Supporting_Information") */
    }


    @Override
    String calculateShortDescription() {
        return name
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        super.fromXml(xml, dataDictionary)
        dataDictionary.webPagesByUin[getUin()] = this
        if(otherProperties["baseUri"].contains("Messages")) {
            path = getPath()
        }
    }

    String getMauroPath() {
        return ""
    }
}