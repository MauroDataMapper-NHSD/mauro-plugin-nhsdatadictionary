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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.publish.ItemLinkScanner
import uk.nhs.datadictionary.publish.MauroCatalogueItemPathResolver
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Singleton
@CompileStatic
abstract class DataDictionaryComponentService<T extends AdministeredItem, D extends NhsDataDictionaryComponent> {

    @Inject
    MetadataCacheableRepository metadataCacheableRepository

    @Inject MauroPersistenceService mauroPersistenceService

    @Inject FolderCacheableRepository folderCacheableRepository

    @Inject ApplicationContext applicationContext

    abstract String getStereotype()

    List<StereotypedCatalogueItem> index(UUID dictionaryFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        getAll(dictionaryFolderId, nhsDataDictionaryService, includeRetired)
            .collect {new StereotypedCatalogueItem(it, stereotype)}
            .sort {it.name}
    }

    abstract def show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService)

    abstract Set<T> getAll(UUID dictionaryFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false)

/*    Map<String, String> getAliases(T catalogueItem) {
        Map<String, String> aliases = [:]
        catalogueItem.aliases
        NhsDataDictionary.aliasFields.each {aliasField ->
            Metadata foundMd = catalogueItem.metadata.find {it.namespace == getMetadataNamespace() && it.key == aliasField.key}
            if (foundMd) {
                aliases[aliasField.value] = foundMd.value
            }
        }
        return aliases
    }
*/
    abstract D getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary)

    List<Map<String, Object>> getWhereUsed(NhsDataDictionary dataDictionary, UUID id) {

        // Do a full check of every "where used" link type, same as the DITA generation. Only way to be sure that
        // every possible link is captured
        Map<String, NhsDataDictionaryComponent> pathLookup = [:]
        dataDictionary.allComponents.each { component ->
            pathLookup[component.getMauroPath()] = component
        }

        dataDictionary.allComponents.each { component ->
            component.replaceLinksInDefinition(pathLookup)
        }

        NhsDataDictionaryComponent component = getByCatalogueItemId(id, dataDictionary)
        component.updateWhereUsed()
        return component.whereUsed.entrySet()
            .findAll { !it.key.isRetired() }
            .sort { it.key.name }
            .collect { entry ->
                [   'catalogueId': entry.key.catalogueItem.id.toString(),
                    'description': entry.value,
                    'isRetired': entry.key.isRetired(),
                    'name': entry.key.name,
                    'stereotype': entry.key.stereotypeForPreview
                ]
            } as List<Map<String, Object>>
    }

    /**
     * This is the HTML link pattern to use for descriptions in Mauro. These could be ingested links, or links created
     * via the Mauro UI
     */
    static Pattern pattern = Pattern.compile("<a\\s+[^>]*?href=\"([^\"]+)\"[^>]*>(.*?)</a>")


    String convertLinksInDescription(UUID branchId, String description) {


        MauroCatalogueItemPathResolver pathResolver = applicationContext.createBean(MauroCatalogueItemPathResolver)
        pathResolver.setVersionedFolderId(branchId)

        PublishContext publishContext = new PublishContext(PublishTarget.WEBSITE)
        publishContext.setItemLinkScanner(
            ItemLinkScanner.createForHtmlPreview(branchId, pathResolver))

        // Don't pretty print the output, try to reduce the response size
        publishContext.prettyPrintHtml = false
        return publishContext.replaceLinksInString(description).trim() ?: null
    }

    String replaceLinksInShortDescription(String input) {
        String output = input
        Matcher matcher = pattern.matcher(input)
        while (matcher.find()) {
            output = output.replace(matcher.group(0), matcher.group(1))
        }
        return output
    }

    static String getStereotypeByPath(String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "businessDefinition"
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "supportingInformation"
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            return "dataSetConstraint"
        }
        if (path[0] == "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}") {
            if(path[path.size() -1].startsWith("de:")) {
                return "attribute"
            }
            return "class"
        }
        if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
            return "element"
        }

        if (path.length == 1 && path[0].startsWith("dm:")) {
            return "dataSet"
        }
        return null
    }


    Map<String, String> attributeMetadata = [
        // Aliases
        "aliasShortName"               : "aliasShortName",
        "aliasAlsoKnownAs"             : "aliasAlsoKnownAs",
        "aliasPlural"                  : "aliasPlural",
        "aliasFormerly"                : "aliasFormerly",
        "aliasFullName"                : "aliasFullName",
        "aliasIndexName"               : "aliasIndexName",
        "aliasSchema"                  : "aliasSchema",
        "name"                         : "name",
        "titleCaseName"                : "TitleCaseName",
        "websitePageHeading"           : "websitePageHeading",

        // SNOMED
        "aliasSnomedCTRefsetId"        : "aliasSnomedCTRefsetId",
        "aliasSnomedCTRefsetName"      : "aliasSnomedCTRefsetName",
        "aliasSnomedCTSubsetName"      : "aliasSnomedCTSubsetName",
        "aliasSnomedCTSubsetOriginalId": "aliasSnomedCTSubsetOriginalId",

        // Publication Lifecycle
        "isPreparatory"                : "isPrepatory",
        "isRetired"                    : "isRetired",
        "retiredDate"                  : "retiredDate",

        // Legacy
        "uin"                          : "uin",
        "baseUri"                      : "base-uri",
        "ultimatePath"                 : "ultimate-path",
        "ddUrl"                        : "DD_URL",
        "search"                       : "search",
        "baseVersion"                  : "baseVersion",
        // "modFinal": "mod__final",
        // "modAbstract": "mod__abstract",

        // Encoding
        "formatLength"                 : "format-length",
        "formatLink"                   : "format-link",


        //"fhirItem": "FHIR_Item",
        //"definition": "definition",
        //"codeSystem": "code-system",
        //"link": "link",
        //"linkTarget": "link-target",
        //"property": "property",
        //"extends": "extends",
        //"type": "type",
        //"readOnly": "readOnly",
        //"clientRole": "clientRole",
        //"permittedNationalCodes": "permitted-national-codes",
        "navigationParent"             : "navigationParent",
        "explanatoryPage"              : "explanatoryPage",
        "class"                        : "class",
        "doNotShowChangeLog"           : "doNotShowChangeLog",
        "node"                         : "node",
        "referencedElement"            : "referencedElement",
        "participant"                  : "participant",
        "supportingFile"               : "supportingFile",
        "isNavigationComponent"        : "isNavigationComponent",
        "isWebsiteIndex"               : "isWebsiteIndex",

        // Not sure about this one...
        // "extraFieldHead": "ExtraFieldhead",

        // do I need to parse these for history?
        //"defaultCode": "DefaultCode",
        //"nationalCodes": "national-codes",
        //"defaultCodes": "default-codes",
        //"valueSet": "value-set",
    ]

    void addMetadataFromComponent(AdministeredItem domainObject, NhsDataDictionaryComponent component) {
        component.otherProperties.each {key, value ->
            if(!NhsDataDictionary.KEYS_FOR_INGEST_ONLY.contains(key))
                addToMetadata(domainObject, component.getMetadataNamespace(), key, value)
        }
    }


    void addToMetadata(AdministeredItem domainObject, String namespace, String key, String value) {
        if (domainObject && value) {
            if(domainObject.metadata.find {
                it.key == key
            }) {
                throw new Exception("Duplicate metadata for $key on object of type ${domainObject.domainType}")
            } else {
                domainObject.metadata(namespace, key, value)
            }
        }
    }


    boolean catalogueItemIsRetired(AdministeredItem catalogueItem) {
        // Maybe should check for namespace here as well?
        catalogueItem.metadata.find {
            it.key == 'isRetired' && it.value == 'true'
        }
    }



    Folder getFolderAtPath(Folder sourceFolder, List<String> path) {
        if (path.size() == 0) {
            return sourceFolder
        } else {
            String nextFolderName = path.remove(0)
            Folder nextFolder = sourceFolder.childFolders.find {it.label == nextFolderName}
            if (!nextFolder) {
                nextFolder = new Folder(label: nextFolderName)
                sourceFolder.childFolders.add(nextFolder)
            }
            return getFolderAtPath(nextFolder, path)

        }

    }

}
