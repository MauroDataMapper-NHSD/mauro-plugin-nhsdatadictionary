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
package uk.nhs.datadictionary.controllers



class UrlMappings {

    static mappings = {
        // provide plugin url mappings here

        group '/api', {
            group '/nhsdd', {
                post "/ingest"(controller: 'NhsDataDictionary', action: 'ingest')

                group '/testing', {
                    post "/buildTestData"(controller: 'testing', action: 'buildTestData')
                    post "/branchTestData"(controller: 'testing', action: 'branchTestData')
                    get '/diff'(controller: 'testing', action: 'diff')
                }

                get "/branches"(controller: 'NhsDataDictionary', action: 'branches')

                group "/$versionedFolderId", {

                    put '/newVersion'(controller: 'NhsDataDictionary', action: 'newVersion')
                    get '/statistics'(controller: 'NhsDataDictionary', action: 'statistics')
                    get '/integrityChecks'(controller: 'NhsDataDictionary', action: 'integrityChecks')


                    group '/publish', {

                        get '/website'(controller: 'NhsDataDictionary', action: 'generateWebsite')

                        get '/changePaper'(controller: 'NhsDataDictionary', action: 'generateChangePaper')

                        get '/shortDescriptions'(controller: 'NhsDataDictionary', action: 'shortDescriptions')

                        get '/iso11179'(controller: 'NhsDataDictionary', action: 'iso11179')

                        // More endpoints to control publication to OntoServer
                        get '/codeSystem/validateBundle'(controller: 'NhsDataDictionary', action: 'codeSystemValidateBundle')
                        get '/valueSet/validateBundle'(controller: 'NhsDataDictionary', action: 'valueSetValidateBundle')
                    }

                    group '/preview', {

                        get '/changePaper'(controller: 'NhsDataDictionary', action: 'previewChangePaper')

                        get '/allItemsIndex'(controller: 'NhsDataDictionary', action: 'allItemsIndex')

                        '/businessDefinitions'(resources: 'businessDefinition', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'businessDefinition', action: 'whereUsed')
                        }
                        '/elements'(resources: 'element', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'element', action: 'whereUsed')
                        }
                        '/attributes'(resources: 'attribute', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'attribute', action: 'whereUsed')
                        }
                        '/classes'(resources: 'class', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'class', action: 'whereUsed')
                        }
                        '/dataSets'(resources: 'dataSet', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'dataSet', action: 'whereUsed')
                        }
                        '/supportingInformation'(resources: 'supportingInformation', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'supportingInformation', action: 'whereUsed')
                        }
                        '/dataSetConstraints'(resources: 'dataSetConstraint', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'dataSetConstraint', action: 'whereUsed')
                        }
                        '/dataSetFolders'(resources: 'dataSetFolder', excludes: DEFAULT_EXCLUDES_AND_NO_SAVE) {
                            "/whereUsed"(controller: 'dataSetFolder', action: 'whereUsed')
                        }
                    }
                }
            }
        }
    }
}
