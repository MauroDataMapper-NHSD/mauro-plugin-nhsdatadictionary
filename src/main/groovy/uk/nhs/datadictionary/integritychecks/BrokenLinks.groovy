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
import jakarta.inject.Singleton
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Singleton
@Slf4j
class BrokenLinks implements IntegrityCheck {

    String name = "Broken Links in description"

    String description = "Check that all external links in descriptions lead to a valid web page"

    static Pattern pattern = Pattern.compile("<a[\\s]*href=\"(http[^\"]*)\"[^>]*>([^<]*)</a>")

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {
        Map<String, List<NhsDataDictionaryComponent>> linkComponentMap = [:]
        Map<NhsDataDictionaryComponent, List<String>> errorComponents = Collections.synchronizedMap(new HashMap<NhsDataDictionaryComponent, List<String>>())
        dataDictionary.allComponents.
            findAll {!it.isRetired() && it.description }.
            each {component ->
                Matcher matcher = pattern.matcher(component.description)
                while(matcher.find()) {
                    List<NhsDataDictionaryComponent> components = linkComponentMap[matcher.group(1)]
                    if(components) {
                        components.add(component)
                    } else {
                        linkComponentMap[matcher.group(1)] = [component]
                    }
                }
            }
        List<Thread> threads = []

        linkComponentMap.each {link, componentList ->
            threads.add(Thread.start {
                if (!isValidLink(link)) {
                    componentList.each {
                        errorComponents.put(it, errorComponents.getOrDefault(it, []) + [link])
                    }
                }
            })
        }
        threads.each { it.join() }

        errorComponents.collect {component, links ->
            new IntegrityCheckError(component, links)
        }
    }

    static boolean isValidLink(String link) {
        int responseCode = 0
        try {
            def conn = URI.create(link).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = 'HEAD'
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.instanceFollowRedirects = true
            conn.connect()
            responseCode = conn.responseCode
            if(responseCode == 403) { // Some sites - e.g. the NHS Data Dictionary (!) forbid head requests, so we could try a 'GET' instead
                def conn2 = URI.create(link).toURL().openConnection() as HttpURLConnection
                conn2.requestMethod = 'GET'
                conn2.connectTimeout = 5000
                conn2.readTimeout = 5000
                conn2.instanceFollowRedirects = true
                conn2.connect()
                responseCode = conn.responseCode
            }
            return responseCode in 200..399
        } catch (Exception ignored) {
            log.debug("Url: '$link' returned a response code of $responseCode")
            return false
        }
    }

}
