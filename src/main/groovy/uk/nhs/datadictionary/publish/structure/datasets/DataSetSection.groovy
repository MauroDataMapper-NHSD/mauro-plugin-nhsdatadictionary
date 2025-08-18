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
package uk.nhs.datadictionary.publish.structure.datasets

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Body
import org.maurodata.dita.elements.langref.base.Div
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.HtmlConstants
import uk.nhs.datadictionary.publish.structure.Section

class DataSetSection extends Section {
    final List<DataSetTable> tables

    DataSetSection(DictionaryItem parent, List<DataSetTable> tables) {
        super(parent, "specification", "Specification")

        this.tables = tables ?: []
    }

    @Override
    Section produceDiff(Section previous) {
        DataSetSection previousSection = previous as DataSetSection

        List<DataSetTable> currentTables = this.tables
        List<DataSetTable> previousTables = previousSection ? previousSection.tables : []

        List<DataSetTable> diffTables = ChangeFunctions.buildDifferencesList(
            currentTables,
            previousTables,
            { DataSetTable currentItem, DataSetTable previousItem ->
                currentItem.produceDiff(previousItem)
            })

        if (!diffTables) {
            // Seems to have found no changes within the tables, just default to current set
            diffTables = currentTables
        }

        if (diffTables.every {it.hierarchicalDiffStatus == DiffStatus.NONE }) {
            // No differences found in these sections
            return null
        }

        // Otherwise, return all the calculated differences
        new DataSetSection(this.parent, diffTables)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            // Use different closure name than "table", otherwise dita dsl thinks its a method call
            this.tables.each { tableStruct ->
                div {
                    table tableStruct.generateDita(context)
                }
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            // Use different closure name than "table", otherwise dita dsl thinks its a method call
            this.tables.each { tableStruct ->
                div {
                    table tableStruct.generateDita(context)
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String containerCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HTML_CONTAINER : null

        this.tables.each { table ->
            builder.div(class: containerCssClass) {
                table.buildHtml(context, builder)
            }
        }
    }
}
