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
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDClassLink
import uk.nhs.datadictionary.NhsDDClassRelationship
import uk.nhs.datadictionary.NhsDataDictionary

import javax.lang.model.type.PrimitiveType

@Slf4j
@Singleton
class ClassService extends DataDictionaryComponentService<DataClass, NhsDDClass> {

    @Inject
    DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    DataElementCacheableRepository dataElementCacheableRepository

    ClassService() {
    }

    String getStereotype() {
        "class"
    }


    @Override
    NhsDDClass show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        DataClass dataClass = dataClassCacheableRepository.findById(id)
        NhsDDClass nhsClass = initialiseComponent(new NhsDDClass(), dataClass, versionedFolderId, nhsDataDictionaryService)
        NhsDataDictionary dataDictionary = new NhsDataDictionary()
        nhsDataDictionaryService.setApiProperties(dataDictionary)
        List<NhsDDAttribute> attributes = getAttributesForShow(nhsClass, null)
        // Assign the attribute by key and non-key types. The NhsDDClass.getAttributes() method will combine them
        nhsClass.keyAttributes = attributes.findAll { it.key }.sort { it.name }
        nhsClass.otherAttributes = attributes.findAll { !it.key }.sort { it.name }

        List<NhsDDClassRelationship> relationships = getRelationshipsForShow(nhsClass, null)
        List<NhsDDClassRelationship> keyRelationships = relationships
            .findAll { it.key }
            .sort { it.targetClass.name }
        List<NhsDDClassRelationship> otherRelationships = relationships
            .findAll { !it.key }
            .sort { it.targetClass.name }
        nhsClass.classRelationships = keyRelationships + otherRelationships

        // Stop the JSON ouptut from recursing
        nhsClass.getAttributes().each {
            it.codes = []
        }

