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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container that keeps a number of configured XML Schema validators. Each validator has an alias. While it is possible to use the full namespace URI as alias, this is not required. For example, the
 * alias could be a simple name such as "ddm" or "emd".
 */
@Slf4j
public class XmlSchemaValidator {
    private final Map<String, URI> aliasToSchemaLocation;

    protected final Map<String, Schema> schemaMap = new HashMap<>();
    private final SchemaFactory schemaFactory;
    private final boolean failOnWarning;

    /**
     * Constructs an XmlSchemaValidator that can validate XML documents against multiple XML Schemas.
     *
     * @param aliasToSchemaLocation a map of schema aliases to schema locations (the URL of the XSD, not the namespace)
     */
    public XmlSchemaValidator(@NonNull Map<String, URI> aliasToSchemaLocation, boolean failOnWarning) {
        this.aliasToSchemaLocation = aliasToSchemaLocation;
        this.schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        this.failOnWarning = failOnWarning;
        this.loadSchemaInstances();
    }

    /**
     * Constructs an XmlSchemaValidator that can validate XML documents against multiple XML Schemas.
     *
     * @param aliasToSchemaLocation a map of schema aliases to schema locations (the URL of the XSD, not the namespace)
     */
    public XmlSchemaValidator(Map<String, URI> aliasToSchemaLocation) {
        this(aliasToSchemaLocation, false);
    }

    private void loadSchemaInstances() {
        for (var alias : aliasToSchemaLocation.keySet()) {
            log.debug("Start loading of schema instance for '{}'", alias);

            try {
                var schemaLocation = aliasToSchemaLocation.get(alias);

                if (schemaLocation != null) {
                    log.debug("Schema location for {} is: '{}'", alias, schemaLocation);
                    var schemaInstance = schemaFactory.newSchema(new URL(schemaLocation.toASCIIString()));
                    log.debug("Storing schema instance for '{}'", alias);
                    schemaMap.put(alias, schemaInstance);
                }
                else {
                    throw new IllegalArgumentException(String.format("Schema location for alias '%s' is null.", alias));
                }
                log.info("Schema instance for '{}' loaded.", alias);
            }
            catch (MalformedURLException | SAXException e) {
                // MalformedURLException should not occur, as URIs are converted to URLs above
                throw new RuntimeException("Could not load schema for alias '" + alias + "': " + e.getMessage(), e);
            }
        }
    }

    /**
     * Validates the document loaded into <code>node</code> against the schema mapped to <code>schemaAlias</code>. Note that a more complete validation can be performed by using a Source instead of a
     * Node, as fatal errors will already be raised when parsing the text into a DOM tree.
     *
     * @param node        the XML document to validate
     * @param schemaAlias the alias of the schema to validate against
     * @return a list of SAXParseExceptions encountered during validation; an empty list indicates the document is valid
     * @throws IOException  if the schema cannot be read
     * @throws SAXException if validation could not be performed
     */
    public List<SAXParseException> validateDocument(Node node, String schemaAlias) throws IOException, SAXException {
        return validateDocument(new DOMSource(node), schemaAlias);
    }

    /**
     * Validates the document loaded into <code>node</code> against the schema mapped to <code>schemaAlias</code>.
     *
     * @param source      the XML document to validate
     * @param schemaAlias the alias of the schema to validate against
     * @return a list of SAXParseExceptions encountered during validation; an empty list indicates the document is valid
     * @throws IOException  if the schema cannot be read
     * @throws SAXException if validation could not be performed
     */
    public List<SAXParseException> validateDocument(Source source, String schemaAlias) throws IOException, SAXException {
        var schemaInstance = schemaMap.get(schemaAlias);

        if (schemaInstance == null) {
            throw new IllegalStateException(String.format("No schema instance found for alias '%s'", schemaAlias));
        }

        var validator = schemaInstance.newValidator();
        log.debug("Creating validator for schema alias '{}'", schemaAlias);
        var exceptions = new ArrayList<SAXParseException>();

        validator.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning(SAXParseException e) {
                // Unsure how to test this; what constitutes a warning seems to be implementation-dependent
                if (failOnWarning) {
                    exceptions.add(e);
                }
            }

            @Override
            public void error(SAXParseException e) {
                exceptions.add(e);
            }

            @Override
            public void fatalError(SAXParseException e) {
                exceptions.add(e);
            }
        });

        log.debug("Validating document against schema alias '{}'", schemaAlias);
        try {
            validator.validate(source);
        }
        catch (SAXException e) {
            log.debug("Caught SAXException during validation: {}. It should be collected via the ErrorHandler, so we ignore it here.", e.getMessage());
        }

        log.debug("Validation against schema alias '{}' completed with {} exceptions", schemaAlias, exceptions.size());
        return exceptions;
    }

}
