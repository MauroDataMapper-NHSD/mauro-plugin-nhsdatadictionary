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
package uk.nhs.datadictionary.integritychecks

import groovy.util.logging.Slf4j
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class InternalLinksInDescriptions implements IntegrityCheck {
    String name = "Internal Links in Descriptions"

    String description = "Check that all internal links to other dictionary items are valid"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {
        List<IntegrityCheckError> foundErrors = []

        dataDictionary.allComponents.forEach {component ->
            IntegrityCheckError integrityCheckError = checkDefinitionInternalLinksAreValid(dataDictionary, component)
            if (integrityCheckError) {
                foundErrors.add(integrityCheckError)
            }
        }

        errors = foundErrors

        errors
    }

    static IntegrityCheckError checkDefinitionInternalLinksAreValid(
        NhsDataDictionary dataDictionary,
        NhsDataDictionaryComponent component) {
        if (!component.definition || component.definition.length() == 0) {
            // Ignore this component
            return null
        }

        log.debug("Checking $component.catalogueItemDomainTypeAsString '$component.name' [$component.catalogueItemIdAsString] for internal links")
        Set<String> paths = getMauroPathsFromHtml(component.definition)

        List<String> invalidLinks = []
        paths.forEach { path ->
            if (!dataDictionary.internalLinks.containsKey(path)) {
                log.warn("$component.catalogueItemDomainTypeAsString '$component.name' [$component.catalogueItemIdAsString] references '$path' which does not exist")
                // TODO?
                //Path pathObject = Path.from(path)
                String label = pathObject.last()?.identifier ?: "Unknown"
                String detail = "Link for '${label}' was not found"
                invalidLinks.add(detail)
            }
        }

        if (invalidLinks.empty) {
            return null
        }

        return new IntegrityCheckError(component, invalidLinks)
    }

    private static Set<String> getMauroPathsFromHtml(String source) {
        Pattern pattern = ~/<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)\1/
        Matcher matcher = pattern.matcher(source)

        Set<String> mauroPaths = []

        for (index in 0..<matcher.count) {
            // The 3rd group in the regex match will be the href value
            String hrefValue = matcher[index][2]

            // Assume that non-valid URLs are actually Mauro paths
            if (!hrefValue.startsWith("http:") && !hrefValue.startsWith("https:") && !hrefValue.startsWith("mailto:")) {
                mauroPaths.add(hrefValue)
            }
        }

        mauroPaths
    }
}
