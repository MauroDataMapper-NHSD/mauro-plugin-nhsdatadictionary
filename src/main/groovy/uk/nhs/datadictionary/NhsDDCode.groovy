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

import org.maurodata.dita.elements.langref.base.Strow
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.meta.SpaceSeparatedStringList
import org.maurodata.domain.terminology.Term
import uk.nhs.datadictionary.publish.changePaper.ChangeAware

class NhsDDCode implements ChangeAware {

    String code
    String definition
    String publishDate
    Integer webOrder
    String webPresentation
    Boolean isDefault

    NhsDDAttribute owningAttribute
    List<NhsDDElement> usedByElements = []

    Term catalogueItem

    boolean isRetired
    String retiredDate

    Map<String, String> propertiesAsMap() {
        return [
            "publishDate"       : publishDate,
            "isRetired"         : isRetired.toString(),
            "retiredDate"       : retiredDate,
            "webOrder"          : "${webOrder}",
            "webPresentation"   : webPresentation,
            "isDefault"         : isDefault.toString()
        ]
    }

    NhsDDCode(NhsDDAttribute owningAttribute) {
        this.owningAttribute = owningAttribute
    }

    NhsDDCode(NhsDDElement owningElement) {
        this.usedByElements.add(owningElement)
    }

    NhsDDCode() {

    }

    boolean hasWebPresentation() {
        webPresentation != null
    }

    String getDescription() {
        String description = webPresentation ?: definition

        // Some of the ingested branches seem to already contain the "(Retired [date])" text in the definition, don't duplicate it
        if (isRetired && !description.contains("Retired")) {
            String retiredDateString = ""
            if (retiredDate) {
                // The profile field to store the retired date is just a "string" type, so just have to assume
                // that it is a valid date
                retiredDateString = " " + retiredDate
            }

            String retiredText = "(Retired$retiredDateString)"

            if (webPresentation) {
                description += "<span>" + retiredText + "</span>"
            }
            else {
                description += " " + retiredText
            }
        }

        description
    }

    Strow toDitaTableRow() {
        Strow.build(outputClass: isRetired ? "retired" : "") {
            stentry code
            stentry {
                if(webPresentation) {
                    div HtmlHelper.replaceHtmlWithDita(getDescription())
                } else {
                    txt getDescription()
                }
            }
        }
    }

    static getCodesTopic(String topicId, String topicTitle, List<NhsDDCode> orderedCodes) {
        Topic.build (id: topicId) {
            title topicTitle
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "4*"]), outputClass: "table table-sm table-striped") {
                    stHead (outputClass: "thead-light") {
                        stentry "Code"
                        stentry "Description"
                    }
                    orderedCodes.each {code ->
                        strow code.toDitaTableRow()
                    }
                }
            }
        }
    }


    @Override
    String getDiscriminator() {
        "${code} - ${definition}"
    }
}
