package uk.nhs.datadictionary

import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.config.ImportGroupConfig
import org.maurodata.plugin.importer.config.ImportParameterConfig

class DataDictionaryImportParameters extends FileImportParameters {


    @ImportParameterConfig(
        displayName = 'Release Date',
        description = 'The date this version of the data dictionary was released - e.g. March 2025',
        order = 1,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Ingest a finalised version',
            order = -4
        )
    )
    String releaseDate

    @ImportParameterConfig(
        displayName = 'Folder Version No',
        description = 'The Version Number this finalised VersionedFolder should take - e.g. 2025.03.01',
        order = 2,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Ingest a finalised version',
            order = -4
        )
    )
    String folderVersionNo

    @ImportParameterConfig(
        displayName = 'BranchName',
        description = 'The branch name for a draft / in-flight branch.  E.g. CR2010',
        order = 1,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Ingest an in-flight branch',
            order = -3
        )
    )
    String branchName

    @ImportParameterConfig(
        displayName = 'Previous Version',
        description = 'The Versioned Folder representing the previous release of this ingested Data Dictionary',
        order = 1,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Ingest Options',
            order = -2
        ))
    UUID prevVersion

    @ImportParameterConfig(
        displayName = 'Omit Attributes',
        description = 'Whether to omit attributes',
        order = 1,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitAttributes = false

    @ImportParameterConfig(
        displayName = 'Omit Elements',
        description = 'Whether to omit elements',
        order = 2,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitElements = false

    @ImportParameterConfig(
        displayName = 'Omit Classes',
        description = 'Whether to omit classes',
        order = 3,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitClasses = false

    @ImportParameterConfig(
        displayName = 'Omit Data Sets',
        description = 'Whether to omit data sets',
        order = 4,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitDataSets = false

    @ImportParameterConfig(
        displayName = 'Omit Business Definitions',
        description = 'Whether to omit business definitions',
        order = 5,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitBusinessDefinitions = true

    @ImportParameterConfig(
        displayName = 'Omit Supporting Information',
        description = 'Whether to omit supporting information',
        order = 6,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitSupportingInformation = true

    @ImportParameterConfig(
        displayName = 'Omit Data Set Constraints',
        description = 'Whether to omit data set constraints',
        order = 7,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitDataSetConstraints = true

    @ImportParameterConfig(
        displayName = 'Omit Data Set Folders',
        description = 'Whether to omit data set folders',
        order = 8,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Partial Ingest Options',
            order = -1
        )
    )
    boolean omitDataSetFolders = true

    boolean isPublishableComponent(NhsDataDictionaryComponent dataDictionaryComponent) {
        if(dataDictionaryComponent instanceof NhsDDAttribute) {
            return !omitAttributes
        }
        if(dataDictionaryComponent instanceof NhsDDElement) {
            return !omitElements
        }
        if(dataDictionaryComponent instanceof NhsDDClass) {
            return !omitClasses
        }
        if(dataDictionaryComponent instanceof NhsDDDataSet) {
            return !omitDataSets
        }
        if(dataDictionaryComponent instanceof NhsDDBusinessDefinition) {
            return !omitBusinessDefinitions
        }
        if(dataDictionaryComponent instanceof NhsDDSupportingInformation) {
            return !omitSupportingInformation
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetConstraint) {
            return !omitDataSetConstraints
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetFolder) {
            return !omitDataSetFolders
        }
        if(dataDictionaryComponent instanceof NhsDDWebPage) {
            return true
        }
    }


}
