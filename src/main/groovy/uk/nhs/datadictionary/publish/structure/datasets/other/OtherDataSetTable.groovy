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
import org.maurodata.dita.elements.langref.base.Table
import org.maurodata.dita.enums.Align
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.HtmlConstants
import uk.nhs.datadictionary.publish.structure.StandardColumn
import uk.nhs.datadictionary.publish.structure.datasets.DataSetTable

class OtherDataSetTable implements DataSetTable {
    static final OtherDataSetColumn MANDATION_COLUMN = new OtherDataSetColumn(
        "col1",
        "Mandation",
        "2*",
        "20%",
        Align.CENTER)

    static final OtherDataSetColumn DATA_ELEMENTS_COLUMN = new OtherDataSetColumn(
        "col2",
        "Data Elements",
        "8*",
        "80%")

    final OtherDataSetHeader header
    final List<OtherDataSetGroup> groups
    final String groupSeparator

    final DiffStatus diffStatus

    OtherDataSetTable(
        OtherDataSetHeader header,
        List<OtherDataSetGroup> groups,
        String groupSeparator) {
        this(header, groups, groupSeparator, DiffStatus.NONE)
    }

    OtherDataSetTable(
        OtherDataSetHeader header,
        List<OtherDataSetGroup> groups,
        String groupSeparator,
        DiffStatus diffStatus) {
        this.header = header
        this.groups = groups
        this.groupSeparator = groupSeparator
        this.diffStatus = diffStatus
    }

    boolean hasSingleGroup() {
        groups.size() <= 1
    }

    List<OtherDataSetColumn> getColumnList() {
        [MANDATION_COLUMN, DATA_ELEMENTS_COLUMN]
    }

    @Override
    String getDiscriminator() {
        // Use the "name" of the table as the identifier (all we have)
        "table:${header.discriminator}"
    }

    @Override
    DataSetTable cloneWithDiffStatus(DiffStatus diffStatus) {
        new OtherDataSetTable(
            this.header,
            this.groups,
            this.groupSeparator,
            diffStatus)
    }

    @Override
    DiffStatus getHierarchicalDiffStatus() {
        if (this.diffStatus != DiffStatus.NONE) {
            return this.diffStatus
        }

        if (this.header.diffStatus != DiffStatus.NONE) {
            return this.header.diffStatus
        }

        if (this.groups.any { it.hierarchicalDiffStatus != DiffStatus.NONE }) {
            return DiffStatus.MODIFIED
        }

        DiffStatus.NONE
    }

    @Override
    DataSetTable produceDiff(DataSetTable previous) {
        OtherDataSetTable previousTable = previous as OtherDataSetTable

        OtherDataSetHeader diffHeader = this.header.produceDiff(previousTable?.header)

        List<OtherDataSetGroup> currentGroups = this.groups
        List<OtherDataSetGroup> previousGroups = previousTable ? previousTable.groups : []

        List<OtherDataSetGroup> diffGroups = ChangeFunctions.buildDifferencesList(
            currentGroups,
            previousGroups,
            { OtherDataSetGroup currentItem, OtherDataSetGroup previousItem ->
                currentItem.produceDiff(previousItem)
            })

        if (!diffGroups) {
            // Seems to have found no changes within the groups, just default to current set
            diffGroups = currentGroups
        }

        boolean anyGroupChanges = diffGroups.any { it.hierarchicalDiffStatus != DiffStatus.NONE }
        if (!diffHeader && !anyGroupChanges) {
            // No differences found
            null
        }

        new OtherDataSetTable(diffHeader ?: this.header, diffGroups, this.groupSeparator)
    }

    @Override
    Table generateDita(PublishContext context) {
        String tableCssClass = PublishHelper.combineCssClassWithDiffStatus(
            context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_DATA_SET_TABLE : "",
            diffStatus)
        String headCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HEAD : ""

        Table.build(outputClass: tableCssClass) {
            tgroup(cols: columnList.size()) {
                columnList.eachWithIndex { column, index ->
                    colspec(colnum: (index + 1), colName: column.colId, colwidth: column.relColWidth, align: column.alignment)
                }
                tHead {
                    row(outputClass: headCssClass) {
                        entry this.header.generateDita(context)
                    }

                    if (hasSingleGroup()) {
                        row {
                            columnList.each { column ->
                                entry(align: Align.CENTER) {
                                    p column.name
                                }
                            }
                        }
                    }
                }
                tBody {
                    this.groups.eachWithIndex {OtherDataSetGroup group, int index ->
                        if (index != 0) {
                            // Group separator
                            row {
                                entry (namest: "col1", nameend: "col2") {
                                    if (!this.groupSeparator.empty) {
                                        b this.groupSeparator
                                    }
                                    else {
                                        p ""
                                    }
                                }
                            }
                        }

                        List<Row> rows = group.generateDita(context)
                        rows.each { innerRow ->
                            row innerRow
                        }
                    }
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String tableCssClass = PublishHelper.combineCssClassWithDiffStatus(
            context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HTML_TOPIC_TABLE : "",
            diffStatus)

        String headCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_TOPIC_HEAD}" : null
        String headerRowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW} ${HtmlConstants.CSS_TABLE_HEAD}" : null
        String rowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW}" : null
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""
        String centerEntryCssClass = "${entryCssClass} ${HtmlConstants.CSS_HTML_ALIGN_CENTER}".trim()
        String bodyCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_TOPIC_BODY}" : null

        builder.table (class: tableCssClass) {
            colgroup {
                columnList.each { column ->
                    builder.col(style: "width: ${column.cssColWidth}")
                }
            }
            builder.thead(class: headCssClass) {
                builder.tr(class: headerRowCssClass) {
                    this.header.buildHtml(context, builder)
                }

                if (hasSingleGroup()) {
                    builder.tr(class: rowCssClass) {
                        builder.th(class: PublishHelper.combineCssClasses(entryCssClass, HtmlConstants.CSS_HTML_ALIGN_CENTER)) {
                            PublishHelper.buildHtmlParagraph(context, builder, MANDATION_COLUMN.name)
                        }

                        builder.th(class: entryCssClass) {
                            PublishHelper.buildHtmlParagraph(context, builder, DATA_ELEMENTS_COLUMN.name)
                        }
                    }
                }
            }
            builder.tbody(class: bodyCssClass) {
                this.groups.eachWithIndex {OtherDataSetGroup group, int index ->
                    if (index != 0) {
                        // Group separator
                        builder.tr(class: rowCssClass) {
                            builder.td(class: centerEntryCssClass, colspan: 2) {
                                builder.b() {
                                    mkp.yield(this.groupSeparator)
                                }
                            }
                        }
                    }

                    group.buildHtml(context, builder)
                }
            }
        }
    }
}

class OtherDataSetColumn extends StandardColumn {
    final String colId
    final Align alignment

    OtherDataSetColumn(
        String colId,
        String name,
        String relColWidth,
        String cssColWidth) {
        this(colId, name, relColWidth, cssColWidth, Align.LEFT)
    }

    OtherDataSetColumn(
        String colId,
        String name,
        String relColWidth,
        String cssColWidth,
        Align alignment) {
        super(name, relColWidth, cssColWidth)

        this.colId = colId
        this.alignment = alignment
    }
}