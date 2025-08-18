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

import org.maurodata.iso11179.domain.MetadataBundle

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.io.FileUtils
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelType
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.terminology.Terminology
import org.maurodata.exception.MauroApplicationException
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBranch
import uk.nhs.datadictionary.NhsDDClassRelationship
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.fhir.FhirBundle
import uk.nhs.datadictionary.fhir.FhirEntry
import uk.nhs.datadictionary.integritychecks.IntegrityCheck
import uk.nhs.datadictionary.publish.MauroCatalogueItemPathResolver
import uk.nhs.datadictionary.publish.changePaper.ChangePaperPreview
import uk.nhs.datadictionary.services.profiles.DDWorkItemProfileProviderService
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@Singleton
class NhsDataDictionaryService {

    static final Map<String, String> KNOWN_KEYS = [
        (API_PROPERTY_RETIRED_TEMPLATE): '<p>This item has been retired from the NHS Data Model and Dictionary.</p>' +
                            '<p>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</p>' +
                           '<p>Access to the last live version of this item can be obtained by emailing <a href=\"mailto:information.standards@nhs' +
                           '.net\">information.standards@nhs.net</a> with "NHS Data Model and Dictionary - Archive Request" in the email subject ' +
                            'line.</p>',
        (API_PROPERTY_PREPARATORY_TEMPLATE): '<p><b>This item is being used for development purposes and has not yet been approved.</b></p>',
        (API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL): "https://mauro.dataproducts.nhs.uk/changerequest/$NhsDataDictionary.CHANGE_REQUEST_NUMBER_TOKEN",
        (API_PROPERTY_CHANGE_LOG_HEADER_TEXT): "<p>Click on the links below to view the change requests this item is part of:</p>",
        (API_PROPERTY_CHANGE_LOG_FOOTER_TEXT): "<p>Click <a href='https://www.datadictionary.nhs.uk/archive'>here</a> to see the Change Log Information for changes before January 2025.</p>"
    ]

    static final String NHSDD_PROPERTY_CATEGORY = 'NHS Data Dictionary'
    static final String API_PROPERTY_RETIRED_TEMPLATE = 'retired.template'
    static final String API_PROPERTY_PREPARATORY_TEMPLATE = 'preparatory.template'
    static final String API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL = 'changelog.url.changerequest'
    static final String API_PROPERTY_CHANGE_LOG_HEADER_TEXT = 'changelog.headertext'
    static final String API_PROPERTY_CHANGE_LOG_FOOTER_TEXT = 'changelog.footertext'

    @Inject
    ItemCacheableRepository.ApiPropertyCacheableRepository apiPropertyCacheableRepository

    @Inject
    FolderCacheableRepository folderCacheableRepository

    DataSetService dataSetService
    ClassService classService
    ElementService elementService
    AttributeService attributeService

    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    DataSetConstraintService dataSetConstraintService
    DataSetFolderService dataSetFolderService

    DDWorkItemProfileProviderService ddWorkItemProfileProviderService

