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
package uk.nhs.datadictionary.publish.structure.datasets.other

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Row
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.datadictionary.publish.structure.DiffAware
import uk.nhs.datadictionary.publish.structure.DiffHierarchyAware
import uk.nhs.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.DitaAware
import uk.nhs.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.datadictionary.publish.structure.HtmlConstants

class OtherDataSetGroup implements
    DitaAware<List<Row>>,
    HtmlBuilder,
    ChangeAware,
    DiffAware<OtherDataSetGroup, OtherDataSetGroup>,
    DiffObjectAware<OtherDataSetGroup>,
    DiffHierarchyAware {
    final List<OtherDataSetRow> rows
    final OtherDataSetHeader header

    final DiffStatus diffStatus

    OtherDataSetGroup(List<OtherDataSetRow> rows, OtherDataSetHeader header) {
        this(rows, header, DiffStatus.NONE)
    }

    OtherDataSetGroup(List<OtherDataSetRow> rows, OtherDataSetHeader header, DiffStatus diffStatus) {
        this.rows = rows
        this.header = header
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        // Use the header as the "name" of the group, only way to identify it
        String headerDiscriminator = this.header?.discriminator ?: "header:none"
        "group:${headerDiscriminator}"
    }

    @Override
    DiffStatus getHierarchicalDiffStatus() {
        if (this.diffStatus != DiffStatus.NONE) {
            return this.diffStatus
        }

        if (this.header && this.header.diffStatus != DiffStatus.NONE) {
            return this.header.diffStatus
        }

        if (this.rows.any { it.diffStatus != DiffStatus.NONE }) {
            return DiffStatus.MODIFIED
        }

        DiffStatus.NONE
    }

    @Override
    OtherDataSetGroup cloneWithDiffStatus(DiffStatus diffStatus) {
        // If this whole group is new/deleted, the individual rows need to be styled to show that
        List<OtherDataSetRow> clonedRows = diffStatus != DiffStatus.NONE
            ? this.rows.collect {row -> row.cloneWithDiffStatus(diffStatus) }
            : this.rows

        new OtherDataSetGroup(clonedRows, this.header, diffStatus)
    }

    @Override
    OtherDataSetGroup produceDiff(OtherDataSetGroup previous) {
        OtherDataSetHeader diffHeader = this.header?.produceDiff(previous?.header)

        List<OtherDataSetRow> currentRows = this.rows
        List<OtherDataSetRow> previousRows = previous ? previous.rows : []

        if (currentRows.empty && previousRows.empty) {
            return null
        }

        List<OtherDataSetRow> diffRows = ChangeFunctions.buildDifferencesList(currentRows, previousRows)

        if (!diffRows) {
            // Seems to have found no changes within the rows, just default to current set
            diffRows = currentRows
        }

        boolean anyRowChanges = diffRows.any { it.diffStatus != DiffStatus.NONE }
        if (!diffHeader && !anyRowChanges) {
            // No differences found
            null
        }

        new OtherDataSetGroup(diffRows, diffHeader ?: this.header)
    }

    @Override
    List<Row> generateDita(PublishContext context) {
        String rowCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_PRIMARY : null

        List<Row> rowList = []

        if (this.header) {
            rowList.add(Row.build(outputClass: PublishHelper.combineCssClassWithDiffStatus(rowCssClass, this.diffStatus)) {
                entry(namest: OtherDataSetTable.MANDATION_COLUMN.colId, nameend: OtherDataSetTable.MANDATION_COLUMN.colId) {
                    p {
                        b OtherDataSetTable.MANDATION_COLUMN.name
                    }
                }
                entry this.header.generateDita(context)
            })
        }

        rowList.addAll(this.rows.collect { row -> row.generateDita(context) })

        rowList
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String rowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW} ${HtmlConstants.CSS_TABLE_PRIMARY}" : null
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""

        if (this.header) {
            builder.tr(class: PublishHelper.combineCssClassWithDiffStatus(rowCssClass, this.diffStatus)) {
                builder.td(class: PublishHelper.combineCssClasses(entryCssClass, HtmlConstants.CSS_HTML_ALIGN_CENTER)) {
                    b OtherDataSetTable.MANDATION_COLUMN.name
                }
                this.header.buildHtml(context, builder)
            }
        }

        this.rows.each { row ->
            row.buildHtml(context, builder)
        }
    }
}
