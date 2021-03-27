package com.opencsv

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.channels.FileLockInterruptionException
import java.nio.charset.CharacterCodingException
import java.nio.charset.MalformedInputException
import java.util.zip.ZipException

class CSVReaderSpec extends Specification {

    @Unroll
    def 'When #exceptionClass is thrown the exception #will be caught'(Class exceptionClass, String will) {
        given:
        BufferedReader br = Stub(BufferedReader.class)
        IOException ioe = exceptionClass.equals(MalformedInputException.class) ? new MalformedInputException(128) : exceptionClass.newInstance()
        br.read() >> { throw ioe }
        CSVReaderBuilder builder = new CSVReaderBuilder(br)
        CSVReader reader = builder.build()
        boolean isCaught = "will".equals(will)
        boolean properlyHandled

        when:
        try {
            reader.readNext()
            properlyHandled = isCaught
        } catch (IOException e) {
            properlyHandled = !isCaught
        }

        then:
        will == "will" || will == "will not"
        properlyHandled == true

        where:
        exceptionClass                      | will
        CharacterCodingException.class      | "will not"
        CharConversionException.class       | "will not"
        UnsupportedEncodingException.class  | "will not"
        MalformedInputException.class | "will not"
        UTFDataFormatException.class        | "will not"
        ZipException.class                  | "will not"
        FileNotFoundException.class         | "will not"
        EOFException.class                  | "will"
        FileLockInterruptionException.class | "will"
        IOException.class                   | "will"

    }
}