    UUID newVersion(CatalogueUser currentUser, UUID versionedFolderId) {
        Folder original = versionedFolderService.get(versionedFolderId)
        // load the VersionedFolder into memory...

        original.metadata.size()
        original.annotations.each {an ->
            an.childAnnotations.size()
        }
        original.childFolders.each { cf ->
            cf.childFolders.each { cf2 ->
                cf2.childFolders.size()
                cf2.metadata.size()
                cf2.rules.size()
                cf2.semanticLinks.size()
                cf2.annotations.size()
                cf2.referenceFiles.size()
            }
            cf.annotations.size()
            cf.metadata.size()
            cf.rules.size()
            cf.semanticLinks.size()
            cf.referenceFiles.size()


        }
        original.rules.size()
        original.referenceFiles.size()
        original.semanticLinks.size()
        original.versionLinks.size()
        // discard the whole structure

        original.id = null
        original.discard()


        original.metadata = []
        original.annotations = []
        original.rules = []
        original.metadata = []
        original.semanticLinks = []
        original.referenceFiles = []
        original.versionLinks = []

        Set<Folder> originalChildFolders = []
        originalChildFolders.addAll(original.childFolders)

        original.childFolders = []
        original.addToChildFolders(originalChildFolders.collect { cf ->
            cf.id = null
            cf.discard()
            cf.annotations = []
            cf.rules = []
            cf.metadata = []
            cf.semanticLinks = []
            cf.referenceFiles = []

            Set<Folder> cfChildFolders = []
            cfChildFolders.addAll(cf.childFolders)

            cf.childFolders = []
            cfChildFolders.each { cf2 ->
                cf2.id = null
                cf2.discard()

                cf2.childFolders = []
                cf2.metadata = []
                cf2.annotations = []
                cf2.rules = []
                cf2.semanticLinks = []
                cf2.referenceFiles = []
                cf.addToChildFolders(cf2)
            }
            cf
        })
        sessionFactory.currentSession.flush()
        // Now save it again as a new thing


        original.branchName = "main"
        original.modelVersion = null
        original.finalised = false

        //original.validate()
        //original.annotations.clear()
        //original.childFolders.clear()

        original.annotations = []
        original.rules = []

        //original.save(flush: true)
        versionedFolderService.saveFolderHierarchy(original)

        //sessionFactory.currentSession.flush()
        //sessionFactory.currentSession.clear()
        Folder old = versionedFolderService.get(versionedFolderId)
        versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(original, old, currentUser)
        original.versionLinks.each {
            it.save()
        }

        //versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(newCopy, original, currentUser)
        //versionedFolderService.saveFolderHierarchy(newCopy)

        /*
        VersionedFolder originalVersionedFolder = versionedFolderService.get(versionedFolderId)
        versionedFolder = versionedFolderService.get(versionedFolder.id)

        versionedFolder.label = originalVersionedFolder.label
        System.err.println(versionedFolder.validate())
        versionedFolder.save()
        */
        return original.id
    }

    def branches(/*UserSecurityPolicyManager userSecurityPolicyManager */) {
        List<Folder> versionedFolders = VersionedFolder.findAll().findAll {
            it.label.startsWith("NHS Data Dictionary")
        }
        if(versionedFolders.size() == 0) {
            return []
        }

        Folder oldestAncestor = versionedFolderService.findOldestAncestor(versionedFolders[0])

        // List<VersionTreeModel>
        Map versionTreeModelList = versionedFolderService.buildModelVersionTree(
            oldestAncestor,
            null,
            null,
            true,
            false,
            userSecurityPolicyManager)
        return versionTreeModelList
    }

    List<IntegrityCheck> integrityChecks(UUID versionedFolderId) {

        log.error("Building data dictionary...")
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        log.error("Built data dictionary...")

        List<Class<IntegrityCheck>> integrityCheckClasses = [
            InternalLinksInDescriptions,
            AllClassesHaveRelationships,
            ClassLinkedToRetiredAttribute,
            AttributesLinkedToAClass,
            ElementsLinkedToAnAttribute,
            DataSetsHaveAnOverview,
            DataSetsIncludePreparatoryItem,
            DataSetsIncludeRetiredItem,
            AllItemsHaveShortDescription,
            AllItemsHaveAlias,
            AllItemsAreWithinValidDateRange,
            ReusedItemNames//,
            //BrokenLinks
        ]

        List<IntegrityCheck> integrityChecks = integrityCheckClasses.collect {checkClass ->
            IntegrityCheck integrityCheck = checkClass.getDeclaredConstructor().newInstance()
            integrityCheck.runCheck(dataDictionary)
            integrityCheck.sortErrors()
            integrityCheck
        }

        return integrityChecks
    }


    NhsDataDictionary buildDataDictionary(UUID versionedFolderId) {
        log.debug("Building Data Dictionary...")
        long totalStart = System.currentTimeMillis()
        NhsDataDictionary dataDictionary = newDataDictionary()
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        buildWorkItemDetails(dataDictionary.containingVersionedFolder, dataDictionary)

        log.debug('Starting {}', Utils.timeTaken(totalStart))


        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)

        Terminology busDefTerminology = terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
        Terminology supDefTerminology = terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
        Terminology dataSetConstraintsTerminology = terminologies.find {it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}

        log.debug('Got terminologies {}', Utils.timeTaken(totalStart))


