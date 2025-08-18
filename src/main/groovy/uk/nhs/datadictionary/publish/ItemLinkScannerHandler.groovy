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
package uk.nhs.datadictionary.publish

import groovy.util.logging.Slf4j
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.services.DataDictionaryComponentService

interface ItemLinkScannerHandler {
    String handleItemLink(String linkHtml, String mauroPath, String text)
}

@Slf4j
class XrefIdItemLinkScannerHandler implements ItemLinkScannerHandler {
    final PathResolver<NhsDataDictionaryComponent> pathResolver

    XrefIdItemLinkScannerHandler(PathResolver<NhsDataDictionaryComponent> pathResolver) {
        this.pathResolver = pathResolver
    }

    @Override
    String handleItemLink(String linkHtml, String mauroPath, String text) {
        NhsDataDictionaryComponent component = pathResolver.get(mauroPath)
        if (!component) {
            // No replacement
            log.warn("Cannot find data dictionary component with path '${mauroPath}'")
            return null
        }

        String cleanedText = text.replaceAll("_"," ")
        String replacement = "<a class='${component.getOutputClass()}' href=\"${component.getDitaKey()}\">${cleanedText}</a>"

        replacement
    }
}

@Slf4j
class HtmlPreviewItemLinkScannerHandler implements ItemLinkScannerHandler {
    final UUID branchId
    final PathResolver<UUID> pathResolver

    HtmlPreviewItemLinkScannerHandler(UUID branchId, PathResolver<UUID> pathResolver) {
        this.branchId = branchId
        this.pathResolver = pathResolver
    }

    @Override
    String handleItemLink(String linkHtml, String mauroPath, String text) {
        try {
            UUID catalogueItemId = pathResolver.get(mauroPath)

            if (!catalogueItemId) {
                // No replacement
                log.warn("Cannot find catalogue item with path '${mauroPath}'")
                return null
            }

            String[] pathParts = mauroPath.split("\\|")
            // TODO: move calling this code to this class?
            String stereotype = DataDictionaryComponentService.getStereotypeByPath(pathParts)
            String cssClass = stereotype
            String replacement = """<a class="${cssClass}" href="#/preview/${branchId.toString()}/${stereotype}/${catalogueItemId.toString()}">${text}</a>"""

            return replacement
        }
        catch (Exception exception) {
            log.error("Error replacing link for '${mauroPath}'", exception)
            return null
        }
    }
}

@Slf4j
class UpdateWhereUsedItemLinkScannerHandler implements ItemLinkScannerHandler {
    final NhsDataDictionaryComponent sourceComponent
    final PathResolver<NhsDataDictionaryComponent> pathResolver

    UpdateWhereUsedItemLinkScannerHandler(
        NhsDataDictionaryComponent sourceComponent,
        PathResolver<NhsDataDictionaryComponent> pathResolver) {
        this.sourceComponent = sourceComponent
        this.pathResolver = pathResolver
    }

    @Override
    String handleItemLink(String linkHtml, String mauroPath, String text) {
        NhsDataDictionaryComponent component = pathResolver.get(mauroPath)
        if (!component) {
            // Nothing to do
            log.warn("Cannot find data dictionary component with path '${mauroPath}'")
            return null
        }

        if (sourceComponent && sourceComponent != component) {
            // In the description, use the source component name in it to match the current live published dictionary content
            component.whereUsed[sourceComponent] = "references in description ${component.name}".toString()
        }

        // No replacement required
        return null
    }
}