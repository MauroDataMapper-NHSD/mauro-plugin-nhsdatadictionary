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
package uk.nhs.datadictionary.publish

import org.maurodata.iso11179.domain.Acceptability
import org.maurodata.iso11179.domain.Contact
import org.maurodata.iso11179.domain.DataElement
import org.maurodata.iso11179.domain.Definition
import org.maurodata.iso11179.domain.DefinitionContext
import org.maurodata.iso11179.domain.Designation
import org.maurodata.iso11179.domain.DesignationContext
import org.maurodata.iso11179.domain.EnumeratedConceptualDomain
import org.maurodata.iso11179.domain.EnumeratedValueDomain
import org.maurodata.iso11179.domain.Individual
import org.maurodata.iso11179.domain.LanguageIdentification
import org.maurodata.iso11179.domain.MessageConformanceLevel
import org.maurodata.iso11179.domain.MessageStandard
import org.maurodata.iso11179.domain.MetadataBundle
import org.maurodata.iso11179.domain.MetadataItem
import org.maurodata.iso11179.domain.Organization
import org.maurodata.iso11179.domain.PermissibleValue
import org.maurodata.iso11179.domain.PostalAddress
import org.maurodata.iso11179.domain.Registration
import org.maurodata.iso11179.domain.RegistrationAuthorityIdentifier
import org.maurodata.iso11179.domain.RegistrationState
import org.maurodata.iso11179.domain.RegistrySpecification
import org.maurodata.iso11179.domain.Role
import org.maurodata.iso11179.domain.ScopedIdentifier
import org.maurodata.iso11179.domain.Slot
import org.maurodata.iso11179.domain.StewardshipRecord
import org.maurodata.iso11179.domain.SubmissionRecord
import org.maurodata.iso11179.domain.Text
import org.maurodata.iso11179.domain.ValueMeaning

import uk.nhs.datadictionary.NhsDataDictionary

