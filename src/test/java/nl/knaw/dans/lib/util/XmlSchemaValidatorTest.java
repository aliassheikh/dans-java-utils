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

import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlSchemaValidatorTest {
    private DocumentBuilderFactory getFactory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        return factory;
    }

    @Test
    public void should_validate_conforming_standalone_document() throws Exception {
        // Given
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI(),
            "note", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/note.xsd")).toURI(),
            "imported", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/imported.xsd")).toURI()
        );
        var personXml = """
            <person xmlns="http://example.com/person">
                <name>John Doe</name>
                <age>30</age>
            </person>
            """;
        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(new StreamSource(new StringReader(personXml)), "person");

        assertThat(exceptions).isEmpty();
    }

    @Test
    public void should_validate_conforming_perparsed_standalone_document() throws Exception {
        // Given
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI(),
            "note", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/note.xsd")).toURI(),
            "imported", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/imported.xsd")).toURI()
        );
        var personXml = """
            <person xmlns="http://example.com/person">
                <name>John Doe</name>
                <age>30</age>
            </person>
            """;
        var personDocument = getFactory().newDocumentBuilder()
            .parse(new java.io.ByteArrayInputStream(personXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(personDocument.getDocumentElement(), "person");

        assertThat(exceptions).isEmpty();
    }

    @Test
    public void should_validate_conforming_document_with_import() throws Exception {
        // Given
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI(),
            "note", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/note.xsd")).toURI(),
            "message", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/imported.xsd")).toURI()
        );
        var messageXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message xmlns="http://example.com/message"
                     xmlns:p="http://example.com/person"
                     xmlns:n="http://example.com/note">
              <from>
                <p:person>
                  <p:name>John Doe</p:name>
                  <p:age>30</p:age>
                </p:person>
              </from>
              <to>
                <p:person>
                  <p:name>Jane Smith</p:name>
                  <p:age>28</p:age>
                </p:person>
              </to>
              <n:note>Hello, this is a note.</n:note>
            </message>
            
            """;
        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(new StreamSource(new StringReader(messageXml)), "message");
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void should_report_error_for_missing_required_element() throws Exception {
        // Given: missing <from> element
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI(),
            "note", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/note.xsd")).toURI(),
            "message", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/imported.xsd")).toURI()
        );
        var invalidXml = """
            <message xmlns="http://example.com/message"
                     xmlns:p="http://example.com/person"
                     xmlns:n="http://example.com/note">
              <to>
                <p:person>
                  <p:name>Jane Smith</p:name>
                  <p:age>28</p:age>
                </p:person>
              </to>
              <n:note>Missing from element</n:note>
            </message>
            """;

        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(new StreamSource(new StringReader(invalidXml)), "message");

        // Then
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.get(0).getMessage()).contains(
            "cvc-complex-type.2.4.a: Invalid content was found starting with element '{\"http://example.com/message\":to}'. One of '{\"http://example.com/message\":from}' is expected.");
    }

    @Test
    public void should_report_error_for_wrong_namespace() throws Exception {
        // Given: <person> element in default namespace instead of person namespace
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI(),
            "note", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/note.xsd")).toURI(),
            "message", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/imported.xsd")).toURI()
        );
        var invalidXml = """
            <message xmlns="http://example.com/message"
                     xmlns:p="http://example.com/person"
                     xmlns:n="http://example.com/note">
              <from>
                <person>
                  <name>John Doe</name>
                  <age>30</age>
                </person>
              </from>
              <to>cvc-complex-type.2.4.a: Invalid content was found starting with element '{"http://example.com/message":person}'. One of '{"http://example.com/person":person}' is expected.
                <p:person>
                  <p:name>Jane Smith</p:name>
                  <p:age>28</p:age>
                </p:person>
              </to>
              <n:note>Namespace error</n:note>
            </message>
            """;

        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(new StreamSource(new StringReader(invalidXml)), "message");

        // Then
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.get(0).getMessage()).contains(
            "cvc-complex-type.2.4.a: Invalid content was found starting with element '{\"http://example.com/message\":person}'. One of '{\"http://example.com/person\":person}' is expected.");
    }

    // An unknown alias should result in an IllegalStateException
    @Test
    public void should_throw_exception_for_unknown_schema_alias() throws Exception {
        // Given
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI()
        );
        var personXml = """
            <person xmlns="http://example.com/person">
                <name>John Doe</name>
                <age>30</age>
            </person>
            """;

        // When / Then
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        assertThatThrownBy(() -> validator.validateDocument(new StreamSource(new StringReader(personXml)), "unknown"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No schema instance found for alias 'unknown'");
    }

    @Test
    public void should_report_fatal_error_as_an_error() throws Exception {
        // Given: an XML with an extra, undefined attribute (should trigger a warning)
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "person", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/person.xsd")).toURI()
        );
        var personXml = """
            <person xmlns="http://example.com/person">
                <name>John Doe</name>
                <age>30</age>
            </UNMATCHED_TAG>
            """;

        // When
        var validator = new XmlSchemaValidator(aliasToSchemaLocation);
        var exceptions = validator.validateDocument(new StreamSource(new StringReader(personXml)), "person");

        // Then: the UNMATCHED_TAG should cause a fatal error
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.get(0).getMessage()).contains("The element type \"person\" must be terminated by the matching end-tag \"</person>\".");
    }

    @Test
    public void should_throw_illegal_argument_exception_for_null_schema_map() {
        assertThatThrownBy(() -> new XmlSchemaValidator(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("aliasToSchemaLocation is marked non-null but is null");
    }

    @Test
    public void should_throw_illegal_argument_exception_for_null_schema_location() {
        Map<String, URI> aliasToSchemaLocation = new HashMap<>();
        aliasToSchemaLocation.put("person", null);
        assertThatThrownBy(() -> new XmlSchemaValidator(aliasToSchemaLocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Schema location for alias 'person' is null");
    }

    @Test
    public void should_throw_runtime_exception_for_invalid_schema() throws Exception {
        Map<String, URI> aliasToSchemaLocation = Map.of(
            "invalid", Objects.requireNonNull(XmlSchemaValidatorTest.class.getResource("/XmlSchemaValidatorTest/invalid.xsd")).toURI()
        );
        assertThatThrownBy(() -> new XmlSchemaValidator(aliasToSchemaLocation))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Could not load schema for alias 'invalid'")
            .hasMessageContaining("Error resolving component 'xs:intege'"); // note the typo in 'integer'
    }

}