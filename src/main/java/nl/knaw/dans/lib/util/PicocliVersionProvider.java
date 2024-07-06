/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.IVersionProvider;

/**
 * Picocli version provider, that reads the version from the manifest file. Note, that this relies on getVersion being called from the main thread. When running the CLI with mvn exec:java, the version
 * will not be found. In that case the version would not be found anyway, as there is no jar file to read the manifest from. 
 */
@Slf4j
public class PicocliVersionProvider implements IVersionProvider {
    public String[] getVersion() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace.length > 0) {
            StackTraceElement mainMethod = trace[trace.length - 1];
            if ("main".equals(mainMethod.getMethodName())) {
                return new String[] { getImplementationVersion(trace[trace.length - 1]) };
            }
            else {
                log.warn("Main method not found in stack trace. Assuming this is a test run.");
            }
        }
        return new String[] { "Unable to determine version" };
    }

    private static String getImplementationVersion(StackTraceElement stackTraceElement) {
        try {
            Class<?> aClass = Class.forName(stackTraceElement.getClassName());
            String implementationVersion = aClass.getPackage().getImplementationVersion();
            if (implementationVersion == null) {
                // executed locally with start.sh (mvn exec:java)
                // unzip -p target/XXX.jar META-INF/MANIFEST.MF | grep '^Implementation-Version'
                log.warn("MANIFEST.MF not found in jar or it did not contain a value for Implementation-Version");
            }
            return implementationVersion;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
