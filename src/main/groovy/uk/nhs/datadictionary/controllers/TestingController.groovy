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

import groovy.util.logging.Slf4j
import io.micronaut.transaction.annotation.Transactional
import uk.nhs.datadictionary.services.TestingService

/**
 * @since 17/02/2022
 */
@Slf4j
@Transactional
class TestingController {
    TestingService testingService


    def buildTestData() {
        def ids = testingService.buildTestData(currentUser)
        respond(ids)
    }

    def branchTestData() {
        def ids = testingService.branchTestData(currentUser, params.releaseId)
        respond(ids)
    }


    def diff() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond(testingService.diff(versionedFolderId))

    }

}
