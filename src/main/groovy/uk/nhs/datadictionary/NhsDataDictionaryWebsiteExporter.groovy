package uk.nhs.datadictionary

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.plugin.exporter.FolderExporterPlugin
import org.maurodata.plugin.exporter.json.JsonFolderExporterPlugin
import org.maurodata.plugin.importer.FolderImporterPlugin
import uk.nhs.datadictionary.publish.website.WebsiteUtility
import uk.nhs.datadictionary.services.NhsDataDictionaryService

@Slf4j
@Singleton
@CompileStatic
class NhsDataDictionaryWebsiteExporter implements FolderExporterPlugin {

    @Inject
    @JsonIgnore
    ApplicationContext applicationContext


    @Override
    String getVersion() {
        "1.0.0"
    }

    String displayName = 'NHS Data Dictionary XML Website Exporter'

    @Override
    byte[] exportModel(Folder model) {
        NhsDataDictionaryService nhsDataDictionaryService = applicationContext.getBean(NhsDataDictionaryService)
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(model.id)
        return WebsiteUtility.generateWebsite(dataDictionary, NhsDataDictionaryService.getTestOutputPath(), new DataDictionaryImportParameters())
    }

    @Override
    String getFileExtension() {
        return ".zip"
    }

    @Override
    String getContentType() {
        return "application/zip"
    }

    @Override
    String getFileName(Folder model) {
        return null
    }

    @Override
    byte[] exportModels(Collection<Folder> models) {
        return exportModel(models[0])
    }
}
