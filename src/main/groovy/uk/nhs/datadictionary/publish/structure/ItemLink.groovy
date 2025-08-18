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
import org.maurodata.dita.elements.langref.base.XRef
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.changePaper.ChangeAware

class ItemLink implements DitaAware<XRef>, HtmlBuilder, ChangeAware {
    final UUID itemId
    final UUID branchId

    final String stereotype
    final DictionaryItem.DictionaryItemState state
    final String name

    final String outputClass

    final DiffStatus diffStatus

    ItemLink(
        UUID itemId,
        UUID branchId,
        String stereotype,
        DictionaryItem.DictionaryItemState state,
        String name,
        String outputClass) {
        this(itemId, branchId, stereotype, state, name, outputClass, DiffStatus.NONE)
    }

    ItemLink(
        UUID itemId,
        UUID branchId,
        String stereotype,
        DictionaryItem.DictionaryItemState state,
        String name,
        String outputClass,
        DiffStatus diffStatus) {
        this.itemId = itemId
        this.branchId = branchId
        this.stereotype = stereotype
        this.state = state
        this.name = name
        this.outputClass = outputClass
        this.diffStatus = diffStatus
    }

    static ItemLink create(NhsDataDictionaryComponent component) {
        new ItemLink(
            component.catalogueItemId,
            component.branchId,
            component.stereotype,
            component.itemState,
            component.name,
            component.stereotypeForPreview)
    }

    @Override
    XRef generateDita(PublishContext context) {
        String officialName = PublishHelper.createOfficialName(this.name, this.state)
        String linkOutputClass = PublishHelper.createItemCssClass(this.outputClass, this.state)
        String xrefId = PublishHelper.createXrefId(stereotype, name, state)

        XRef.build(format: "html", keyRef: xrefId, outputClass: linkOutputClass) {
            txt officialName
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        // Differentiate between what strings are used. outputClass goes into HTTP URLs e.g. lowercase stereotype like "element", "attribute", etc
        // The full CSS class to use is the stereotype e.g. "element" but may also optionally include "retired" depending on the state
        String officialName = PublishHelper.createOfficialName(this.name, this.state)
        String linkOutputClass = PublishHelper.createItemCssClass(this.outputClass, this.state)
        String urlStereotype = this.outputClass

        String href = "#/preview/${branchId}/${urlStereotype}/${itemId}"

        builder.a(class: linkOutputClass, title: officialName, href: href) {
            mkp.yield(officialName)
        }
    }

    @Override
    String getDiscriminator() {
        // Don't use the item ID because when comparing item links they will probably be from different
        // branches. Use a more relaxed identifier which could still show differences
        "${stereotype}_${name}".toLowerCase().replace(" ", "_")
    }

    ItemLink cloneWithDiff(DiffStatus diffStatus) {
        new ItemLink(this.itemId, this.branchId, this.stereotype, this.state, this.name, this.outputClass, diffStatus)
    }
}
