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
import org.maurodata.dita.elements.langref.base.Body
import org.maurodata.dita.elements.langref.base.Div
import org.maurodata.dita.elements.langref.base.Strow
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.Change

class AliasesSection extends Section {
    final AliasesTable table

    AliasesSection(DictionaryItem parent, AliasesTable table) {
        this(parent, "Also Known As", table)
    }

    AliasesSection(DictionaryItem parent, List<AliasesRow> rows) {
        this(parent, new AliasesTable(rows))
    }

    AliasesSection(DictionaryItem parent, String title, AliasesTable table) {
        super(parent, "aliases", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        AliasesSection previousSection = previous as AliasesSection
        AliasesTable diffTable = this.table.produceDiff(previousSection?.table) as AliasesTable
        if (!diffTable) {
            return null
        }

        new AliasesSection(this.parent, Change.ALIASES_TYPE, diffTable)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            p headerText
            simpletable table.generateDita(context)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            p headerText
            simpletable table.generateDita(context)
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        builder.p(class: paragraphOutputClass) {
            mkp.yield(headerText)
        }

        table.buildHtml(context, builder)
    }

    String getHeaderText() {
        "This ${parent.stereotype} is also known by these names:"
    }
}

class AliasesTable extends StandardTable<AliasesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Context", "1*", "34%"),
        new StandardColumn("Alias", "2*", "66%"),
    ]

    AliasesTable(List<AliasesRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable<AliasesRow> cloneWithRows(List<AliasesRow> rows) {
        new AliasesTable(rows)
    }
}

class AliasesRow extends StandardRow {
    final String context
    final String alias

    AliasesRow(String context, String alias) {
        this(context, alias, DiffStatus.NONE)
    }

    AliasesRow(String context, String alias, DiffStatus diffStatus) {
        super(diffStatus)
        this.context = context
        this.alias = alias
    }

    @Override
    String getDiscriminator() {
        "context:${context}-alias:${alias}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt this.context
            }
            stentry(outputClass: outputClass) {
                txt this.alias
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String diffOutputClass = PublishHelper.getDiffCssClass(diffStatus)

        String rowCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_STROW : null
        String entryCssClass = context.target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_STENTRY
            : !diffOutputClass.empty
                                   ? diffOutputClass
                                   : null

        builder.tr(class: rowCssClass) {
            builder.td(class: entryCssClass) {
                mkp.yield(this.context)
            }
            builder.td(class: entryCssClass) {
                mkp.yield(this.alias)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new AliasesRow(this.context, this.alias, diffStatus)
    }
}