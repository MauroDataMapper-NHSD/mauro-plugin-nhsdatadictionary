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

class ClassRelationshipSection extends Section {
    final ClassRelationshipTable table

    ClassRelationshipSection(DictionaryItem parent, ClassRelationshipTable table) {
        this(parent, "Relationships", table)
    }

    ClassRelationshipSection(DictionaryItem parent, List<ClassRelationshipRow> rows) {
        this(parent, new ClassRelationshipTable(rows))
    }

    ClassRelationshipSection(DictionaryItem parent, String title, ClassRelationshipTable table) {
        super(parent, "relationships", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        ClassRelationshipSection previousSection = previous as ClassRelationshipSection
        ClassRelationshipTable diffTable = this.table.produceDiff(previousSection?.table) as ClassRelationshipTable
        if (!diffTable) {
            return null
        }

        new ClassRelationshipSection(this.parent, Change.CHANGED_RELATIONSHIPS_TYPE, diffTable)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        builder.p(class: paragraphOutputClass) {
            mkp.yield(headerText)
        }

        table.buildHtml(context, builder)
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

    private String getHeaderText() {
        "Each ${parent.name}:"
    }
}

class ClassRelationshipTable extends StandardTable<ClassRelationshipRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Key", "1*", "10%"),
        new StandardColumn("Relationship", "5*", "45%"),
        new StandardColumn("Class", "5*", "45%"),
    ]

    ClassRelationshipTable(List<ClassRelationshipRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable cloneWithRows(List<ClassRelationshipRow> rows) {
        new ClassRelationshipTable(rows)
    }
}

class ClassRelationshipRow extends StandardRow {
    final boolean isKey
    final String description
    final ItemLink classLink

    ClassRelationshipRow(boolean isKey, String description, ItemLink classLink) {
        this(isKey, description, classLink, DiffStatus.NONE)
    }

    ClassRelationshipRow(boolean isKey, String description, ItemLink classLink, DiffStatus diffStatus) {
        super(diffStatus)

        this.isKey = isKey
        this.description = description
        this.classLink = classLink
    }

    @Override
    String getDiscriminator() {
        "${isKey ? "Key: " : ""}${description} ${classLink.name}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt isKey ? 'Key' : ''
            }
            stentry(outputClass: outputClass) {
                txt description
            }
            stentry(outputClass: outputClass) {
                xRef classLink.generateDita(context)
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
                mkp.yield(isKey ? 'Key' : '')
            }
            builder.td(class: entryCssClass) {
                mkp.yield(description)
            }
            builder.td(class: entryCssClass) {
                classLink.buildHtml(context, builder)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new ClassRelationshipRow(this.isKey, this.description, this.classLink, diffStatus)
    }
}