        return nhsClass
    }

    List<NhsDDAttribute> getAttributesForShow(NhsDDClass nhsClass, NhsDataDictionary dataDictionary) {
        Set<DataElement> attributeDataElements = dataElementCacheableRepository.findAllByDataClass(nhsClass.catalogueItem)
            .each {dataElement ->
                dataElement.dataType = mauroPersistenceService.dataTypeCacheableRepository.findById(dataElement.dataType.id)
            }
            .findAll {dataElement ->
                !(dataElement.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE)
            }

        // Get a cut-down version of the NhsDDAttribute list, we don't need national codes for previewing an NhsDDClass
        attributeDataElements
            .collect {dataElement ->
                NhsDDAttribute attribute = new NhsDDAttribute().fromMauroItem(dataDictionary, mauroPersistenceService, dataElement)
                attribute.catalogueItem = dataElement
                return attribute
            }
        .findAll { nhsAttribute ->
            // Do not include retired attributes in the list
            !nhsAttribute.isRetired()
        }
    }

    List<NhsDDClassRelationship> getRelationshipsForShow(NhsDDClass nhsClass, NhsDataDictionary dataDictionary) {
        Set<DataElement> relationshipDataElements = dataElementCacheableRepository.findAllByDataClass(nhsClass.catalogueItem)
            .each {dataElement ->
                dataElement.dataType = mauroPersistenceService.dataTypeCacheableRepository.findById(dataElement.dataType.id)
            }
            .findAll {
                it.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE
            }

        relationshipDataElements.collect { dataElement ->
            DataClass referencedClass = dataClassCacheableRepository.findById(dataElement.dataType.referenceClass.id)
            NhsDDClass referencedNhsClass = new NhsDDClass().fromMauroItem(dataDictionary, mauroPersistenceService, referencedClass) as NhsDDClass
            referencedNhsClass.catalogueItem = referencedClass
            NhsDDClassRelationship relationship = new NhsDDClassRelationship(dataElement, referencedNhsClass)
            relationship
        }
    }

    @Override
    Set<DataClass> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        DataModel classesModel = nhsDataDictionaryService.getClassesModel(versionedFolderId)
        List<DataClass> dataClasses = dataClassCacheableRepository.readAllByDataModel(classesModel)
        Map<UUID, DataClass> classMap = dataClasses.collectEntries {
            [it.id, it]
        }
        List<Metadata> metadata = metadataCacheableRepository.findByMultiFacetAwareItemIdInAndNamespaceAndKey(dataClasses.id, new NhsDDClass().getMetadataNamespace(), "isRetired")

        metadata.each {md ->
            classMap[md.multiFacetAwareItemId].metadata.add(md)
        }
        return classMap.values().findAll {dataClass ->
            dataClass.label != "Retired" && (includeRetired || !catalogueItemIsRetired(dataClass))
        } as Set<DataClass>
    }



    void createClassesModel(NhsDataDictionary dataDictionary, DataModel classesDataModel,
                        Map<String, DataClass> attributeClassesByUin, Set<String> attributeUinIsKey) {

        DataClass retiredDataClass = new DataClass(
            label: "Retired",
            dataModel: classesDataModel)

        classesDataModel.childDataClasses.add(retiredDataClass)
        classesDataModel.allDataClasses.add(retiredDataClass)

        TreeMap<String, DataClass> classesByUin = new TreeMap<>()

        dataDictionary.classes.each {name, clazz ->
            DataClass dataClass = new DataClass(
                label: name,
                description: clazz.description
            )

            // TODO unnecessary as the or statement above excludes all non-retired DCs
            classesDataModel.allDataClasses.add(dataClass)
            if (clazz.isRetired()) {
                retiredDataClass.dataClasses.add(dataClass)

            } else {
                classesDataModel.childDataClasses.add(dataClass)
            }

            // We used to link the attributes here, but now that's all done in the attribute
            // service because they're stored directly there.
            // However, since the classes contain the information about which attribute appears in which class,
            // we'll maintain a map here

            clazz.getAttributes().each {
                attributeClassesByUin[it.uin] = dataClass
            }
            clazz.keyAttributes.each {
                attributeUinIsKey.add(it.uin)
            }
            addMetadataFromComponent(dataClass, clazz)

            classesByUin[clazz.getUin()] = dataClass
        }

        // Now link the references - we can only do this once all the classes are in place
        TreeMap<String, DataType> classReferenceTypesByName = new TreeMap<>()
        dataDictionary.classes.each {name, clazz ->
            clazz.classLinks.each {classLink ->
                if (classLink.metaclass == "KernelAssociation20") {

                    if (classLink.supplierClass) {
                        DataClass thisDataClass = classesByUin[classLink.clientClass.getUin()]
                        // Get the target class
                        DataClass targetDataClass = classesByUin[classLink.supplierClass.getUin()]

                        // Get a reference type (create if it doesn't exist)
                        DataType targetReferenceType = classReferenceTypesByName[targetDataClass.label]
                        if (!targetReferenceType) {

                            targetReferenceType = new DataType(
                                label: "${targetDataClass.label} Reference",
                                referenceClass: targetDataClass,
                                dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE)
                            //targetDataClass.addToReferenceTypes(targetReferenceType)
                            classesDataModel.dataTypes.add(targetReferenceType)
                            classReferenceTypesByName[targetDataClass.label] = targetReferenceType
                        }

                        DataType sourceReferenceType = classReferenceTypesByName[thisDataClass.label]
                        if (!sourceReferenceType) {
                            sourceReferenceType = new DataType(
                                label: "${thisDataClass.label} Reference",
                                referenceClass: thisDataClass,
                                dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE)
                            //thisDataClass.addToReferenceTypes(sourceReferenceType)
                            classesDataModel.dataTypes.add(sourceReferenceType)
                            classReferenceTypesByName[thisDataClass.label] = sourceReferenceType
                        }

                        // create a data element in the class
                        String sourceLabel = classLink.clientRole
                        int count = 0
                        if (thisDataClass.dataElements) {
                            count = thisDataClass.dataElements.findAll {it.label.startsWith(sourceLabel.trim())}.size()
                        }
                        if (count > 0) {
                            sourceLabel += " (${count})"
                        }
                        DataElement sourceDataElement = new DataElement(label: sourceLabel, dataType: targetReferenceType)
                        NhsDDClassLink.setMultiplicityToDataElement(sourceDataElement, classLink.supplierCardinality)
                        addMetadataForLink(clazz.getMetadataNamespace(), classLink, sourceDataElement)
                        addToMetadata(sourceDataElement, clazz.getMetadataNamespace(), NhsDDClassLink.IS_KEY_METADATA_KEY, classLink.isPartOfSupplierKey().toString())
                        addToMetadata(sourceDataElement, clazz.getMetadataNamespace(), NhsDDClassLink.IS_CHOICE_METADATA_KEY, classLink.hasRelationClientExclusivity().toString())
                        //addToMetadata(sourceDataElement, NhsDDClassLink.DIRECTION_METADATA_KEY, NhsDDClassLink.CLIENT_DIRECTION)
                        thisDataClass.dataElements.add(sourceDataElement)

                        String targetLabel = classLink.supplierRole
                        count = 0
                        if (targetDataClass.dataElements) {
                            count = targetDataClass.dataElements.findAll {it.label.startsWith(targetLabel.trim())}.size()
                        }
                        if (count > 0) {
                            targetLabel += " (${count})"
                        }
                        DataElement targetDataElement = new DataElement(label: targetLabel, dataType: sourceReferenceType)
                        NhsDDClassLink.setMultiplicityToDataElement(targetDataElement, classLink.clientCardinality)
                        addMetadataForLink(clazz.getMetadataNamespace(), classLink, targetDataElement)
                        addToMetadata(targetDataElement, clazz.getMetadataNamespace(), NhsDDClassLink.IS_KEY_METADATA_KEY, classLink.isPartOfClientKey().toString())
                        addToMetadata(targetDataElement, clazz.getMetadataNamespace(), NhsDDClassLink.IS_CHOICE_METADATA_KEY, classLink.hasRelationSupplierExclusivity().toString())
                        //addToMetadata(targetDataElement, NhsDDClassLink.DIRECTION_METADATA_KEY, NhsDDClassLink.SUPPLIER_DIRECTION)
                        targetDataClass.dataElements.add(targetDataElement)
                    }
                } else if (classLink.metaclass == "Generalization20") {
                    DataClass thisDataClass = classesByUin[classLink.clientClass.getUin()]
                    // Get the target class
                    DataClass targetDataClass = classesByUin[classLink.supplierClass.getUin()]
                    thisDataClass.extendsDataClasses.add(targetDataClass)
                }
            }
        }
    }

    void addMetadataForLink(String namespace, NhsDDClassLink classLink, DataElement dataElement) {
        Map<String, String> metadata = [
            "uin": classLink.uin,
            "metaclass": classLink.metaclass,
            "clientRole": classLink.clientRole,
            "supplierRole": classLink.supplierRole,
            "clientCardinality": classLink.clientCardinality,
            "supplierCardinality": classLink.supplierCardinality,
            "name": classLink.name,
            "partOfClientKey": classLink.partOfClientKey,
            "partOfSupplierKey": classLink.partOfSupplierKey,
            "supplierUin": classLink.supplierUin,
            "clientUin": classLink.clientUin,
            "relationSupplierExclusivity": classLink.relationSupplierExclusivity,
            "relationClientExclusivity": classLink.relationClientExclusivity,
            "direction": classLink.direction
        ]
        metadata.each {key, value ->
            addToMetadata(dataElement, namespace, key, value.toString())
        }
    }

    NhsDDClass classFromDataClass(DataClass dc, NhsDataDictionary dataDictionary) {
        NhsDDClass clazz = new NhsDDClass().fromMauroItem(dataDictionary, mauroPersistenceService, dc)
        dc.dataElements.each {dataElement ->
            if(dataElement.dataType instanceof PrimitiveType) {
                NhsDDAttribute foundAttribute = dataDictionary.attributes[dataElement.label]
                if (foundAttribute) {
                    clazz.keyAttributes.add(foundAttribute)
                } else {
                    log.error("Cannot find attribute: {}", dataElement.label)
                }
            }
        }
        return clazz
    }

    NhsDDClass getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.classes.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }
}