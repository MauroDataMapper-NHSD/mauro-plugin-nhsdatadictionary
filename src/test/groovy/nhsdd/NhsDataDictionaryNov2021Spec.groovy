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
package nhsdd

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelService
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.CodeSetService
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.terminology.TerminologyService
import spock.lang.Shared
import spock.lang.Specification
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.integritychecks.IntegrityCheck
import uk.nhs.datadictionary.services.NhsDataDictionaryService
import uk.nhs.datadictionary.services.TestingService

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * To run this against a PG db you need to alter the application.yml file.
 * The block at the end inside environments needs to be altered to
 * <pre>
 *     test:
 #        spring.flyway.enabled: false
 #        maurodatamapper:
 #            authority:
 #                name: 'Test Authority'
 #                url: 'http://localhost'
 #        database:
 #            name: 'nhsdd'
 #            creation: 'CREATE SCHEMA IF NOT EXISTS CORE\;CREATE SCHEMA IF NOT EXISTS DATAMODEL\;CREATE SCHEMA IF NOT EXISTS TERMINOLOGY'
 dataSource:
 driverClassName: org.postgresql.Driver
 dialect: org.hibernate.dialect.PostgreSQL10Dialect
 username: maurodatamapper
 password: MauroDataMapper1234
 dbCreate: none
 url: 'jdbc:postgresql://${database.host}:${database.port}/${database.name}'
 * </pre>
 *
 * You can then use {@code UUID releaseId = VersionedFolder.by().id().get()} to get a pre-loaded ingest rather than loading a clean ingest
 *
 * @since 14/12/2021
 */
@Slf4j
@MicronautTest
//@Ignore("Ingest of older version of Data Dictionary takes too long to test. Keep just in case but skip running these tests.")
class NhsDataDictionaryNov2021Spec extends Specification {

    @Shared
    Node xml

    @Inject
    NhsDataDictionaryService nhsDataDictionaryService

    TerminologyService terminologyService
    CodeSetService codeSetService
    DataModelService dataModelService
    //FolderService versionedFolderService
    //FolderService folderService
    TestingService testingService

    static final XmlParser xmlParser = new XmlParser()

    void setupSpec() {
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("november2021.xml")
        xml = xmlParser.parse(inputStream)
    }

    void 'I00 : test xml ingest of November 2021'() {

        when:
        NhsDataDictionary dataDictionary = NhsDataDictionary.buildFromXml(xml, new DataDictionaryImportParameters())

        then:

        assertEquals dataDictionary.attributes.size(), 2526
        assertEquals dataDictionary.elements.size(), 4915
        assertEquals dataDictionary.classes.size(), 363
        assertEquals dataDictionary.dataSets.size(), 261
        assertEquals dataDictionary.businessDefinitions.size(), 1230
        assertEquals dataDictionary.supportingInformation.size(), 152
        assertEquals dataDictionary.dataSetConstraints.size(), 33

    }


    void 'I01 : test xml ingest and save of November 2021'() {

        when:
        Folder dd = nhsDataDictionaryService.ingest(xml, new DataDictionaryImportParameters())

        then:
        dd
        checkNovember2021(dd, false, 75, 921, 1116, 263)
    }

    void 'I02 : test double ingest of November 2021'() {
        // This is to test that ingesting again doesnt error and also to test the batch deletion code
        given:
        setupData()
        testingService.cleanIngest(user, 'november2021.xml', 'November 2021', 'main')
        def xml = loadXml('november2021.xml')
        assert xml

        when: 'ingest again'
        log.info('---------- 2nd Ingest --------')
        // Delete old folder complete in 21 secs 547 ms with the batching
        Folder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', false, null, null, null, new DataDictionaryImportParameters())

        then:
        noExceptionThrown()
        dd
        checkNovember2021(dd, false, 75, 921, 1116, 263)
    }