        Folder dataSetsFolder = dataDictionary.containingVersionedFolder.childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}

        DataModel classesModel = getClassesModel(versionedFolderId)
        DataModel elementsModel = getElementsModel(versionedFolderId)

        log.debug('Got models {}', Utils.timeTaken(totalStart))


        if(classesModel) {
            addAttributesToDictionary(classesModel, dataDictionary)
            log.info('Added to dictionary - attributes... {}', Utils.timeTaken(totalStart))
            addClassesToDictionary(classesModel, dataDictionary)
            log.info('Added to dictionary - classes... {}', Utils.timeTaken(totalStart))
        } else {
            log.error("No classes model found")
        }
        if(elementsModel) {
            addElementsToDictionary(elementsModel, dataDictionary)
            log.info('Added to dictionary - elements... {}', Utils.timeTaken(totalStart))
        } else {
            log.error("No elements model found")
        }
        if(dataSetsFolder) {
            addDataSetFoldersToDictionary(dataSetsFolder, dataDictionary)
            log.info('Added to dictionary - folders... {}', Utils.timeTaken(totalStart))
            addDataSetsToDictionary(dataSetsFolder, dataDictionary)
            log.info('Added to dictionary - data sets... {}', Utils.timeTaken(totalStart))
        } else {
            log.error("No datasets folder found")
        }
        if(busDefTerminology) {
            addBusDefsToDictionary(busDefTerminology, dataDictionary)
            log.info('Added to dictionary - bus defs... {}', Utils.timeTaken(totalStart))
        } else {
            log.error("No business definitions terminology found")
        }
        if(supDefTerminology) {
            addSupDefsToDictionary(supDefTerminology, dataDictionary)
            log.debug('Added to dictionary - sup defs... {}', Utils.timeTaken(totalStart))
        } else {
            log.info("No supporting definitions terminology found")
        }
        if(dataSetConstraintsTerminology) {
            addDataSetConstraintsToDictionary(dataSetConstraintsTerminology, dataDictionary)
            log.info('Added to dictionary - data set constraints... {}', Utils.timeTaken(totalStart))
        } else {
            log.error("No dataset constraints terminology found")
        }

        log.debug('Added to dictionary... {}', Utils.timeTaken(totalStart))

        dataDictionary.buildInternalLinks()

        log.debug('Data Dictionary built in {}', Utils.timeTaken(totalStart))

        return dataDictionary
    }

    Terminology getBusinessDefinitionTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getSupportingDefinitionTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getDataSetConstraintTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}
    }

    DataModel getElementsModel(UUID versionedFolderId) {
        List<DataModel> dataModels = dataModelService.findAllByFolderId(versionedFolderId)
        dataModels.find {it.label == NhsDataDictionary.ELEMENTS_MODEL_NAME}
    }

    DataModel getClassesModel(UUID versionedFolderId) {
        List<DataModel> dataModels = dataModelService.findAllByFolderId(versionedFolderId)
        dataModels.find {it.label == NhsDataDictionary.CLASSES_MODEL_NAME}
    }

    Folder getDataSetsFolder(UUID versionedFolderId) {
        List<Folder> childFolders = folderService.findAllByParentId(versionedFolderId)
        childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}
    }

    void addAttributesToDictionary(DataModel classesModel, NhsDataDictionary dataDictionary) {
        Set<DataElement> attributeElements = classesModel.getAllDataElements().findAll {
            !(it.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE)
        }
        dataDictionary.attributes = attributeService.collectNhsDataDictionaryComponents(attributeElements, dataDictionary)
        dataDictionary.attributes.values().each { ddAttribute ->
            dataDictionary.attributesByCatalogueId[ddAttribute.getCatalogueItem().id] = ddAttribute
        }
    }

    void addElementsToDictionary(DataModel elementsModel, NhsDataDictionary dataDictionary) {
        Set<DataElement> elementElements = elementsModel.getAllDataElements()
        List<Metadata> elementMetadata = Metadata.byMultiFacetAwareItemIdInList(elementElements.collect {it.id} as List).list()
        elementMetadata.each { metadata ->
            if(dataDictionary.elementsMetadata[metadata.multiFacetAwareItemId]) {
                dataDictionary.elementsMetadata[metadata.multiFacetAwareItemId].add(metadata)
            } else {
                dataDictionary.elementsMetadata[metadata.multiFacetAwareItemId] = [metadata]
            }
        }
        dataDictionary.elements = elementService.collectNhsDataDictionaryComponents(elementElements, dataDictionary, dataDictionary.elementsMetadata)
        dataDictionary.elements.values().each { ddElement ->
            dataDictionary.elementsByCatalogueId[ddElement.getCatalogueItem().id] = ddElement
        }
    }

    void addClassesToDictionary(DataModel classesModel, NhsDataDictionary dataDictionary) {
        //DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}
        //DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
        Set<DataClass> classClasses = classesModel.dataClasses.collect() as Set
        classClasses.addAll(classClasses.find {it.label == "Retired"}.dataClasses)
        classClasses.removeAll {it.label == "Retired"}
        dataDictionary.classes = classService.collectNhsDataDictionaryComponents(classClasses, dataDictionary)
        dataDictionary.classes.values().each { ddClass ->
            dataDictionary.classesByCatalogueId[ddClass.getCatalogueItem().id] = ddClass
        }

        // Now link associations
        dataDictionary.classes.values().each { dataClass ->
            ((DataClass)dataClass.catalogueItem).dataElements.each { dataElement ->
                if(dataElement.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE) {
                    DataClass referencedClass = ((DataType)dataElement.dataType).referenceClass
                    dataClass.classRelationships.add(new NhsDDClassRelationship(dataElement, dataDictionary.classes[referencedClass.label]))
                } else {
                    NhsDDAttribute attribute = dataDictionary.attributesByCatalogueId[dataElement.id]
                    if (attribute) {
                        attribute.parentClass = dataClass
                        if (attribute.otherProperties['isKey']) {
                            dataClass.keyAttributes.add(attribute)
                        } else {
                            dataClass.otherAttributes.add(attribute)
                        }
                    }
                    else {
                        log.warn("Cannot find link association for NhsDDAttribute based on Mauro data element '$dataElement.label' [$dataElement.id]")
                    }
                }
            }
            ((DataClass)dataClass.catalogueItem).getExtendedDataClasses().each { extendedDataClass ->
                dataClass.extendsClasses.add(dataDictionary.classesByCatalogueId[extendedDataClass.id])
            }

            // TODO:  Sort key and non-key attributes here...
            //((DataClass)dataClass.catalogueItem).dataElements.each { dataElement ->
            //}
        }

    }


    void addDataSetsToDictionary(Folder dataSetsFolder, NhsDataDictionary dataDictionary) {
        Map<List<String>, Set<DataModel>> dataSetModelMap = dataSetService.getAllDataSets([], dataSetsFolder)
        dataSetModelMap.each {path, dataModels ->
            dataModels.each { dataModel ->
                List<UUID> allIds = []
                allIds.addAll(dataModel.getAllDataElements().collect{it.id})
                allIds.addAll(dataModel.getDataClasses().collect {it.id})
                List<Metadata> dataSetsMetadata = Metadata.byMultiFacetAwareItemIdInList(allIds).list()
                dataSetsMetadata.each { metadata ->
                    if(dataDictionary.dataSetsMetadata[metadata.multiFacetAwareItemId]) {
                        dataDictionary.dataSetsMetadata[metadata.multiFacetAwareItemId].add(metadata)
                    } else {
                        dataDictionary.dataSetsMetadata[metadata.multiFacetAwareItemId] = [metadata]
                    }
                }

                NhsDDDataSet dataSet = dataSetService.getNhsDataDictionaryComponentFromCatalogueItem(dataModel, dataDictionary, null)
                dataSet.path.addAll(path)
                dataDictionary.dataSets[dataModel.label] = dataSet
                List<String> folderPath = []
                folderPath.addAll(path)
                String parentFolderName = folderPath.removeLast()
                NhsDDDataSetFolder folder = dataDictionary.dataSetFolders[folderPath].find {it.name == parentFolderName }
                folder.dataSets[dataSet.name] = dataSet
            }
        }
    }

    void addDataSetFoldersToDictionary(Folder dataSetsFolder, NhsDataDictionary dataDictionary) {
        Map<List<String>, Set<Folder>> dataSetFolders = dataSetFolderService.getAllFolders([], dataSetsFolder)

        dataSetFolders.each { path, folders ->
            folders.each {folder ->
                NhsDDDataSetFolder dataSetFolder = dataSetFolderService.getNhsDataDictionaryComponentFromCatalogueItem(path, folder, dataDictionary)
                if(dataDictionary.dataSetFolders[path]) {
                    dataDictionary.dataSetFolders[path].add(dataSetFolder)
                } else {
                    dataDictionary.dataSetFolders[path] = [dataSetFolder]
                }
            }
        }

        dataDictionary.dataSetFolders.each { path, dataSetFoldersAtPath ->
            dataSetFoldersAtPath.each { dataSetFolder ->
                List<String> childPath = []
                childPath.addAll(path)
                childPath.add(dataSetFolder.name)

                dataDictionary.dataSetFolders[childPath].each {childDataSetFolder ->
                    dataSetFolder.childFolders[childDataSetFolder.name] = childDataSetFolder
                }
            }

        }
    }


    void addBusDefsToDictionary(Terminology busDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.businessDefinitions = businessDefinitionService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(busDefsTerminology.id), dataDictionary)
    }

    void addSupDefsToDictionary(Terminology supDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.supportingInformation = supportingInformationService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(supDefsTerminology.id), dataDictionary)
    }

    void addDataSetConstraintsToDictionary(Terminology dataSetConstraintsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.dataSetConstraints =
            dataSetConstraintService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(dataSetConstraintsTerminology.id),
                                                                  dataDictionary)
    }