import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class ISO11179Helper {

    final static String ORGANISATION_IDENTIFIER = "https://www.england.nhs.uk"
    final static String CONTACT_IDENTIFIER = "cath.chilcott@nhs.net"

    static MetadataBundle generateMetadataBundle(NhsDataDictionary nhsDataDictionary) {
        MetadataBundle metadataBundle = new MetadataBundle().tap {
            sourceRegistry = generateRegistrySpecification(nhsDataDictionary)

            messageStandard.add(MessageStandard.ISO_IEC_19583_26)
            messageConformanceLevel = MessageConformanceLevel.BASIC
            final GregorianCalendar now = new GregorianCalendar()
            exportedOn = DatatypeFactory.newInstance().newXMLGregorianCalendar(now)

            content = generateContent(nhsDataDictionary)
        }

    }

    static RegistrySpecification generateRegistrySpecification(NhsDataDictionary nhsDataDictionary) {
        return new RegistrySpecification().tap {
            name = "NHS Data Dictionary"
            webAddress = "https://www.datadictionary.nhs.uk"
            standard = "ISO/IEC 19583-26:2023(Basic)"
            conformanceLevel = "Basic"
            characterRepertoire = "UTF-8"
            referenceDocumentIdentifierForm = "uri"
        }

    }

    static MetadataBundle.Content generateContent(NhsDataDictionary nhsDataDictionary) {
        return new MetadataBundle.Content().tap {content ->
            nhsDataDictionary.elements.values()
                    .sort {it.name }
                    .findAll { it.name.toLowerCase().startsWith('a')}
                    .each {nhsDDElement ->
                content.dataElement.add(
                        new DataElement().tap {isoDataElement ->
                            itemMetadata = new MetadataItem().tap {
                                identifier.add(new ScopedIdentifier().tap {
                                    scope = "datadictionary.nhs.uk"
                                    identifier = nhsDDElement.name
                                    version = nhsDataDictionary.branchName

                                })
                                nhsDDElement.aliases.each {aliasType, alias ->
                                    slot.add(new Slot().tap {
                                        name = aliasType
                                        type = "xs:string"
                                        value.add(alias)
                                    })
                                }


                                designation.add(new Designation().tap {

                                    scope.add(getDesignationScope())
                                    sign = nhsDDElement.name
                                    language = getLanguage()

                                })

                                definition.add(new Definition().tap {
                                    text = new Text().tap {
                                        value = nhsDDElement.definition
                                    }
                                    language = getLanguage()
                                    scope.add(getDefinitionScope())
                                })
                                submissionRecord.add(new SubmissionRecord().tap {
                                    organizationIdentifier = ORGANISATION_IDENTIFIER
                                    contactIdentifier = CONTACT_IDENTIFIER
                                })
                                registrationState.add(createNhsRegistrationState())
                                stewardshipRecord = new StewardshipRecord().tap {
                                    organizationIdentifier = ORGANISATION_IDENTIFIER
                                    contactIdentifier = CONTACT_IDENTIFIER
                                }
                                creationDate = createDate()
                                lastChangeDate = createDate()
                            }
                            domain = new ScopedIdentifier().tap {
                                scope = 'datadictionary.nhs.uk'
                                identifier = nhsDDElement.name + " domain"
                                version = nhsDataDictionary.branchName
                            }
                            meaning = new ScopedIdentifier().tap {
                                scope = 'datadictionary.nhs.uk'
                                identifier = nhsDDElement.name + " meaning"
                                version = nhsDataDictionary.branchName
                            }

                        }
                )
                if(nhsDDElement.codes) {
                    content.enumeratedValueDomain.add(new EnumeratedValueDomain().tap { enumeratedValueDomain ->
                        itemMetadata = new MetadataItem().tap {
                            identifier.add(new ScopedIdentifier().tap {
                                scope = "datadictionary.nhs.uk"
                                identifier = nhsDDElement.name + " domain"
                                version = nhsDataDictionary.branchName

                            })
                            nhsDDElement.aliases.each { aliasType, alias ->
                                slot.add(new Slot().tap {
                                    name = aliasType
                                    type = "xs:string"
                                    value.add(alias + " domain")
                                })
                            }
                            designation.add(new Designation().tap {
                                scope.add(getDesignationScope())
                                sign = nhsDDElement.name + " domain"
                                language = getLanguage()
                            })
                            submissionRecord.add(new SubmissionRecord().tap {
                                organizationIdentifier = ORGANISATION_IDENTIFIER
                                contactIdentifier = CONTACT_IDENTIFIER
                            })
                            registrationState.add(createNhsRegistrationState())
                            stewardshipRecord = new StewardshipRecord().tap {
                                organizationIdentifier = ORGANISATION_IDENTIFIER
                                contactIdentifier = CONTACT_IDENTIFIER
                            }
                            creationDate = createDate()
                            lastChangeDate = createDate()
                        }
                        datatype = new ScopedIdentifier().tap {
                            scope = "datadictionary.nhs.uk"
                            identifier = "string"
                            version = nhsDataDictionary.branchName
                        }
                        meaning = new ScopedIdentifier().tap {
                            scope = "datadictionary.nhs.uk"
                            identifier = "the meaning of a string"
                            version = nhsDataDictionary.branchName
                        }
                        nhsDDElement.codes.each {code ->
                            member.add(new PermissibleValue().tap { permissibleValue ->
                                permittedValue = code.code
                                beginDate = createDate()
                                // no end date
                                meaning = new ScopedIdentifier().tap {
                                    scope = "datadictionary.nhs.uk"
                                    identifier = nhsDDElement.name + " - " + code.code + " domain"
                                    version = nhsDataDictionary.branchName
                                }
                            })
                        }
                    })
                    content.enumeratedConceptualDomain.add(new EnumeratedConceptualDomain().tap { enumeratedConceptualDomain ->
                        itemMetadata = new MetadataItem().tap {
                            identifier.add(new ScopedIdentifier().tap {
                                scope = "datadictionary.nhs.uk"
                                identifier = nhsDDElement.name + " meaning"
                                version = nhsDataDictionary.branchName

                            })
                            /*
                            nhsDDElement.aliases.each { aliasType, alias ->
                                slot.add(new Slot().tap {
                                    name = aliasType
                                    type = "xs:string"
                                    value.add(alias + " meaning")
                                })
                            }
                            */
                            designation.add(new Designation().tap {
                                scope.add(getDesignationScope())
                                sign = nhsDDElement.name + " meaning"
                                language = getLanguage()
                            })
                            submissionRecord.add(new SubmissionRecord().tap {
                                organizationIdentifier = ORGANISATION_IDENTIFIER
                                contactIdentifier = CONTACT_IDENTIFIER
                            })
                            registrationState.add(createNhsRegistrationState())
                            stewardshipRecord = new StewardshipRecord().tap {
                                organizationIdentifier = ORGANISATION_IDENTIFIER
                                contactIdentifier = CONTACT_IDENTIFIER
                            }
                            creationDate = createDate()
                            lastChangeDate = createDate()
                        }
                        nhsDDElement.codes.each {code ->
                            member.add(new ValueMeaning().tap { valueMeaning ->
                                identifier.add(new ScopedIdentifier().tap {
                                    scope = "datadictionary.nhs.uk"
                                    identifier = nhsDDElement.name + " - " + code.code + " domain"
                                    version = nhsDataDictionary.branchName

                                })
                                /*
                                nhsDDElement.aliases.each { aliasType, alias ->
                                    slot.add(new Slot().tap {
                                        name = aliasType
                                        type = "xs:string"
                                        value.add(alias + " meaning")
                                    })
                                }
                                */
                                designation.add(new Designation().tap {
                                    scope.add(getDesignationScope())
                                    sign = code.definition
                                    language = getLanguage()
                                })

                                beginDate = createDate()
                                // no end date
                            })
                        }
                    })

                }
            }
            content.organization.add(getNhsOrganization())
            content.contact.add(getNhsContact())
        }
    }

    static XMLGregorianCalendar createDate() {
        GregorianCalendar c = new GregorianCalendar()
        c.setTime(new Date())
        DatatypeFactory.newInstance().newXMLGregorianCalendar(c);

    }

/*
        <registration_state>
            <registration_authority>
                <international_code_designator>GB</international_code_designator>
                <organization_identifier>https://digital.nhs.uk/services/nhs-data-model-and-dictionary-service</organization_identifier>
            </registration_authority>
            <state>
                <registration_status>Preferred Standard</registration_status><!--the standadisation status of the item -->
                <effective_date>2022-01-01T00:00:00Z</effective_date>
                <until_date>2022-01-01T00:00:00Z</until_date>
                <administrative_status>Released</administrative_status><!--where the content is in the administration workflow-->
            </state>
        </registration_state>
*/

    static Registration createNhsRegistrationState() {
        new Registration().tap {
            registrationAuthority.add(new RegistrationAuthorityIdentifier().tap {
                internationalCodeDesignator = 'GB'
                organizationIdentifier = 'https://digital.nhs.uk/services/nhs-data-model-and-dictionary-service'
            })
            state = new RegistrationState().tap {
                registrationStatus = 'Preferred Standard'
                effectiveDate = createDate()
                untilDate = createDate()
                administrativeStatus = 'Released'
            }
        }
    }

/*
            <contact>
                <individual>
                    <name>Cath Chilcott</name>
                    <mail_address>
                        <organisation_name>NHS Data Model and Dictionary Service</organisation_name>
                        <city>Leeds</city>
                    </mail_address>
                    <email_address>cath.chilcott@nhs.net</email_address>
                    <role>
                        <title>Principal Business and Operations Delivery Manager</title>
                        <mail_address>
                            <organisation_name>NHS Data Model and Dictionary Service</organisation_name>
                            <city>Leeds</city>
                        </mail_address>
                        <email_address>cath.chilcott@nhs.net</email_address>
                    </role>
                </individual>
            </contact>
*/

    static Contact getNhsContact() {
        new Contact().tap {
            individual = new Individual().tap {
                contactIdentifier = CONTACT_IDENTIFIER
                name = 'Cath Chilcott'
                mailAddress = new PostalAddress().tap {
                    organisationName = 'NHS Data Model and Dictionary Service'
                    city = 'Leeds'
                }
                emailAddress.add('cath.chilcott@nhs.net')
                role.add(new Role().tap {
                    title = 'Principal Business and Operations Delivery Manager'
                    mailAddress = new PostalAddress().tap {
                        organisationName = 'NHS Data Model and Dictionary Service'
                        city = 'Leeds'
                    }
                    emailAddress.add('cath.chilcott@nhs.net')
                })
            }
            organizationIdentifier = ORGANISATION_IDENTIFIER
        }
    }
/*
            <organization>
                <name>NHS Data Model and Dictionary Service</name>
                <mail_address>
                    <organisation_name>NHS Data Model and Dictionary Service</organisation_name>
                    <city>Leeds</city>
                </mail_address>
                <email_address></email_address>
                <phone_number>123456789123456</phone_number>
                <uri>https://digital.nhs.uk/services/nhs-data-model-and-dictionary-service</uri>
            </organization>
 */
    static Organization getNhsOrganization() {
        new Organization().tap {
            name.add('NHS Data Model and Dictionary Service')
            organisationIdentifier = ORGANISATION_IDENTIFIER
            mailAddress = new PostalAddress().tap {
                organisationName = 'NHS Data Model and Dictionary Service'
                city = 'Leeds'
            }
            emailAddress.add('information.standards@nhs.net')
            phoneNumber.add('012345678901234')
            uri = 'https://digital.nhs.uk/services/nhs-data-model-and-dictionary-service'
        }
    }



        /*
            <definition>
                <text>NHS NUMBER is the same as attribute NHS NUMBER.</text>
                <language>
                    <language_identifier>en</language_identifier>
                </language>
                <scope>
                    <context><!--boilerplate scope entry for NHSDD context-->
                        <scope>NHS</scope>
                        <identifier>000001</identifier>
                        <version>1</version>
                    </context>
                    <acceptability>preferred</acceptability>
                </scope>
            </definition>

         */
    static DefinitionContext getDefinitionScope() {
        new DefinitionContext().tap {
            context.add(new ScopedIdentifier().tap {
                scope = 'NHS'
                identifier = '000001'
                version = '1'
            })
            acceptability = Acceptability.PREFERRED
        }
    }


    static DesignationContext getDesignationScope() {
        new DesignationContext().tap {

            context.add(new ScopedIdentifier().tap {
                scope = 'NHS'
                identifier = '000001'
                version = '1'
            })

        }
    }

    static LanguageIdentification getLanguage() {
        new LanguageIdentification().tap {
            languageIdentifier = 'en'
        }
    }

}
