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
package datadictionary.publish.structure

import jakarta.inject.Inject
import spock.lang.Specification
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.publish.ItemLinkScanner
import uk.nhs.datadictionary.publish.NhsDataDictionaryComponentPathResolver
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.services.NhsDataDictionaryService

abstract class DataDictionaryComponentStructureSpec<T extends NhsDataDictionaryComponent> extends Specification {

    @Inject NhsDataDictionaryService nhsDataDictionaryService

    NhsDataDictionary dataDictionary

    UUID branchId
    String definition
    NhsDataDictionaryComponentPathResolver componentPathResolver
    MockCatalogueItemPathResolver catalogueItemPathResolver

    T activeItem
    T retiredItem
    T preparatoryItem

    T previousItemDescriptionChange
    T newItemDescriptionChange
    T previousItemAliasesChange
    T previousItemAllChange

    PublishContext websiteDitaPublishContext
    PublishContext websiteHtmlPublishContext
    PublishContext changePaperDitaPublishContext
    PublishContext changePaperHtmlPublishContext

    def setup() {
        branchId = UUID.fromString("782602d4-e153-45d8-a271-eb42396804da")

        dataDictionary = createDataDictionary(branchId)


        definition = getDefinition()
        setupRelatedItems()

        setupActiveItem()
        setupRetiredItem()
        setupPreparatoryItem()
        setupPreviousItems()

        if (retiredItem) {
            retiredItem.otherProperties["isRetired"] = true.toString()
        }

        if (preparatoryItem) {
            preparatoryItem.otherProperties["isPreparatory"] = true.toString()
        }

        setupComponentPathResolver()
        setupCatalogueItemPathResolver()
        setupPublishContexts()
    }

    protected NhsDataDictionary createDataDictionary(UUID branchId) {
        nhsDataDictionaryService.newDataDictionary(branchId)
    }

    protected static <K, V> Map<K, V> copyMap(Map<K, V> properties) {
        Map<K, V> copy = [:]
        properties.each { key, value -> copy[key] = value }
        copy
    }

    abstract String getDefinition()

    abstract void setupRelatedItems()

    protected void setupComponentPathResolver() {
        componentPathResolver = new NhsDataDictionaryComponentPathResolver()

        // Self-reference this item
        componentPathResolver.add(activeItem.getMauroPath(), activeItem)
    }

    protected void setupCatalogueItemPathResolver() {
        catalogueItemPathResolver = new MockCatalogueItemPathResolver()

        // Self-reference this item
        catalogueItemPathResolver.add(activeItem.getMauroPath(), activeItem.catalogueItem.id)
    }

    private void setupPublishContexts() {
        ItemLinkScanner ditaItemLinkScanner = ItemLinkScanner.createForDitaOutput(componentPathResolver)
        ItemLinkScanner htmlItemLinkScanner = ItemLinkScanner.createForHtmlPreview(branchId, catalogueItemPathResolver)

        websiteDitaPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        websiteHtmlPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)

        changePaperDitaPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        changePaperHtmlPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)
    }

    abstract void setupActiveItem()

    abstract void setupRetiredItem()

    abstract void setupPreparatoryItem()

    abstract void setupPreviousItems()

    protected void addWhereUsed(T currentItem, NhsDataDictionaryComponent otherItem) {
        String description = "references in description $currentItem.name"
        currentItem.addWhereUsed(otherItem, description)
    }

    protected void addChangeLog(T currentItem, NhsDDChangeLog... entries) {
        currentItem.changeLogHeaderText = "<p>Click on the links below to view the change requests this item is part of:</p>"
        currentItem.changeLogFooterText = """<p>Click <a class="- topic/xref xref" href="https://www.datadictionary.nhs.uk/archive" target="_blank" rel="external noopener">here</a> to see the Change Log Information for changes before January 2025.</p>"""

        entries.each { currentItem.changeLog.add(it) }
    }
}