/*
    Set<DataClass> getDataClasses(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)

        DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}

        Set<DataClass> classClasses = []
        classClasses.addAll(classesClass.dataClasses.findAll {it.label != "Retired"})
        if (includeRetired) {
            DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
            classClasses.addAll(retiredClassesClass.dataClasses)
        }

        return classClasses

    }

    Set<DataElement> getDataElements(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.ELEMENTS_MODEL_NAME)

        return coreModel.getAllDataElements().findAll{
            includeRetired || it.parent.label == "Retired"
        }
    }

    Set<DataElement> getAttributes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CLASSES_MODEL_NAME)

        return coreModel.getAllDataElements().findAll{
            includeRetired || it.parent.label == "Retired"
        }

    }
*/

    List<StereotypedCatalogueItem> allItemsIndex(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

        List<StereotypedCatalogueItem> allItems = []

        allItems.addAll(dataDictionary.dataSets.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.classes.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.elements.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.attributes.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.businessDefinitions.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.supportingInformation.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.dataSetConstraints.values().collect {
            new StereotypedCatalogueItem(it)
        })
        return allItems.sort {it.label.toLowerCase()}
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    private static <T> Set<T> findDuplicates(Collection<T> collection) {

        Set<T> duplicates = new HashSet<>(1000);
        Set<T> uniques = new HashSet<>();

        for (T t : collection) {
            if (!uniques.add(t)) {
                duplicates.add(t);
            }
        }

        return duplicates;
    }


    Folder ingest(def xml, DataDictionaryImportParameters parameters) {

        NhsDataDictionary nhsDataDictionary = NhsDataDictionary.buildFromXml(xml, parameters)

        nhsDataDictionary.branchName = parameters.newBranchName ?: "main"
        log.info('Ingesting new NHSDD with release date {}, finalise {}, folderVersionNo {}, prevVersion {}', parameters.releaseDate, parameters.finalised, parameters.folderVersionNo, parameters.prevVersion)
        Instant startTime = Instant.now()
        Instant originalStartTime = Instant.now()
        String dictionaryFolderName = "NHS Data Dictionary"
        if (parameters.deletePrevious || (!parameters.finalised && !parameters.folderVersionNo && !parameters.prevVersion)) {
            deleteOriginalFolder(dictionaryFolderName)
            log.info("Delete old folder complete in ${Duration.between(startTime, Instant.now()).toString()}")
            startTime = Instant.now()
        }

        Folder dictionaryFolder = new Folder(label: dictionaryFolderName, branchName: nhsDataDictionary.branchName)

        defaultProfileMetadata().each { metadata ->
            dictionaryFolder.metadata.add(metadata)
        }

        /*
        if (!folderService.validate(dictionaryFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dictionaryFolder.errors)
        }
        versionedFolderService.save(dictionaryFolder)
        */

        if (parameters.prevVersion) {
            Folder prevDictionaryVersion = folderCacheableRepository.readById(parameters.prevVersion)
            dictionaryFolder.versionLinks.add(new VersionLink(
                versionLinkType: 'NEW_FORK_OF',
                targetModel: prevDictionaryVersion
            ))
            //versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(dictionaryFolder, prevDictionaryVersion)

        }

        Map<String, Terminology> attributeTerminologiesByName = [:]
        Map<String, DataClass> attributeClassesByUin = [:]
        Set<String> attributeUinIsKey = []

        if(parameters.publishAttributes || parameters.publishClasses) {

            DataModel classesDataModel =
                    new DataModel(label: NhsDataDictionary.CLASSES_MODEL_NAME,
                            description: "NHS Data Dictionary Data Model (Classes and Attributes)",
                            dataModelType: DataModelType.DATA_STANDARD,
                            folder: dictionaryFolder,
                            branchName: nhsDataDictionary.branchName)

            startTime = Instant.now()
            classService.persistClasses(nhsDataDictionary, classesDataModel, currentUser.emailAddress, attributeClassesByUin, attributeUinIsKey)
            log.info('ClassService persisted in {}', Utils.timeTaken(startTime))

            if(publishOptions.publishAttributes) {
                startTime = System.currentTimeMillis()
                attributeService.persistAttributes(nhsDataDictionary, dictionaryFolder, classesDataModel, currentUser.emailAddress, attributeTerminologiesByName, attributeClassesByUin, attributeUinIsKey)
                log.info('AttributeService persisted in {}', Utils.timeTaken(startTime))
            }
            validateAndSaveModel(classesDataModel)
        }

        DataModel elementDataModel = null
        if (publishOptions.publishElements) {
            elementDataModel =
                    new DataModel(label: NhsDataDictionary.ELEMENTS_MODEL_NAME,
                            description: "NHS Data Dictionary Data Elements",
                            folder: dictionaryFolder,
                            createdBy: currentUser.emailAddress,
                            authority: authorityService.defaultAuthority,
                            type: DataModelType.DATA_STANDARD,
                            branchName: nhsDataDictionary.branchName)

            startTime = System.currentTimeMillis()
            elementService.persistElements(nhsDataDictionary, dictionaryFolder, elementDataModel, currentUser.emailAddress, attributeTerminologiesByName)
            log.info('ElementService persisted in {}', Utils.timeTaken(startTime))


            validateAndSaveModel(elementDataModel)
            endTime = System.currentTimeMillis()
            log.info('{} model built in {}', NhsDataDictionary.ELEMENTS_MODEL_NAME, Utils.getTimeString(endTime - startTime))
        }



        if(publishOptions.publishBusinessDefinitions) {
            startTime = System.currentTimeMillis()
            businessDefinitionService.persistBusinessDefinitions(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
            log.info('BusinessDefinitionService persisted in {}', Utils.timeTaken(startTime))
        }

        if(publishOptions.publishSupportingInformation) {
            startTime = System.currentTimeMillis()
            supportingInformationService.persistSupportingInformation(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
            log.info('SupportingInformationService persisted in {}', Utils.timeTaken(startTime))
        }

        if(publishOptions.publishDataSetConstraints) {
            startTime = System.currentTimeMillis()
            dataSetConstraintService.persistDataSetConstraints(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
            log.info('DataSetConstraintService persisted in {}', Utils.timeTaken(startTime))
        }

        if(publishOptions.publishDataSetFolders) {
            startTime = System.currentTimeMillis()
            dataSetFolderService.persistDataSetFolders(nhsDataDictionary, dictionaryFolder, elementDataModel, currentUser.emailAddress)
            log.info('DataSetFolderService persist complete in {}', Utils.timeTaken(startTime))
        }


        if(publishOptions.publishDataSets) {
            startTime = System.currentTimeMillis()
            dataSetService.persistDataSets(nhsDataDictionary, dictionaryFolder, elementDataModel, currentUser)
            log.info('DataSetService persist complete in {}', Utils.timeTaken(startTime))
        }

        // If any changes remain on the dictionary folder then save them
        if (dictionaryFolder.isDirty()) {
            log.debug('Validate and save {}', dictionaryFolderName)
            startTime = System.currentTimeMillis()
            if (!folderService.validate(dictionaryFolder)) {
                throw new MauroApplicationException('NHSDD', 'Invalid model', dictionaryFolder.errors)
            }
            versionedFolderService.save(dictionaryFolder, validate: false, flush: true)
            log.info('Validate and save {} complete in {}', dictionaryFolderName, Utils.timeTaken(startTime))
        } else {
            // Otherwise flush and clear the session before we do anything else
            log.debug('Flush the session')
            sessionFactory.currentSession.flush()
        }

        dictionaryFolder = versionedFolderService.get(dictionaryFolder.id)

        if (finalise) {
            log.info('Finalising {}', dictionaryFolderName)
            startTime = System.currentTimeMillis()
            ModelVersion requestedFolderVersion = folderVersionNo ? ModelVersion.from(folderVersionNo) : null
            versionedFolderService.finaliseFolder(dictionaryFolder, currentUser, requestedFolderVersion, VersionChangeType.MAJOR, releaseDate)
            log.info('Finalise {} complete in {}', dictionaryFolderName, Utils.timeTaken(startTime))
        }

        log.info('Final save and flush of the ingested {}', dictionaryFolderName)
        if (!folderService.validate(dictionaryFolder)) {
            throw new MauroApplicationException('NHSDD', 'Invalid model', dictionaryFolder.errors)
        }
        versionedFolderService.saveFolderHierarchy(dictionaryFolder)
        log.info('Ingest {} complete in {}', dictionaryFolderName, Utils.timeTaken(originalStartTime))
        dictionaryFolder

    }


    void deleteOriginalFolder(String coreFolderName) {
        Folder folder = folderService.findByPath(coreFolderName)
        if (folder) {
            folderService.delete(flolder, true, true)
        }
    }

    FhirBundle codeSystemValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.attributes.values()
            .findAll { attribute ->
                !attribute.isRetired() &&
                    attribute.codes.size() > 0
            }
            .sort { it.name }
            .collect {attribute ->
                new FhirEntry( requestUrl: 'CodeSystem/$validate',
                               resource: FhirCodeSystem.fromDDAttribute(attribute, "1.0.0", publishDate))
            }

        return new FhirBundle(type: "transaction", entries: entries)
    }

    FhirBundle valueSetValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.elements.values()
            .findAll { element ->
                !element.isRetired() &&
                    element.codes.size() > 0
            }
            .sort { it.name }
            .collect { element ->
                new FhirEntry( requestUrl: 'ValueSet/$validate',
                               resource: FhirValueSet.fromDDElement(element, "1.0.0", publishDate))
            }
        return new FhirBundle(type: "transaction", entries: entries)
    }

    String iso11179(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)

        MetadataBundle metadataBundle = ISO11179Helper.generateMetadataBundle(thisDataDictionary)

        return MarshalHelper.marshallMetadataBundleToString(metadataBundle)
    }


    ChangePaperPreview previewChangePaper(UUID versionedFolderId, boolean includeDataSets = false) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        Folder previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)

        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)
        NhsDataDictionary previousDataDictionary = buildDataDictionary(previousVersion.id)

        MauroCatalogueItemPathResolver pathResolver = new MauroCatalogueItemPathResolver(
            thisDictionary,
            dataModelService,
            dataClassService,
            dataElementService,
            terminologyService)

        ChangePaperPreview preview = ChangePaperHtmlUtility.generateChangePaper(
            pathResolver,
            thisDataDictionary,
            previousDataDictionary,
            includeDataSets)

        preview
    }

    File generateChangePaper(UUID versionedFolderId, boolean includeDataSets = false, boolean isTest = false) {

        Folder thisDictionary = versionedFolderService.get(versionedFolderId)

        Folder previousVersion
        if(thisDictionary.isFinalised()) {
            previousVersion = versionedFolderService.get(versionLinkService.findBySourceModelAndLinkType(thisDictionary, VersionLinkType.NEW_MODEL_VERSION_OF)?.targetModelId)
        } else {
            previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)
        }
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)
        NhsDataDictionary previousDataDictionary = previousVersion ? buildDataDictionary(previousVersion.id) : null

        Path outputPath = Paths.get(getTestOutputPath())
        if(!isTest) {
            outputPath = Files.createTempDirectory('changePaper')
        }
        return ChangePaperPdfUtility.generateChangePaper(thisDataDictionary, previousDataDictionary, outputPath, includeDataSets)
    }

    File generateWebsite(UUID versionedFolderId, DataDictionaryImportParameters parameters) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)
        thisDataDictionary.branchName = thisDictionary.finalised ? thisDictionary.modelVersionTag : thisDictionary.branchName

        //Path outputPath = Files.createTempDirectory('website')

        Path outputPath = Paths.get(getTestOutputPath())

        return WebsiteUtility.generateWebsite(thisDataDictionary, outputPath, parameters)
    }

    def shortDescriptions(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)

        List<String> response = []
        thisDataDictionary.getAllComponents().sort { it.name.toLowerCase()}.each { component ->
            String shortDescription = component.otherProperties["shortDescription"]
            response.add(""" "${component.getStereotype()}", "${component.getNameWithRetired()}", "${shortDescription}" """)
        }
        return response
    }

    NhsDataDictionary newDataDictionary() {
        NhsDataDictionary nhsDataDictionary = new NhsDataDictionary()
        setApiProperties(nhsDataDictionary)
        loadBranchInformation(nhsDataDictionary)
        return nhsDataDictionary
    }

    void setApiProperties(NhsDataDictionary dataDictionary) {

        apiPropertyCacheableRepository.findByCategory(NHSDD_PROPERTY_CATEGORY).each {apiProperty ->
            if(apiProperty.key == API_PROPERTY_RETIRED_TEMPLATE) {
                dataDictionary.retiredItemText = apiProperty.value
            }
            if(apiProperty.key == API_PROPERTY_PREPARATORY_TEMPLATE) {
                dataDictionary.preparatoryItemText = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL) {
                dataDictionary.changeRequestUrl = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_HEADER_TEXT) {
                dataDictionary.changeLogHeaderText = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_FOOTER_TEXT) {
                dataDictionary.changeLogFooterText = apiProperty.value
            }
        }
        if(!dataDictionary.preparatoryItemText) {
            dataDictionary.preparatoryItemText = KNOWN_KEYS[API_PROPERTY_PREPARATORY_TEMPLATE]
        }
        if(!dataDictionary.retiredItemText) {
            dataDictionary.retiredItemText = KNOWN_KEYS[API_PROPERTY_RETIRED_TEMPLATE]
        }
        if (!dataDictionary.changeRequestUrl) {
            dataDictionary.changeRequestUrl = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL]
        }
        if (!dataDictionary.changeLogHeaderText) {
            dataDictionary.changeLogHeaderText = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_HEADER_TEXT]
        }
        if (!dataDictionary.changeLogFooterText) {
            dataDictionary.changeLogFooterText = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_FOOTER_TEXT]
        }
    }

    void loadBranchInformation(NhsDataDictionary dataDictionary) {

        List<Folder> dictionaryFolders = folderCacheableRepository.readAll().findAll {it.label.startsWith("NHS Data Dictionary") }

        if (dictionaryFolders.empty) {
            return
        }

        dictionaryFolders
            .collect {versionedFolder -> new NhsDDBranch(versionedFolder) }
            .findAll {branch -> !branch.finalised && branch.branchName }
            .each { branch ->
                dataDictionary.workItemBranches[branch.branchName] = branch
            }
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
        // TODO - fix this
        // MergeDiff<Folder> objectDiff = versionedFolderService.getMergeDiffForVersionedFolders(thisDictionary, previousVersion)
        log.info('Merge Diff took {}', Utils.timeTaken(start))

        return objectDiff
    }

    void buildWorkItemDetails(Folder thisVersionedFolder, NhsDataDictionary dataDictionary) {
        dataDictionary.workItemDetails =
            thisVersionedFolder.metadata.findAll {md ->
                md.namespace == 'uk.nhs.datadictionary.workItem' // ddWorkItemProfileProviderService.metadataNamespace
            }.collectEntries {md ->
                [md.key, md.value]
            }
    }

    static List<Metadata> defaultProfileMetadata() {
        return [
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'namespace',
                         value: DDWorkItemProfileProviderService.getPackageName().toString()),
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'name',
                         value: DDWorkItemProfileProviderService.getSimpleName()),
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'version',
                         value: '1.0.0')
        ]
    }

    void validateAndSaveModel(DataModel dataModel) {
        long startTime = System.currentTimeMillis()
        log.debug('Validating [{}] model', dataModel.label)
        dataModelService.validate(dataModel)
        long endTime = System.currentTimeMillis()
        log.info('Validate [{}] model complete in {}', dataModel.label, Utils.getTimeString(endTime - startTime))
        startTime = endTime
        if (dataModel.hasErrors()) {
            throw new Exception('DMSXX', 'Model is invalid', dataModel.errors)
        }

        dataModelService.saveModelWithContent(dataModel, 1000)
        endTime = System.currentTimeMillis()
        log.info('Saved [{}] model complete in {}', dataModel.label, Utils.getTimeString(endTime - startTime))
    }

    /**
     * Prepare and get a test directory name under $TEMP - replaces the use of
     * the user desktop which might not be present on a server.
     */
    static String getTestOutputPath() {
        File ditaTestDir = new File(System.getProperty("java.io.tmpdir"), "ditaTest")

        if (ditaTestDir.exists()) {
            FileUtils.deleteDirectory(ditaTestDir)
        }
        ditaTestDir.mkdirs()

        ditaTestDir.absolutePath
    }
}