    void 'F01 : Finalise Nov 2021 ingest'() {
        given:
        setupData()
        def xml = loadXml('november2021.xml')
        assert xml

        when: 'finalise'
        // finalise dictionary complete in 39 secs 787 ms
        Folder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', true, null, null, null, new DataDictionaryImportParameters())

        then:
        noExceptionThrown()
        dd
        checkNovember2021(dd, true, 75, 921, 1116, 263)
    }

    void 'B01 : Branch Nov 2021 ingest'() {
        given:
        setupData()
        //        UUID releaseId = VersionedFolder.by().id().get()
        UUID releaseId = testingService.cleanIngest(user, 'november2021.xml', 'November 2021', 'main', true, true)
        Folder release = versionedFolderService.get(releaseId)

        when:
        log.info('---------- Starting new branch ----------')
        long start = System.currentTimeMillis()
        Folder branched = versionedFolderService.createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME,
                                                                                      release, user, true)
        log.info('New branch creation took {}', Utils.timeTaken(start))

        if (branched && branched.hasErrors()) {
            //GormUtils.outputDomainErrors(messageSource, branched)
        }

        then:
        branched
        !branched.hasErrors()

        when:
        Folder validated = folderService.validate(branched) as Folder

        then:
        !validated.hasErrors()

        when:
        versionedFolderService.save(validated, validate: false, flush: true)

        then:
        noExceptionThrown()

        when:
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        Folder branch = versionedFolderService.get(branched.id)

