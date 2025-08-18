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
package uk.nhs.datadictionary.publish.structure

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Simpletable
import org.maurodata.dita.elements.langref.base.Strow
import org.maurodata.dita.meta.SpaceSeparatedStringList
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.changePaper.ChangeFunctions

abstract class StandardTable<R extends StandardRow> implements
    DitaAware<Simpletable>,
    HtmlBuilder,
    DiffAware<StandardTable<R>, StandardTable<R>> {
    final List<StandardColumn> columns
    final List<R> rows

    StandardTable(List<StandardColumn> columns, List<R> rows) {
        this.columns = columns
        this.rows = rows
    }

    boolean isEmpty() {
        !rows || rows.empty
    }

    @Override
    StandardTable<R> produceDiff(StandardTable<R> previous) {
        List<R> currentRows = this.rows
        List<R> previousRows = previous ? previous.rows : []

        if (currentRows.empty && previousRows.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentRows, previousRows)) {
            return null
        }

        List<R> newRows = ChangeFunctions.getDifferences(currentRows, previousRows)
        List<R> removedRows = ChangeFunctions.getDifferences(previousRows, currentRows)

        if (newRows.empty && removedRows.empty) {
            return null
        }

        List<R> diffRows = []
        currentRows.each { row ->
            if (newRows.any { it.discriminator == row.discriminator }) {
                diffRows.add(row.cloneWithDiff(DiffStatus.NEW) as R)
            }
            else {
                diffRows.add(row)
            }
        }
        removedRows.each { row ->
            diffRows.add(row.cloneWithDiff(DiffStatus.REMOVED) as R)
        }

        cloneWithRows(diffRows)
    }

    @Override
    Simpletable generateDita(PublishContext context) {
        List<String> relColWidths = columns.collect {column -> column.relColWidth }

        String tableCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE : ""
        String headCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HEAD : ""

        Simpletable.build(relColWidth: new SpaceSeparatedStringList (relColWidths), outputClass: tableCssClass) {
            stHead (outputClass: headCssClass) {
                columns.each { column ->
                    stentry column.name
                }
            }
            rows.each { row ->
                strow row.generateDita(context)
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String containerCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HTML_CONTAINER_SIMPLE : null
        String tableCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_HTML_TOPIC_SIMPLETABLE} $HtmlConstants.CSS_TABLE" : null
        String headCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_TOPIC_STHEAD} ${HtmlConstants.CSS_TABLE_HEAD}" : null
        String entryCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_STENTRY : null

        builder.div(class: containerCssClass) {
            builder.table(class: tableCssClass) {
                colgroup {
                    columns.each { column ->
                        builder.col(style: "width: ${column.cssColWidth}")
                    }
                }
                thead {
                    builder.tr(class: headCssClass) {
                        columns.each { column ->
                            builder.th(class: entryCssClass) {
                                mkp.yield(column.name)
                            }
                        }
                    }
                }
                tbody {
                    rows.each { row ->
                        row.buildHtml(context, builder)
                    }
                }
            }
        }
    }

    protected abstract StandardTable<R> cloneWithRows(List<R> rows)
}

class StandardColumn {
    final String name
    final String relColWidth
    final String cssColWidth

    StandardColumn(String name, String relColWidth, String cssColWidth) {
        this.name = name
        this.relColWidth = relColWidth
        this.cssColWidth = cssColWidth
    }
}

abstract class StandardRow implements DitaAware<Strow>, HtmlBuilder, ChangeAware {
    final DiffStatus diffStatus

    StandardRow() {
        this(DiffStatus.NONE)
    }

    StandardRow(DiffStatus diffStatus) {
        this.diffStatus = diffStatus
    }

    protected abstract StandardRow cloneWithDiff(DiffStatus diffStatus)
}


