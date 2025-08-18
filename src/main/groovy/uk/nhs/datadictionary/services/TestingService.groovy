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
package uk.nhs.datadictionary.services

import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.folder.FolderService
import org.maurodata.domain.security.CatalogueUser
import uk.nhs.datadictionary.DataDictionaryImportParameters

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 17/02/2022
 */
@Transactional
@Slf4j
class TestingService {

    NhsDataDictionaryService nhsDataDictionaryService
    FolderService versionedFolderService
    FolderService folderService

    Map<String, String> buildTestData(CatalogueUser user) {
        // Ingest, finalise september
        UUID releaseId = cleanIngest(user, 'september2021.xml', 'September 2021', 'main')
        // Ingest november
        UUID novBranchId = cleanIngest(user, 'november2021.xml', 'November 2021', 'main', false)
        // Change the folder name and link to the sept release
        Folder release = versionedFolderService.get(releaseId)
        Folder novBranch = versionedFolderService.get(novBranchId)
        novBranch.label = 'NHS Data Dictionary (September 2021)'
        versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(novBranch, release, UnloggedUser.instance)
        versionedFolderService.save(novBranch, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        [releaseId  : releaseId.toString(),
         novBranchId: novBranchId.toString()]
    }

    Map<String, String> branchTestData(CatalogueUser user, String releaseId) {
        // Create a september branch
        Folder release = versionedFolderService.get(releaseId)
        UUID septBranchId = createBranch(user, release, 'september_2021')
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        [releaseId   : releaseId.toString(),
         septBranchId: septBranchId.toString()]

    }

    UUID cleanIngest(CatalogueUser user, String name, String releaseDate, String branchName, boolean finalised = true, boolean deletePrevious = false) {
        log.info('---------- Ingesting {} ----------', name)
        def xml = loadXml(name)
        assert xml
        Folder dd = nhsDataDictionaryService.ingest(user, xml, releaseDate, finalised, null, null, branchName, new DataDictionaryImportParameters(), deletePrevious)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished Ingesting {} ----------', name)
        dd.id
    }

    def loadXml(String filename) {
        Path resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources')
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        def xml = new XmlParser(false, false).parse(Files.newBufferedReader(testFilePath))
        xml
    }


    UUID createBranch(CatalogueUser user, Folder release, String branchName) {
        log.info('---------- Starting {} branch ----------', branchName)
        Folder branch = versionedFolderService.createNewBranchModelVersion(release, branchName,)
        Folder validated = folderService.validate(branch) as Folder
        assert !validated.hasErrors()
        Folder saved = versionedFolderService.save(validated, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished {} branch ----------', branchName)
        saved.id
    }


    def diff(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)

        Folder previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)

        // Load the things into memory
        //NhsDataDictionary thisDataDictionary = buildDataDictionary(versionedFolderId)
        //NhsDataDictionary previousDataDictionary = buildDataDictionary(versionedFolderId)
        //ObjectDiff objectDiff = versionedFolderService.getDiffForVersionedFolders(thisDictionary, previousVersion)

        log.info('---------- Starting merge diff ----------')
        long start = System.currentTimeMillis()
        //MergeDiff<Folder> objectDiff = versionedFolderService.getMergeDiffForVersionedFolders(thisDictionary, previousVersion)
        log.info('Merge Diff took {}', Utils.timeTaken(start))

        return objectDiff
    }
}
