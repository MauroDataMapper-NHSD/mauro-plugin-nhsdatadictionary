package uk.nhs.datadictionary.publish.website

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class GitHubStaticContentHelperService {

    static final String GITHUB_BRANCH_URL = "https://github.com/NHSDigital/DataDictionaryPublication/archive/refs/heads/master.zip"
    static final String GIHUB_DITA_FOLDER = "DataDictionaryPublication-master/Website/"


    static Map<String, ByteArrayOutputStream> getGithubDirAsMap(){

        // Get the zip file and save it into the directory
        InputStream inputStream = new URI(GITHUB_BRANCH_URL).toURL().openStream()
        zipStreamToMap(inputStream)

    }

    static Map<String, ByteArrayOutputStream> zipStreamToMap(InputStream zipInput) {
        byte[] buffer = new byte[8192]
        Map<String, ByteArrayOutputStream> result = [:]  // LinkedHashMap in Groovy

        zipInput.withCloseable { input ->
            new ZipInputStream(input).withCloseable { zis ->
                ZipEntry entry
                while ((entry = zis.nextEntry) != null) {
                    if (!entry.directory && entry.name.startsWith(GIHUB_DITA_FOLDER)) {
                        def baos = new ByteArrayOutputStream(
                            (entry.size > 0 && entry.size <= Integer.MAX_VALUE) ? (int) entry.size : 32
                        )
                        copyToBaos(zis, baos, buffer)
                        result[entry.name.replace(GIHUB_DITA_FOLDER, "")] = baos
                    }
                    zis.closeEntry()
                }
            }
        }
        result
    }

    private static void copyToBaos(InputStream input, ByteArrayOutputStream out, byte[] buffer) {
        for (int read = input.read(buffer); read != -1; read = input.read(buffer)) {
            out.write(buffer, 0, read)
        }
    }

}