        then:
        checkNovember2021(branch, false, 148, 1842, 2232, 524)
    }

    void 'MD01 : Merge Diff Nov 2021 ingest and branch'() {
        given:
        setupData()
        UUID releaseId = testingService.cleanIngest(user, 'november2021.xml', 'November 2021', 'main')
        Folder release = versionedFolderService.get(releaseId)
        UUID mainBranchId = testingService.createBranch(user, release, VersionAwareConstraints.DEFAULT_BRANCH_NAME)
        UUID testBranchId = testingService.createBranch(user, release, 'test')
        Folder main = versionedFolderService.get(mainBranchId)
        Folder test = versionedFolderService.get(testBranchId)

        when:
        log.info('---------- Starting merge diff ----------')
        long start = System.currentTimeMillis()
        //MergeDiff<Folder> mergeDiff = versionedFolderService.getMergeDiffForVersionedFolders(test, main)
        log.info('Merge Diff took {}', Utils.timeTaken(start))

        then:
        mergeDiff.empty
    }

    void 'MD02 : Merge Diff Nov 2021 and Sept 2021 ingest'() {
        /*
        Sept2021 (1.0.0) -> Sept2021 (september_2021) // branch of the original
                         \
                          \-> Sept2021 (main) // ingest of nov2021 file
         */
        given:
        setupData()
        // Ingest initial models and configure
        def ids = testingService.buildTestData(user)
        String releaseId = ids.releaseId
        String novBranchId = ids.novBranchId

        // Create a september branch
        ids = testingService.branchTestData(user, releaseId)
        String septBranchId = ids.septBranchId

        Folder novBranch = versionedFolderService.get(novBranchId)
        Folder septBranch = versionedFolderService.get(septBranchId)


        when:
        log.info('---------- Starting merge diff ----------')
        long start = System.currentTimeMillis()
        //MergeDiff<VersionedFolder> mergeDiff = versionedFolderService.getMergeDiffForVersionedFolders(novBranch, septBranch)
        log.info('Merge Diff took {}', Utils.timeTaken(start))
        mergeDiff.flattenedDiffs.removeIf({
            org.maurodata.domain.model.Path.PathNode last = it.fullyQualifiedPath.last()
            last.matches(new org.maurodata.domain.model.Path.PathNode('md', 'uk.nhs.datadictionary.term.publishDate', null, 'value')) ||
            last.attribute == 'modelResourceId'
        })

        then:
        !mergeDiff.empty
        if (mergeDiff.numberOfDiffs != 144) {
            writeMergeDiffOut(mergeDiff)
        }
        writeMergeDiffOut(mergeDiff)
        mergeDiff.numberOfDiffs == 144
    }

    void 'M01 : Merge Nov 2021 patches into Sept 2021 ingest'() {
        /*
        Sept2021 (1.0.0) -> Sept2021 (september_2021) // branch of the original
                         \
                          \-> Sept2021 (main) // ingest of nov2021 file
         */
        given:
        setupData()
        // Ingest initial models and configure
        def ids = testingService.buildTestData(user)
        String releaseId = ids.releaseId
        String novBranchId = ids.novBranchId

        // Create a september branch
        ids = testingService.branchTestData(user, releaseId)
        String septBranchId = ids.septBranchId

        Folder novBranch = versionedFolderService.get(novBranchId)
        Folder septBranch = versionedFolderService.get(septBranchId)

        //ObjectPatchData objectPatchData = new ObjectPatchData()
        String patchJson = new String(loadTestFile('mergePatches.json'))
        DataBindingUtils.bindObjectToInstance(objectPatchData, new JsonSlurper().parseText(patchJson))


        when:
        log.info('---------- Starting merge  ----------')
        long start = System.currentTimeMillis()
        Folder mergedFolder =
            versionedFolderService.mergeObjectPatchDataIntoVersionedFolder(objectPatchData, septBranch, novBranch, PublicAccessSecurityPolicyManager.instance)
        log.info('Merge  took {}', Utils.timeTaken(start))

        then:
        mergedFolder
    }

    void 'S01 : Obtain statistics for November 2021'() {
        given:
        setupData()
        UUID releaseId = testingService.cleanIngest(user, 'november2021.xml', 'November 2021', 'main', false)
        //        UUID releaseId = VersionedFolder.by().id().get()

        when:
        long start = System.currentTimeMillis()
        NhsDataDictionary nhsDataDictionary = nhsDataDictionaryService.buildDataDictionary(releaseId)
        String statsJson = renderStatisticsAsJson(nhsDataDictionary)
        log.info('Stats obtained in {}', Utils.timeTaken(start))
        def stats = new JsonSlurper().parseText(statsJson)

        then:
        stats
        log.info('{}', JsonOutput.prettyPrint(statsJson))
        checkStatsMapEntry(stats, 'Attributes', 2526, 0, 1192)
        checkStatsMapEntry(stats, 'Data Elements', 4915, 8, 2201)
        checkStatsMapEntry(stats, 'Classes', 363, 0, 138)
        checkStatsMapEntry(stats, 'Data Sets', 261, 0, 136)
        checkStatsMapEntry(stats, 'NHS Business Definitions', 1230, 1, 390)
        checkStatsMapEntry(stats, 'Supporting Information', 152, 0, 24)
        checkStatsMapEntry(stats, 'Data Set Constraints', 33, 0, 5)
    }

    void 'IN01 : Run integrity checks for November 2021'() {
        given:
        setupData()
        UUID releaseId = testingService.cleanIngest(user, 'november2021.xml', 'November 2021', 'main')
        //        UUID releaseId = VersionedFolder.by().id().get()

        when:
        long start = System.currentTimeMillis()
        List<IntegrityCheck> checks = nhsDataDictionaryService.integrityChecks(releaseId)
        log.info('Integrity checks obtained in {}', Utils.timeTaken(start))
        log.info('{}', checks)
        then:
        checks
        log.info('{}', checks)
    }
/*
    void writeMergeDiffOut(MergeDiff mergeDiff) {
        log.error('Diffs {}', mergeDiff.numberOfDiffs)
        String actual = renderMergeDiffAsJson(mergeDiff)
        writeFile('mergeDiff.json', actual)

    }
*/
    void outputChildFolderContents(Folder parentFolder, String variableName) {
        log.warn '\n{}', parentFolder.childFolders.collect {cf ->
            outputFolderContents(variableName, cf)
        }.join('\n')
    }

    String outputFolderContents(String parent, Folder check) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(check.id)
        List<CodeSet> codeSets = codeSetService.findAllByFolderId(check.id)
        List<DataModel> dataModels = dataModelService.findAllByFolderId(check.id)
        if (check.childFolders) {
            return "checkFolderContentsWithChildren(${parent}.childFolders.find{ it.label == '${check.label}' }, ${check.childFolders?.size() ?: 0}, " +
                   "${terminologies.size()}, ${codeSets.size()}, " +
                   "${dataModels.size()})"
        }
        "checkFolderContents(${parent}.childFolders.find{ it.label == '${check.label}' }, ${terminologies.size()}, ${codeSets.size()}, " +
        "${dataModels.size()})"
    }
