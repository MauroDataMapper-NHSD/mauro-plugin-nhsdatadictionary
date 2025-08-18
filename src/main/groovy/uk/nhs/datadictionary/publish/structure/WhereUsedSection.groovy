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

class WhereUsedSection extends Section {
    final WhereUsedTable table

    WhereUsedSection(DictionaryItem parent, WhereUsedTable table) {
        super(parent, "whereUsed", "Where Used")

        this.table = table
    }

    WhereUsedSection(DictionaryItem parent, List<WhereUsedRow> rows) {
        this(parent, new WhereUsedTable(rows))
    }

    @Override
    Section produceDiff(Section previous) {
        // Do not compare "Where Used" section
        return null
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            simpletable table.generateDita(context)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            simpletable table.generateDita(context)
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        table.buildHtml(context, builder)
    }
}

class WhereUsedTable extends StandardTable<WhereUsedRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Type", "1*", "15%"),
        new StandardColumn("Link", "3*", "45%"),
        new StandardColumn("How used", "2*", "40%"),
    ]

    WhereUsedTable(List<WhereUsedRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable<WhereUsedRow> cloneWithRows(List<WhereUsedRow> rows) {
        new WhereUsedTable(rows)
    }
}

class WhereUsedRow extends StandardRow {
    final String type
    final ItemLink link
    final String usage

    WhereUsedRow(String type, ItemLink link, String usage) {
        this.type = type
        this.link = link
        this.usage = usage
    }

    @Override
    Strow generateDita(PublishContext context) {
        Strow.build() {
            stentry type
            stentry {
                xRef link.generateDita(context)
            }
            stentry usage
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_STROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                mkp.yield(type)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                link.buildHtml(context, builder)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                mkp.yield(usage)
            }
        }
    }

    @Override
    String getDiscriminator() {
        // No change detection required
        return null
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        // No change detection required
        return null
    }
}

