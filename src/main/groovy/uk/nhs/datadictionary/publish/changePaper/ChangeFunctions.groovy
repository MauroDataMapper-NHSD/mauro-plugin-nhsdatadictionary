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
package uk.nhs.datadictionary.publish.changePaper

import groovy.xml.MarkupBuilder
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.datadictionary.publish.structure.DiffStatus

class ChangeFunctions {
    static <T extends ChangeAware> boolean areEqual(List<T> first, List<T> second) {
        if (!first) {
            return false
        }

        if (!second) {
            return false
        }

        if (first.size() != second.size()) {
            return false
        }

        first.every { firstItem ->
            second.any { secondItem -> secondItem.discriminator == firstItem.discriminator }
        }
    }

    static <T extends ChangeAware> List<T> getDifferences(List<T> source, List<T> compare) {
        source.findAll { sourceItem ->
            !compare.find {compareItem -> compareItem.discriminator == sourceItem.discriminator }
        }
    }

    /**
     * Build a new collection of items that contains new, removed and identical items combined.
     * @param currentList The list of current items.
     * @param previousList The list of previous items to compare against.
     * @param identicalItemHandler Optional closure for handling identical items. If not provided, any identical items are
     * automatically added to the result list. Otherwise, choose how to handle them e.g. using a deeper object hierarchy.
     * The closure accepts two arguments: the current item and the previous item (if available)
     * @return A new list of combined items
     */
    static <T extends ChangeAware & DiffObjectAware<T>> List<T> buildDifferencesList(
        List<T> currentList,
        List<T> previousList,
        Closure<T> identicalItemHandler = null) {
        List<T> newList = getDifferences(currentList, previousList)
        List<T> removedList = getDifferences(previousList, currentList)

        List<T> diffList = []
        currentList.each { currentItem ->
            if (newList.any { it.discriminator == currentItem.discriminator }) {
                diffList.add(currentItem.cloneWithDiffStatus(DiffStatus.NEW))
            }
            else if (identicalItemHandler) {
                T previousItem = previousList.find {it.discriminator == currentItem.discriminator }
                T handledItem = identicalItemHandler.call(currentItem, previousItem)
                if (handledItem) {
                    // Possible change added here because it may have been computed by something else
                    diffList.add(handledItem)
                }
                else {
                    // No change
                    diffList.add(currentItem)
                }
            }
            else {
                diffList.add(currentItem)
            }
        }
        removedList.each { removedItem ->
            diffList.add(removedItem.cloneWithDiffStatus(DiffStatus.REMOVED))
        }

        diffList
    }

    // Use publish structure model instead
    @Deprecated
    static <T extends ChangeAware & NhsDataDictionaryComponent> StringWriter createUnorderedListHtml(String title, List<T> currentItems, List<T> previousItems) {
        List<T> newItems = getDifferences(currentItems, previousItems)
        List<T> removedItems = getDifferences(previousItems, currentItems)

        if (newItems.empty && removedItems.empty) {
            return null
        }

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

        markupBuilder.div {
            p {
                b title
            }
            ul {
                currentItems.each {element ->
                    if (newItems.any { it.discriminator == element.discriminator }) {
                        markupBuilder.li(class: "new") {
                            mkp.yield(element.name)
                        }
                    }
                    else {
                        markupBuilder.li {
                            mkp.yield(element.name)
                        }
                    }
                }
                removedItems.each { element ->
                    markupBuilder.li(class: "deleted") {
                        mkp.yield(element.name)
                    }
                }
            }
        }

        htmlWriter
    }
}
