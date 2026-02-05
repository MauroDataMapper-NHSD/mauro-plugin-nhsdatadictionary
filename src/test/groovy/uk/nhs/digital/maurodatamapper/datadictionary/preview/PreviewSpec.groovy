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
        this.class.getClassLoader().getResourceAsStream("datadictionary_november2025.xml").withStream {stream ->
            xmlBytes = stream.readAllBytes()
        }
        dataDictionaryImportParameters = new DataDictionaryImportParameters()
        dataDictionaryImportParameters.importFile = new FileParameter('datadictionary_november2025.xml', 'application/xml', xmlBytes)
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
        catalogueItems.size() == 10107

        catalogueItems.count { sci ->
            (0..9).find {sci.name.startsWith(it.toString())}
        } == 6

        [a:690, b:386, c:1215, d:594, e:521, f:181, g:208, h:396, i:326, j:33, k:30, l:247, m:479,
        n:467, o: 599, p:1404, q:18, r:607, s:955, t:366, u:154, v:42, w:147, x:2, y:32, z:2].every {alpha, size ->
            System.err.println(alpha)
            System.err.println(catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.sort {
                it.name.toLowerCase()
            }.collect {it.name})
            System.err.println(catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.sort {
                it.name.toLowerCase()
            }.collect {it.name}.size())
            catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.size() == size
        }

/*
    In 'C' the published version has CLINICAL TRIAL IDENTIFIER (e) and CHILDHOOD IMMUNISATION TYPE as both retired and non-retired
       There are two DISCHARGE LETTER ISSUED DATE (COMMUNITY CARE) (e)
       And two PATIENT ORGANISATION END DATE (a)
 */

        catalogueItems.every {
            !it.stereotype.isEmpty() &&
                !it.name.isEmpty() &&
                it.catalogueItemId

        }
    }

}
