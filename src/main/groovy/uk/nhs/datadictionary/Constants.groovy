package uk.nhs.datadictionary

class Constants {

    static final String WEBSITE_URL = "https://www.datadictionary.nhs.uk"

    static final String METADATA_NAMESPACE = "uk.nhs.datadictionary"
    static final String METADATA_DATASET_TABLE_NAMESPACE = METADATA_NAMESPACE + ".dataset.table"
    static final String METADATA_DATASET_ELEMENT_NAMESPACE = METADATA_NAMESPACE + ".dataset.element"
    static final String DEFAULT_PROFILE_NAMESPACE = "default.profile"

    static final String FOLDER_NAME = "NHS Data Dictionary"
    static final String ELEMENTS_MODEL_NAME = "Data Elements"
    static final String CLASSES_MODEL_NAME = "Classes and Attributes"
    static final String DATA_SETS_FOLDER_NAME = "Data Sets"
    static final String BUSINESS_DEFINITIONS_TERMINOLOGY_NAME = "NHS Business Definitions"
    static final String SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME = "Supporting Information"
    static final String DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME = "Data Set Constraints"

    // The following keys are used in DataSetParser and NhsDDDataSetClass and NhsDDDataSetElement and
    // must match the property names used in the dataSetTableProfile and dataSetElementProfile.
    static final String DATASET_TABLE_KEY_WEB_ORDER = "webOrder"
    static final String DATASET_TABLE_KEY_MRO = "mandation"
    static final String DATASET_TABLE_KEY_GROUP_REPEATS = "groupRepeats"
    static final String DATASET_TABLE_KEY_RULES = "rules"
    static final String DATASET_TABLE_KEY_MULTIPLICITY_TEXT = "multiplicity"
    static final String DATASET_TABLE_KEY_CHOICE = "choice"
    static final String DATASET_TABLE_KEY_AND = "and"
    static final String DATASET_TABLE_KEY_INCLUSIVE_OR = "inclusiveOr"
    static final String DATASET_TABLE_KEY_NOT_OPTION = "notOption"
    static final String DATASET_TABLE_KEY_ADDRESS_CHOICE = "addressChoice"
    static final String DATASET_TABLE_KEY_NAME_CHOICE = "nameChoice"
    static final String DATASET_TABLE_KEY_DATA_SET_REFERENCE = "dataSetReference"
    static final String DATASET_TABLE_KEY_DATA_SET_REFERENCE_TO = "dataSetReferenceTo"

    static final String CHANGE_REQUEST_NUMBER_TOKEN = "{cr_number}"

}
