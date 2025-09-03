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
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder

@Slf4j
class NhsDDDataSetFolder implements NhsDataDictionaryComponent <Folder> {

    List<String> folderPath = []

    Map<String, NhsDDDataSetFolder> childFolders = [:]
    Map<String, NhsDDDataSet> dataSets = [:]

    @Override
    String getStereotype() {
        "Data Set Folder"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "data_sets"
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".data set folder"
    }


    @Override
    boolean isValidXmlNode(def xmlNode) {
        return xmlNode."name".text().contains("Introduction") &&
               xmlNode."base-uri".text().contains("Messages")
    }


    @Override
    String calculateShortDescription() {
        if(!definition || definition == "") {
            return name
        }
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [name]
            aliases.addAll(getAliases().values())

            try {

                List<String> allSentences = calculateSentences(definition?:"")

                if(isRetired()) {
                    return allSentences[0]
                } else {
                    return allSentences.find {sentence ->
                        aliases.find {alias ->
                            sentence.contains(alias)
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + name)
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        //dataDictionary.webPagesByUin[getUin()] = this
        if(otherProperties["baseUri"].contains("Messages")) {
            folderPath = getWebPath()
        }
    }

    String getMauroPath() {
        folderPath
            .collect { pathComponent -> "fo:${pathComponent}" }
            .join("|")
    }

    Map<String, String> getUrlReplacements() {
        if(!this.otherProperties["ddUrl"]) {
            return [:]
        }
        String ddUrl = this.otherProperties["ddUrl"]
        String ddUrl2 = ddUrl.replace("introduction", "menu")
        String ddUrl3 = ddUrl.replace("introduction", "overview")

        String frontEndUrl = this.mauroPath

        return [
            (ddUrl) : frontEndUrl,
            (ddUrl2) : frontEndUrl,
            (ddUrl3) : frontEndUrl
        ]
    }


    List<String> getDitaFolderPath() {
        folderPath.collect {
            it.replaceAll("[^A-Za-z0-9- ]", "").replace(" ", "_")
        }
    }


    void setPath(List<String> path) {
        folderPath.addAll(path)
        folderPath.add(catalogueItem.label)
    }



}