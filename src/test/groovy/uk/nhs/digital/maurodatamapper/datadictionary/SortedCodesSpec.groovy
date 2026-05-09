package uk.nhs.digital.maurodatamapper.datadictionary

import spock.lang.Specification
import uk.nhs.datadictionary.NhsDDCode

class SortedCodesSpec extends Specification {

    void 'codes in alphabetical order'() {

        when:
        List<NhsDDCode> codes = ['03', '02', '01', '00'].collect {
            new NhsDDCode(code: it, definition: it)
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['00', '01', '02', '03']

        when:
        codes = ['01', '02', '98', '03'].collect {
            new NhsDDCode(code: it, definition: it)
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['01', '02', '03', '98']

        when:
        codes = ['A', 'B', 'Y', 'N'].collect {
            new NhsDDCode(code: it, definition: it)
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['A', 'B', 'N', 'Y']

        when:
        codes = ['Y', 'N', '1', '2'].collect {
            new NhsDDCode(code: it, definition: it)
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['1', '2', 'N', 'Y']

    }

    void 'codes with web order set'() {

        when:
        List<NhsDDCode> codes = ['03', '02', '01', '00'].collect {
            new NhsDDCode(code: it, definition: it)
        }
        codes.eachWithIndex {NhsDDCode entry, int i ->
            entry.webOrder = i
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['03', '02', '01', '00']

        when:
        codes = ['Z', 'Y', 'X', 'W'].collect {
            new NhsDDCode(code: it, definition: it)
        }
        codes.eachWithIndex {NhsDDCode entry, int i ->
            entry.webOrder = i
        }

        then:
        NhsDDCode.sortCodes(codes)*.code == ['Z', 'Y', 'X', 'W']
    }

    void 'codes with web order partially set'() {

        when:
        List<NhsDDCode> codes = ['03', '02', '01', '00'].collect {
            new NhsDDCode(code: it, definition: it)
        }
        codes.find {it.code == '03'}.webOrder = 1

        then:
        NhsDDCode.sortCodes(codes)*.code == ['03', '00', '01', '02']

    }
}