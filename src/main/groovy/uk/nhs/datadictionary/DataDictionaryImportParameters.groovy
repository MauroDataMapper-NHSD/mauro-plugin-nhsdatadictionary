package uk.nhs.datadictionary

import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.config.ImportGroupConfig
import org.maurodata.plugin.importer.config.ImportParameterConfig

class DataDictionaryImportParameters extends FileImportParameters {


    @ImportParameterConfig(
        displayName = 'Release Date',
        description = 'The date this version of the data dictionary was released',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Ingest Options',
            order = -2
        )
    )
    String releaseDate

    @ImportParameterConfig(
        displayName = 'Folder Version No',
        description = 'The Version Number this finalised VersionedFolder should take',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Ingest Options',
            order = 1
        )
    )
    String folderVersionNo

    @ImportParameterConfig(
        displayName = 'Previous Version',
        description = 'The Versioned Folder representing the previous release of this ingested Data Dictionary',
        order = 4,
        group = @ImportGroupConfig(
            name = 'Ingest Options',
            order = 0
        ))
    UUID prevVersion

    @ImportParameterConfig(
        displayName = 'Delete Previous Version',
        description = '(Development) Delete the previous version of the Data Dictionary',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Ingest Options',
            order = 2
        )
    )
    Boolean deletePrevious


    @ImportParameterConfig(
        displayName = 'Ingest Attributes',
        description = 'Whether to ingest the attributes',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishAttributes = true

    @ImportParameterConfig(
        displayName = 'Ingest Elements',
        description = 'Whether to ingest the elements',
        order = 2,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishElements = true

    @ImportParameterConfig(
        displayName = 'Ingest Classes',
        description = 'Whether to ingest the classes',
        order = 3,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishClasses = true

    @ImportParameterConfig(
        displayName = 'Ingest Data Sets',
        description = 'Whether to ingest the data sets',
        order = 4,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishDataSets = true

    @ImportParameterConfig(
        displayName = 'Ingest Business Definitions',
        description = 'Whether to ingest the business definitions',
        order = 5,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishBusinessDefinitions = true

    @ImportParameterConfig(
        displayName = 'Ingest Supporting Information',
        description = 'Whether to ingest the supporting information',
        order = 6,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishSupportingInformation = true

    @ImportParameterConfig(
        displayName = 'Ingest Data Set Constraints',
        description = 'Whether to ingest the data set constraints',
        order = 7,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishDataSetConstraints = true

    @ImportParameterConfig(
        displayName = 'Ingest Data Set Folders',
        description = 'Whether to ingest the data set folders',
        order = 8,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean publishDataSetFolders = true

    boolean isPublishableComponent(NhsDataDictionaryComponent dataDictionaryComponent) {
        if(dataDictionaryComponent instanceof NhsDDAttribute) {
            return publishAttributes
        }
        if(dataDictionaryComponent instanceof NhsDDElement) {
            return publishElements
        }
        if(dataDictionaryComponent instanceof NhsDDClass) {
            return publishClasses
        }
        if(dataDictionaryComponent instanceof NhsDDDataSet) {
            return publishDataSets
        }
        if(dataDictionaryComponent instanceof NhsDDBusinessDefinition) {
            return publishBusinessDefinitions
        }
        if(dataDictionaryComponent instanceof NhsDDSupportingInformation) {
            return publishSupportingInformation
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetConstraint) {
            return publishDataSetConstraints
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetFolder) {
            return publishDataSetFolders
        }
        if(dataDictionaryComponent instanceof NhsDDWebPage) {
            return true
        }
    }


}
