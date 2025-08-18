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

import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.services.DataDictionaryComponentService

import java.util.regex.Matcher
import java.util.regex.Pattern

class ItemLinkScanner {
    // TODO: Move DataDictionaryComponentService.pattern to this class only
    static final Pattern MAURO_PATH_HTML_LINK_PATTERN = DataDictionaryComponentService.pattern

    final ItemLinkScannerHandler handler

    ItemLinkScanner(ItemLinkScannerHandler handler) {
        this.handler = handler
    }

    static ItemLinkScanner createForDitaOutput(PathResolver<NhsDataDictionaryComponent> pathResolver) {
        new ItemLinkScanner(new XrefIdItemLinkScannerHandler(pathResolver))
    }

    static ItemLinkScanner createForHtmlPreview(UUID branchId, PathResolver<UUID> pathResolver) {
        new ItemLinkScanner(new HtmlPreviewItemLinkScannerHandler(branchId, pathResolver))
    }

    String scanForItemLinks(String source) {
        if (!source) {
            return source
        }

        String updated = source
        Matcher matcher = MAURO_PATH_HTML_LINK_PATTERN.matcher(updated)
        while (matcher.find()) {
            String entireMatch = matcher.group(0)
            String mauroPathHref = matcher.group(1)
            String linkText = matcher.group(2)

            if (mauroPathHref.startsWith('http') || mauroPathHref.startsWith('mailto')) {
                // Ignore
                continue
            }

            String replacementLinkHtml = handler.handleItemLink(entireMatch, mauroPathHref, linkText)
            if (replacementLinkHtml) {
                updated = updated.replace(entireMatch, replacementLinkHtml)
            }
        }

        updated
    }
}
