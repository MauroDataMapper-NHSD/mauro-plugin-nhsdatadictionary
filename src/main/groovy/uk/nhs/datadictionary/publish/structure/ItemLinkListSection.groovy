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
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.Change
import uk.nhs.datadictionary.publish.changePaper.ChangeFunctions

class ItemLinkListSection extends Section {
    static final String ATTRIBUTES_TYPE = "attributes"
    static final String ELEMENTS_TYPE = "dataElements"

    static final String ATTRIBUTES_TITLE_SINGULAR = "Attribute"
    static final String ATTRIBUTES_TITLE_PLURAL = "Attributes"
    static final String ELEMENTS_TITLE = "Data Elements"

    final List<ItemLink> itemLinks

    ItemLinkListSection(DictionaryItem parent, String type, String title, List<ItemLink> itemLinks) {
        super(parent, type, title)

        this.itemLinks = itemLinks
    }

    static ItemLinkListSection createElementsListSection(DictionaryItem parent, List<ItemLink> itemLinks) {
        new ItemLinkListSection(parent, ELEMENTS_TYPE, ELEMENTS_TITLE, itemLinks)
    }

    static ItemLinkListSection createAttributesListSection(DictionaryItem parent, List<ItemLink> itemLinks) {
        String title = itemLinks.size() > 1 ? ATTRIBUTES_TITLE_PLURAL : ATTRIBUTES_TITLE_SINGULAR
        new ItemLinkListSection(parent, ATTRIBUTES_TYPE, title, itemLinks)
    }

    @Override
    Section produceDiff(Section previous) {
        ItemLinkListSection previousSection = previous as ItemLinkListSection

        List<ItemLink> currentLinks = this.itemLinks
        List<ItemLink> previousLinks = previousSection?.itemLinks ?: []

        if (currentLinks.empty && previousLinks.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentLinks, previousLinks)) {
            return null
        }

        List<ItemLink> newLinks = ChangeFunctions.getDifferences(currentLinks, previousLinks)
        List<ItemLink> removedLinks = ChangeFunctions.getDifferences(previousLinks, currentLinks)

        if (newLinks.empty && removedLinks.empty) {
            return null
        }

        List<ItemLink> diffLinks = []
        currentLinks.each { row ->
            if (newLinks.any { it.discriminator == row.discriminator }) {
                diffLinks.add(row.cloneWithDiff(DiffStatus.NEW))
            }
            else {
                diffLinks.add(row)
            }
        }
        removedLinks.each { row ->
            diffLinks.add(row.cloneWithDiff(DiffStatus.REMOVED))
        }

        String diffTitle = this.type == ELEMENTS_TYPE ? Change.CHANGED_ELEMENTS_TYPE : Change.CHANGED_ATTRIBUTES_TYPE
        new ItemLinkListSection(parent, type, diffTitle, diffLinks)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""
        String listCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_UNORDERED_LIST : null
        String itemCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_LIST_ITEM : null

        builder.div {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                builder.p(class: paragraphOutputClass) {
                    b changePaperTitle
                }
            }
            builder.ul(class: listCssClass) {
                itemLinks.each {itemLink ->
                    String diffOutputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    String itemOutputClass = context.target == PublishTarget.WEBSITE
                        ? itemCssClass
                        : !diffOutputClass.empty
                        ? diffOutputClass
                        : null

                    builder.li(class: itemOutputClass) {
                        itemLink.buildHtml(context, builder)
                    }
                }
            }
        }
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                p {
                    b changePaperTitle
                }
            }
            ul {
                itemLinks.each { itemLink ->
                    String outputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    li(outputClass: outputClass) {
                        xRef itemLink.generateDita(context)
                    }
                }
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                p {
                    b changePaperTitle
                }
            }
            ul {
                itemLinks.each { itemLink ->
                    String outputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    li(outputClass: outputClass) {
                        xRef itemLink.generateDita(context)
                    }
                }
            }
        }
    }

    private String getChangePaperTitle() {
        if (this.type == ATTRIBUTES_TYPE) {
            return this.itemLinks.size() > 1 ? ATTRIBUTES_TITLE_PLURAL : ATTRIBUTES_TITLE_SINGULAR
        }

        return ELEMENTS_TITLE
    }
}