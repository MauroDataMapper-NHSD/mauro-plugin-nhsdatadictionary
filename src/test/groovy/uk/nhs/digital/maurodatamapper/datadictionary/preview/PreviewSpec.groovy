package uk.nhs.digital.maurodatamapper.datadictionary.preview

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.annotation.Sql
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.folder.FolderApi
import org.maurodata.domain.folder.Folder
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.web.ListResponse
import spock.lang.Shared
import spock.lang.Specification
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDataDictionaryImporter
import uk.nhs.datadictionary.api.NhsDataDictionaryApi
import uk.nhs.datadictionary.services.NhsDataDictionaryService
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

@Slf4j
@MicronautTest(startApplication = true, environments = ['secured'])
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
class PreviewSpec extends Specification {

    @Inject
    @Shared
    NhsDataDictionaryImporter nhsDataDictionaryImporter

    @Inject
    FolderApi folderApi

    @Inject NhsDataDictionaryApi nhsDataDictionaryAPI


    @Shared
    byte[] xmlBytes = new byte[60000000]

    @Shared
    DataDictionaryImportParameters dataDictionaryImportParameters

    @Shared UUID branchId

    @Inject ApplicationContext ctx

    void setup() {
        println "Active envs = ${ctx.environment.activeNames}"
        System.err.println(Runtime.getRuntime().maxMemory())
        this.class.getClassLoader().getResourceAsStream("november2021.xml").withStream {stream ->
            xmlBytes = stream.readAllBytes()
        }
        dataDictionaryImportParameters = new DataDictionaryImportParameters()
        dataDictionaryImportParameters.importFile = new FileParameter('november2021.xml', 'application/xml', xmlBytes)
        MultipartBody importRequest = MultipartBody.builder()
        //  .addPart('folderId', folderId.toString()) // Should now be optional
            .addPart('importFile', 'file.json', MediaType.APPLICATION_XML_TYPE, xmlBytes)
            .build()

        ListResponse<Folder> folders = folderApi.importModel(
            importRequest,
            nhsDataDictionaryImporter.namespace,
            nhsDataDictionaryImporter.name,
            nhsDataDictionaryImporter.version)

        branchId = folders.items.get(0).id
    }

    void "list all elements"() {
        when:
        List<StereotypedCatalogueItem> catalogueItems = nhsDataDictionaryAPI.allItemsIndex(branchId)

        then:
        System.err.println(catalogueItems.size())
        catalogueItems.size() == 9219
        catalogueItems.every {
            !it.stereotype.isEmpty() &&
                !it.name.isEmpty() &&
                it.catalogueItemId

        }
    }

}