/*
    String renderMergeDiffAsJson(MergeDiff mergeDiff) {
        //WritableScriptTemplate t = templateEngine.resolveTemplate(MergeDiff, Locale.default)
        def writable = t.make(mergeDiff: mergeDiff)
        def sw = new StringWriter()
        writable.writeTo(sw)
        sw.toString()
    }
*/

    String renderStatisticsAsJson(NhsDataDictionary nhsDataDictionary) {
        //WritableScriptTemplate t = templateEngine.resolveTemplate('/nhsDataDictionary/statistics.gson')
        def writable = t.make(nhsDataDictionary: nhsDataDictionary)
        def sw = new StringWriter()
        writable.writeTo(sw)
        sw.toString()
    }


    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    void writeFile(String filename, String content) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        Files.deleteIfExists(testFilePath)
        Files.write(testFilePath, content.bytes)
    }

    private void checkStatsMapEntry(Map<String, Map<String, Number>> statsMap, String name, int total, int preparatory, int retired) {
        assertEquals("Total ${name}", total, statsMap[name].Total)
        assertEquals("Preparatory ${name}", preparatory, statsMap[name].Preparatory)
        assertEquals("Retired ${name}", retired, statsMap[name].Retired)
    }

    private void checkFolderContentsWithChildren(Folder check, int childFolderCount, int terminologyCount, int codeSetCount, int dataModelCount, boolean finalised) {
        assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, terminologyCount, codeSetCount, dataModelCount, finalised)
    }

    private void checkFolderContentsWithChildrenOnly(Folder check, int childFolderCount, boolean finalised) {
        assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, 0, 0, 0, finalised)
    }

    private void checkFolderWithTerminologiesOnly(Folder check, int terminologyCount, boolean finalised) {
        checkFolderContents(check, terminologyCount, 0, 0, finalised)
    }

    private void checkFolderWithCodeSetsOnly(Folder check, int codeSetCount, boolean finalised) {
        checkFolderContents(check, 0, codeSetCount, 0, finalised)
    }

    private void checkFolderWithDataModelsOnly(Folder check, int dataModelCount, boolean finalised) {
        checkFolderContents(check, 0, 0, dataModelCount, finalised)
    }

    private void checkFolderContents(Folder check, int terminologyCount, int codeSetCount, int dataModelCount, boolean finalised) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(check.id)
        List<CodeSet> codeSets = codeSetService.findAllByFolderId(check.id)
        List<DataModel> dataModels = dataModelService.findAllByFolderId(check.id)

        checkModels(check.label, 'Terminologies', terminologyCount, finalised, terminologies)
        checkModels(check.label, 'CodeSets', codeSetCount, finalised, codeSets)
        checkModels(check.label, 'DataModels', dataModelCount, finalised, dataModels)
    }

    void checkModels(String label, String name, int count, boolean finalised, List<Model> models) {
        if (count) {
            assertEquals "${name} in ${label}", count, models.size()
            assertTrue "${name} in ${label} are finalised ${finalised}", models.every {it.finalised == finalised}
        } else {
            assertTrue("No ${name} in ${label}", models.isEmpty())
        }
    }

    void checkModelItemIndexes(Collection<ModelItem> modelItems, String path) {
        if (!modelItems) return
        modelItems.sort().eachWithIndex {mi, i ->
            assertEquals("${path} >> ${mi.domainType} ${mi.label} idx", i, mi.idx)
        }
        if (modelItems.first() instanceof DataClass) {
            (modelItems as Collection<DataClass>).each {
                checkModelItemIndexes(it.dataClasses, "${path}|${it.label}")
                checkModelItemIndexes(it.dataElements, "${path}|${it.label}")
            }
        }
    }

    void checkPaths(List<AdministeredItem> mdmDomains) {
        mdmDomains.each {
            Path uncheckedPath = it.getUncheckedPath()
            Path checkedPath = it.getPath()
            assertEquals('Checked path matches unchecked path', uncheckedPath, checkedPath)
        }
    }

    void checkNovember2021(Folder nhsddToTest, boolean finalised, int totalFolders = 0, int totalTerminologies = 0, int totalCodeSets = 0, int dataModels = 0) {

        // Clear out the whole session to ensure absolutely no corruption of unsaved or unflushed data
        sessionFactory.currentSession.clear()

        // Check the stored content matches up with what we expect for facets, paths and BTs
        assertTrue 'All MD have ids', Metadata.list().every {it.multiFacetAwareItemId}
        assertTrue 'All BT have ids', BreadcrumbTree.list().every {it.domainId}
        BreadcrumbTree.list().each {
            String uncheckedTreeString = it.treeString
            it.checkTree()
            if (it.isDirty('treeString')) log.warn('[{}] does not match [{}]', uncheckedTreeString, it.treeString)
        }
        assertTrue 'All BT have correct treestring', BreadcrumbTree.list().every {
            !it.isDirty('treeString')
        }

        checkPaths(Folder.list())
        checkPaths(Terminology.list())
        checkPaths(CodeSet.list())
        checkPaths(DataModel.list())
        checkPaths(Term.list())
        checkPaths(DataClass.list())
        checkPaths(DataElement.list())
        checkPaths(DataType.list())
        checkPaths(TermRelationshipType.list())
        checkPaths(TermRelationship.list())

        Folder nhsdd = versionedFolderService.get(nhsddToTest.id)

        assertEquals 'NHSDD Folder finalisation', finalised, nhsdd.finalised

        if (totalFolders) {

            assertEquals('Total Folders', totalFolders, folderService.count())
            assertEquals('Total Terminologies', totalTerminologies, terminologyService.count())
            assertEquals('Total CodeSets', totalCodeSets, codeSetService.count())
            assertEquals('Total DataModels', dataModels, dataModelService.count())
        } else {
            log.warn('Folders {}, Terminologies {} CodeSets {} DataModels {}',
                     folderService.count(),
                     terminologyService.count(),
                     codeSetService.count(),
                     dataModelService.count())
        }

        checkFolderContentsWithChildren(nhsdd, 3, 3, 0, 2, finalised)

        DataModel classesDataModel = dataModelService.findByLabel(NhsDataDictionary.CLASSES_MODEL_NAME)
        DataModel elementsDataModel = dataModelService.findByLabel(NhsDataDictionary.ELEMENTS_MODEL_NAME)
        assertNotNull(NhsDataDictionary.CLASSES_MODEL_NAME, classesDataModel)
        assertNotNull(NhsDataDictionary.ELEMENTS_MODEL_NAME, elementsDataModel)
        assertEquals("${NhsDataDictionary.CLASSES_MODEL_NAME} dataclasses", 364, classesDataModel.dataClasses.size())
        //assertEquals("${NhsDataDictionary.ELEMENTS_MODEL_NAME} child dataclasses", 3, coreDataModel.childDataClasses.size())
        checkModelItemIndexes(classesDataModel.childDataClasses, NhsDataDictionary.CLASSES_MODEL_NAME)
        checkModelItemIndexes(elementsDataModel.childDataClasses, NhsDataDictionary.ELEMENTS_MODEL_NAME)

        // 'direct children'
        //        outputChildFolderContents(dd, 'dd')
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Attribute Terminologies'}, 24, finalised)
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Data Element CodeSets'}, 24, finalised)
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Data Sets'}, 7, finalised)


        Folder dataSets = nhsdd.childFolders.find {it.label == 'Data Sets'}
        Folder attributes = nhsdd.childFolders.find {it.label == 'Attribute Terminologies'}
        Folder elements = nhsdd.childFolders.find {it.label == 'Data Element CodeSets'}

        // 'children of attributes'
        //        outputChildFolderContents(attributes, 'attributes')
        //        outputChildFolderContents(elements, 'elements')
        //        outputChildFolderContents(dataSets, 'dataSets')
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'A'}, 67, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'B'}, 32, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'C'}, 130, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'D'}, 31, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'E'}, 46, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'F'}, 24, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'G'}, 12, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'H'}, 18, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'I'}, 28, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'J'}, 7, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'K'}, 1, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'L'}, 29, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'M'}, 66, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'N'}, 33, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'O'}, 25, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'P'}, 138, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Q'}, 4, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'R'}, 66, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'S'}, 88, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'T'}, 36, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'U'}, 12, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'V'}, 9, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'W'}, 15, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Y'}, 1, finalised)

        // 'children of elements'
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'A'}, 74, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'B'}, 49, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'C'}, 169, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'D'}, 37, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'E'}, 52, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'F'}, 25, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'G'}, 14, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'H'}, 18, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'I'}, 35, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'J'}, 6, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'K'}, 1, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'L'}, 30, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'M'}, 81, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'N'}, 49, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'O'}, 35, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'P'}, 163, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Q'}, 5, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'R'}, 74, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'S'}, 107, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'T'}, 48, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'U'}, 14, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'V'}, 11, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'W'}, 18, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Y'}, 1, finalised)

        // 'children of datasets'
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Administrative Data Sets'}, 2, finalised)
        //checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-2'}, 47, finalised)
        //checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-3'}, 15, finalised)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Central Return Data Sets'}, 6, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Content'}, 1, 0, 0, 1, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Data Sets'}, 2, 0, 0, 13, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Supporting Data Sets'}, 1, 0, 0, 8, finalised)
        checkFolderContentsWithChildrenOnly(dataSets.childFolders.find {it.label == 'Retired'}, 9, finalised)

        Folder clinicalContent = dataSets.childFolders.find {it.label == 'Clinical Content'}
        Folder clinicalDataSets = dataSets.childFolders.find {it.label == 'Clinical Data Sets'}
        Folder supportingDataSets = dataSets.childFolders.find {it.label == 'Supporting Data Sets'}
        Folder retired = dataSets.childFolders.find {it.label == 'Retired'}

        // 'children if clinical content'
        //        outputChildFolderContents(clinicalContent, 'clinicalContent')
        //        outputChildFolderContents(clinicalDataSets, 'clinicalDataSets')
        //        outputChildFolderContents(supportingDataSets, 'supportingDataSets')
        //        outputChildFolderContents(retired, 'retired')
        checkFolderContents(clinicalContent.childFolders.find {it.label == 'National Joint Registry Data Set'}, 0, 0, 6, finalised)

        // 'children of clinical datasets'
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'COSDS'}, 0, 0, 15, finalised)
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'National Neonatal Data Set'}, 0, 0, 2, finalised)

        // 'children of supporting datasets'
        checkFolderContents(supportingDataSets.childFolders.find {it.label == 'PLICS Data Set'}, 0, 0, 10, finalised)

        // 'children of retired'
        //checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-1'}, 27, finalised)
        //checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-2'}, 1, finalised)
        //checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6 Old Layout'}, 27, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Central Returns Data Sets'}, 29, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Clinical Content'}, 2, finalised)
        checkFolderContentsWithChildren(retired.childFolders.find {it.label == 'Clinical Data Sets'}, 1, 0, 0, 13, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Commissioning Data Set V5'}, 26, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Supporting Data Sets'}, 1, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS Supporting Information'}, 2, finalised)


        Folder retiredClinicalDataSets = retired.childFolders.find {it.label == 'Clinical Data Sets'}

        // 'children of retired clinical datasets'
        //        outputChildFolderContents(retiredClinicalDataSets, 'retiredClinicalDataSets')
        checkFolderWithDataModelsOnly(retiredClinicalDataSets.childFolders.find {it.label == 'National Renal Data Set'}, 8, finalised)
    }
}
