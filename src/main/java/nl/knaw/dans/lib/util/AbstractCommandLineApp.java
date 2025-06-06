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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.Configuration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.Generics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Slf4j
public abstract class AbstractCommandLineApp<C extends Configuration> implements Callable<Integer> {
    public static String CONFIG_FILE_KEY = "dans.default.config";

    /**
     * Path from which an example configuration file can be loaded, to be instantiated with the default configuration, if no configuration file is found.
     */
    public static String EXAMPLE_CONFIG_FILE_KEY = "dans.default.example.config";

    public void run(String[] args) throws IOException, ConfigurationException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        File configFile = new File(System.getProperty(CONFIG_FILE_KEY));
        if (!configFile.exists()) {
            Path exampleConfigFile = Paths.get(System.getProperty(EXAMPLE_CONFIG_FILE_KEY));
            FileUtils.copyFile(exampleConfigFile.toFile(), configFile);
            System.err.println("Configuration file not found, copied example configuration file to " + configFile.getAbsolutePath());
        }
        C config = loadConfiguration(configFile);
        MetricRegistry metricRegistry = new MetricRegistry();
        config.getLoggingFactory().configure(metricRegistry, getName());
        CommandLine commandLine = new CommandLine(this);
        configureCommandLine(commandLine, config);
        commandLine.addSubcommand(new GenerateCompletion());
        try {
            commandLine.execute(args);
        }
        catch (Exception e) {
            log.error("Error running command", e);
            flushAndStopLoggerContext();
            System.exit(1);
        }
        finally {
            flushAndStopLoggerContext();
        }
        System.exit(0);
    }

    private void flushAndStopLoggerContext() {
        // Otherwise System.exit may cut off the log messages
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }

    public C loadConfiguration(File configFile) throws ConfigurationException, IOException {
        Validator validator = Validators.newValidator();
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());

        SubstitutingSourceProvider sourceProvider = new SubstitutingSourceProvider(
            new FileConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        );

        ConfigurationFactory<C> configurationFactory = new YamlConfigurationFactory<>(Generics.getTypeParameter(getClass(), Configuration.class), validator, objectMapper, "dans");
        return configurationFactory.build(sourceProvider, configFile.getPath());
    }

    public abstract String getName();

    public abstract void configureCommandLine(CommandLine commandLine, C config);

    public Integer call() throws Exception {
        return 0;
    }

}
