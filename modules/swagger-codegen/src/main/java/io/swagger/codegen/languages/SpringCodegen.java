package io.swagger.codegen.languages;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenResponse;
import io.swagger.codegen.CodegenSecurity;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.features.BeanValidationFeatures;
import io.swagger.codegen.languages.features.OptionalFeatures;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

public class SpringCodegen extends AbstractJavaCodegen implements BeanValidationFeatures, OptionalFeatures {
    private interface DataTypeAssigner {
        void setReturnContainer(String returnContainer);

        void setReturnType(String returnType);
    }

    public static final String DEFAULT_LIBRARY = "spring-boot";
    public static final String TITLE = "title";
    public static final String CONFIG_PACKAGE = "configPackage";
    public static final String BASE_PACKAGE = "basePackage";
    public static final String INTERFACE_ONLY = "interfaceOnly";
    public static final String DELEGATE_PATTERN = "delegatePattern";
    public static final String SINGLE_CONTENT_TYPES = "singleContentTypes";
    public static final String JAVA_8 = "java8";
    public static final String ASYNC = "async";
    public static final String RESPONSE_WRAPPER = "responseWrapper";
    public static final String USE_TAGS = "useTags";
    public static final String SPRING_MVC_LIBRARY = "spring-mvc";
    public static final String SPRING_CLOUD_LIBRARY = "spring-cloud";
    public static final String IMPLICIT_HEADERS = "implicitHeaders";
    public static final String SWAGGER_DOCKET_CONFIG = "swaggerDocketConfig";
    public static final String VERSION_SPRING = "springVersion";
    public static final String VERSION_SPRINGFOX = "springfoxVersion";
    public static final String VERSION_JACKSON = "jacksonVersion";
    public static final String DIST_RELEASE_REPO_URL = "releaseDistRepoUrl";
    public static final String DIST_SNAPSHOT_REPO_URL = "snapshotDistRepoUrl";
    public static final String REPO_URL = "repoUrl";
    public static final String PLUGIN_REPO_URL = "pluginRepoUrl";
    public static final String USE_JETTY_PLUGIN = "useJetty";
    public static final String VERSION_DESCRIPTION = "versionDescription";

    protected String title = "swagger-petstore";
    protected String configPackage = "io.swagger.configuration";
    protected String basePackage = "io.swagger";
    protected boolean interfaceOnly = false;
    protected boolean delegatePattern = false;
    protected boolean delegateMethod = false;
    protected boolean singleContentTypes = false;
    protected boolean java8 = false;
    protected boolean async = false;
    protected String responseWrapper = "";
    protected boolean useTags = false;
    protected boolean useBeanValidation = true;
    protected boolean implicitHeaders = false;
    protected boolean swaggerDocketConfig = false;
    protected boolean useOptional = false;
    protected String springVersion = "4.3.4.RELEASE";
    protected String springfoxVersion = "2.6.1";
    protected String jacksonVersion = "2.8.6";
    protected boolean useJetty = false;

    public SpringCodegen() {
        super();
        outputFolder = "generated-code/javaSpring";
        apiTestTemplateFiles.clear(); // TODO: add test template
        embeddedTemplateDir = templateDir = "JavaSpring";
        apiPackage = "io.swagger.api";
        modelPackage = "io.swagger.model";
        invokerPackage = "io.swagger.api";
        artifactId = "swagger-spring";

        additionalProperties.put(CONFIG_PACKAGE, configPackage);
        additionalProperties.put(BASE_PACKAGE, basePackage);

        // spring uses the jackson lib
        additionalProperties.put("jackson", "true");

        cliOptions.add(new CliOption(TITLE, "server title name or client service name"));
        cliOptions.add(new CliOption(CONFIG_PACKAGE, "configuration package for generated code"));
        cliOptions.add(new CliOption(BASE_PACKAGE, "base package (invokerPackage) for generated code"));
        cliOptions.add(CliOption.newBoolean(INTERFACE_ONLY,
                "Whether to generate only API interface stubs without the server files."));
        cliOptions.add(CliOption.newBoolean(DELEGATE_PATTERN,
                "Whether to generate the server files using the delegate pattern"));
        cliOptions.add(CliOption.newBoolean(SINGLE_CONTENT_TYPES,
                "Whether to select only one produces/consumes content-type by operation."));
        cliOptions.add(CliOption.newBoolean(JAVA_8, "use java8 default interface"));
        cliOptions.add(CliOption.newBoolean(ASYNC, "use async Callable controllers"));
        cliOptions.add(new CliOption(RESPONSE_WRAPPER,
                "wrap the responses in given type (Future,Callable,CompletableFuture,ListenableFuture,DeferredResult,HystrixCommand,RxObservable,RxSingle or fully qualified type)"));
        cliOptions.add(CliOption.newBoolean(USE_TAGS, "use tags for creating interface and controller classnames"));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        cliOptions.add(CliOption.newBoolean(IMPLICIT_HEADERS, "Use of @ApiImplicitParams for headers."));
        cliOptions.add(
                CliOption.newBoolean(SWAGGER_DOCKET_CONFIG, "Generate Spring Swagger Docket configuration class."));
        cliOptions.add(CliOption.newBoolean(USE_OPTIONAL, "Use Optional container for optional parameters"));
        cliOptions.add(new CliOption(VERSION_SPRING, "version of the Spring Framework to use"));
        cliOptions.add(new CliOption(VERSION_SPRINGFOX, "version of the SpringFox library to use"));
        cliOptions.add(new CliOption(VERSION_JACKSON, "version of the Jackson library to use"));
        cliOptions.add(new CliOption(DIST_RELEASE_REPO_URL, "URL for release repo"));
        cliOptions.add(new CliOption(DIST_SNAPSHOT_REPO_URL, "URL for snapshot repo"));
        cliOptions.add(new CliOption(REPO_URL, "URL for dependency repo"));
        cliOptions.add(new CliOption(PLUGIN_REPO_URL, "URL for plugin dependency repo"));
        cliOptions.add(new CliOption(USE_JETTY_PLUGIN, "use Jetty plugin for integration test"));
        cliOptions.add(new CliOption(VERSION_DESCRIPTION, "Description used in the @version tag at the class level"));

        supportedLibraries.put(DEFAULT_LIBRARY, "Spring-boot Server application using the SpringFox integration.");
        supportedLibraries.put(SPRING_MVC_LIBRARY, "Spring-MVC Server application using the SpringFox integration.");
        supportedLibraries.put(SPRING_CLOUD_LIBRARY,
                "Spring-Cloud-Feign client with Spring-Boot auto-configured settings.");
        setLibrary(DEFAULT_LIBRARY);

        final CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        library.setDefault(DEFAULT_LIBRARY);
        library.setEnum(supportedLibraries);
        library.setDefault(DEFAULT_LIBRARY);
        cliOptions.add(library);
    }

    @Override
    public void addOperationToGroup(final String tag, final String resourcePath, final Operation operation,
            final CodegenOperation co, final Map<String, List<CodegenOperation>> operations) {
        if ((library.equals(DEFAULT_LIBRARY) || library.equals(SPRING_MVC_LIBRARY)) && !useTags) {
            String basePath = resourcePath;
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            final int pos = basePath.indexOf("/");
            if (pos > 0) {
                basePath = basePath.substring(0, pos);
            }

            if (basePath.equals("")) {
                basePath = "default";
            } else {
                co.subresourceOperation = !co.path.isEmpty();
            }
            List<CodegenOperation> opList = operations.get(basePath);
            if (opList == null) {
                opList = new ArrayList<>();
                operations.put(basePath, opList);
            }
            opList.add(co);
            co.baseName = basePath;
        } else {
            super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        }
    }

    /**
     * @param returnType
     *            The return type that needs to be converted
     * @param dataTypeAssigner
     *            An object that will assign the data to the respective fields in the model.
     */
    private void doDataTypeAssignment(final String returnType, final DataTypeAssigner dataTypeAssigner) {
        final String rt = returnType;
        if (rt == null) {
            dataTypeAssigner.setReturnType("Void");
        } else if (rt.startsWith("List")) {
            final int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("List<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("List");
            }
        } else if (rt.startsWith("Map")) {
            final int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Map<".length(), end).split(",")[1].trim());
                dataTypeAssigner.setReturnContainer("Map");
            }
        } else if (rt.startsWith("Set")) {
            final int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Set<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("Set");
            }
        }
    }

    @Override
    public String getHelp() {
        return "Generates a Java SpringBoot Server application using the SpringFox integration.";
    }

    public String getJacksonVersion() {
        return jacksonVersion;
    }

    @Override
    public String getName() {
        return "spring";
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if ("null".equals(property.example)) {
            property.example = null;
        }

        // Add imports for Jackson
        if (!Boolean.TRUE.equals(model.isEnum)) {
            model.imports.add("JsonProperty");

            if (Boolean.TRUE.equals(model.hasEnums)) {
                model.imports.add("JsonValue");
            }
        } else { // enum class
            // Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonCreator");
            }
        }
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);

        // Add imports for Jackson
        final List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final List<Object> models = (List<Object>) objs.get("models");
        for (final Object _mo : models) {
            final Map<String, Object> mo = (Map<String, Object>) _mo;
            final CodegenModel cm = (CodegenModel) mo.get("model");
            // for enum model
            if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                cm.imports.add(importMapping.get("JsonValue"));
                final Map<String, String> item = new HashMap<>();
                item.put("import", importMapping.get("JsonValue"));
                imports.add(item);
            }
        }

        return objs;
    }

    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        final Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            final List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (final CodegenOperation operation : ops) {
                final List<CodegenResponse> responses = operation.responses;
                if (responses != null) {
                    for (final CodegenResponse resp : responses) {
                        if ("0".equals(resp.code)) {
                            resp.code = "200";
                        }
                        doDataTypeAssignment(resp.dataType, new DataTypeAssigner() {
                            @Override
                            public void setReturnContainer(final String returnContainer) {
                                resp.containerType = returnContainer;
                            }

                            @Override
                            public void setReturnType(final String returnType) {
                                resp.dataType = returnType;
                            }
                        });
                    }
                }

                doDataTypeAssignment(operation.returnType, new DataTypeAssigner() {

                    @Override
                    public void setReturnContainer(final String returnContainer) {
                        operation.returnContainer = returnContainer;
                    }

                    @Override
                    public void setReturnType(final String returnType) {
                        operation.returnType = returnType;
                    }
                });

                if (implicitHeaders) {
                    removeHeadersFromAllParams(operation.allParams);
                }
            }
        }

        return objs;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(final Map<String, Object> objs) {
        if (library.equals(SPRING_CLOUD_LIBRARY)) {
            final List<CodegenSecurity> authMethods = (List<CodegenSecurity>) objs.get("authMethods");
            if (authMethods != null) {
                for (final CodegenSecurity authMethod : authMethods) {
                    authMethod.name = camelize(sanitizeName(authMethod.name), true);
                }
            }
        }
        return objs;
    }

    @Override
    public void preprocessSwagger(final Swagger swagger) {
        super.preprocessSwagger(swagger);
        if ("/".equals(swagger.getBasePath())) {
            swagger.setBasePath("");
        }

        if (!additionalProperties.containsKey(TITLE)) {
            // From the title, compute a reasonable name for the package and the API
            String title = swagger.getInfo().getTitle();

            // Drop any API suffix
            if (title != null) {
                title = title.trim().replace(" ", "-");
                if (title.toUpperCase().endsWith("API")) {
                    title = title.substring(0, title.length() - 3);
                }

                this.title = camelize(sanitizeName(title), true);
            }
            additionalProperties.put(TITLE, this.title);
        }

        final String host = swagger.getHost();
        String port = "8080";
        if (host != null) {
            final String[] parts = host.split(":");
            if (parts.length > 1) {
                port = parts[1];
            }
        }

        this.additionalProperties.put("serverPort", port);
        if (swagger.getPaths() != null) {
            for (final String pathname : swagger.getPaths().keySet()) {
                final Path path = swagger.getPath(pathname);
                if (path.getOperations() != null) {
                    for (final Operation operation : path.getOperations()) {
                        if (operation.getTags() != null) {
                            final List<Map<String, String>> tags = new ArrayList<>();
                            for (final String tag : operation.getTags()) {
                                final Map<String, String> value = new HashMap<>();
                                value.put("tag", tag);
                                value.put("hasMore", "true");
                                tags.add(value);
                            }
                            if (tags.size() > 0) {
                                tags.get(tags.size() - 1).remove("hasMore");
                            }
                            if (operation.getTags().size() > 0) {
                                final String tag = operation.getTags().get(0);
                                operation.setTags(Arrays.asList(tag));
                            }
                            operation.setVendorExtension("x-tags", tags);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void processOpts() {

        // Process java8 option before common java ones to change the default dateLibrary to java8.
        if (additionalProperties.containsKey(JAVA_8)) {
            this.setJava8(Boolean.valueOf(additionalProperties.get(JAVA_8).toString()));
        }
        if (this.java8) {
            additionalProperties.put("javaVersion", "1.8");
            additionalProperties.put("jdk8", "true");
            if (!additionalProperties.containsKey(DATE_LIBRARY)) {
                setDateLibrary("java8");
            }
        }

        // set invokerPackage as basePackage
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setBasePackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            additionalProperties.put(BASE_PACKAGE, basePackage);
            LOGGER.info("Set base package to invoker package (" + basePackage + ")");
        }

        super.processOpts();

        // clear model and api doc template as this codegen
        // does not support auto-generated markdown doc at the moment
        // TODO: add doc templates
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");

        if (additionalProperties.containsKey(TITLE)) {
            this.setTitle((String) additionalProperties.get(TITLE));
        }

        if (additionalProperties.containsKey(CONFIG_PACKAGE)) {
            this.setConfigPackage((String) additionalProperties.get(CONFIG_PACKAGE));
        }

        if (additionalProperties.containsKey(BASE_PACKAGE)) {
            this.setBasePackage((String) additionalProperties.get(BASE_PACKAGE));
        }

        if (additionalProperties.containsKey(INTERFACE_ONLY)) {
            this.setInterfaceOnly(Boolean.valueOf(additionalProperties.get(INTERFACE_ONLY).toString()));
        }

        if (additionalProperties.containsKey(DELEGATE_PATTERN)) {
            this.setDelegatePattern(Boolean.valueOf(additionalProperties.get(DELEGATE_PATTERN).toString()));
        }

        if (additionalProperties.containsKey(SINGLE_CONTENT_TYPES)) {
            this.setSingleContentTypes(Boolean.valueOf(additionalProperties.get(SINGLE_CONTENT_TYPES).toString()));
        }

        if (additionalProperties.containsKey(JAVA_8)) {
            this.setJava8(Boolean.valueOf(additionalProperties.get(JAVA_8).toString()));
        }

        if (additionalProperties.containsKey(ASYNC)) {
            this.setAsync(Boolean.valueOf(additionalProperties.get(ASYNC).toString()));
        }

        if (additionalProperties.containsKey(RESPONSE_WRAPPER)) {
            this.setResponseWrapper((String) additionalProperties.get(RESPONSE_WRAPPER));
        }

        if (additionalProperties.containsKey(USE_TAGS)) {
            this.setUseTags(Boolean.valueOf(additionalProperties.get(USE_TAGS).toString()));
        }

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBoolean(USE_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(USE_OPTIONAL)) {
            this.setUseOptional(convertPropertyToBoolean(USE_OPTIONAL));
        }

        if (useBeanValidation) {
            writePropertyBack(USE_BEANVALIDATION, useBeanValidation);
        }

        if (additionalProperties.containsKey(IMPLICIT_HEADERS)) {
            this.setImplicitHeaders(Boolean.valueOf(additionalProperties.get(IMPLICIT_HEADERS).toString()));
        }

        if (additionalProperties.containsKey(SWAGGER_DOCKET_CONFIG)) {
            this.setSwaggerDocketConfig(Boolean.valueOf(additionalProperties.get(SWAGGER_DOCKET_CONFIG).toString()));
        }

        if (additionalProperties.containsKey(USE_JETTY_PLUGIN)) {
            this.setUseJetty(Boolean.valueOf(additionalProperties.get(USE_JETTY_PLUGIN).toString()));
        }

        if (additionalProperties.containsKey(VERSION_JACKSON)) {
            this.setJacksonVersion((String) additionalProperties.get(VERSION_JACKSON));
        }

        if (additionalProperties.containsKey(VERSION_SPRING)) {
            this.setSpringVersion((String) additionalProperties.get(VERSION_SPRING));
        }

        if (additionalProperties.containsKey(VERSION_SPRINGFOX)) {
            this.setSpringfoxVersion((String) additionalProperties.get(VERSION_SPRINGFOX));
        }

        typeMapping.put("file", "Resource");
        importMapping.put("Resource", "org.springframework.core.io.Resource");

        if (useOptional) {
            writePropertyBack(USE_OPTIONAL, useOptional);
        }

        if (this.interfaceOnly && this.delegatePattern) {
            if (this.java8) {
                this.delegateMethod = true;
                additionalProperties.put("delegate-method", true);
            } else {
                throw new IllegalArgumentException(
                        String.format("Can not generate code with `%s` and `%s` true while `%s` is false.",
                                DELEGATE_PATTERN, INTERFACE_ONLY, JAVA_8));
            }
        }

        if (!this.interfaceOnly) {
            supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
            supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

            if (library.equals(DEFAULT_LIBRARY)) {
                supportingFiles.add(new SupportingFile("homeController.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "HomeController.java"));
                supportingFiles.add(new SupportingFile("swagger2SpringBoot.mustache",
                        (sourceFolder + File.separator + basePackage).replace(".", java.io.File.separator),
                        "Swagger2SpringBoot.java"));
                supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache",
                        (sourceFolder + File.separator + basePackage).replace(".", java.io.File.separator),
                        "RFC3339DateFormat.java"));
                supportingFiles.add(new SupportingFile("application.mustache",
                        ("src.main.resources").replace(".", java.io.File.separator), "application.properties"));
            }
            if (library.equals(SPRING_MVC_LIBRARY)) {
                supportingFiles.add(new SupportingFile("webApplication.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "WebApplication.java"));
                supportingFiles.add(new SupportingFile("webMvcConfiguration.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "WebMvcConfiguration.java"));
                supportingFiles.add(new SupportingFile("swaggerUiConfiguration.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "SwaggerUiConfiguration.java"));
                supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "RFC3339DateFormat.java"));
                supportingFiles.add(new SupportingFile("application.properties",
                        ("src.main.resources").replace(".", java.io.File.separator), "swagger.properties"));
            }
            if (library.equals(SPRING_CLOUD_LIBRARY)) {
                supportingFiles.add(new SupportingFile("apiKeyRequestInterceptor.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "ApiKeyRequestInterceptor.java"));
                supportingFiles.add(new SupportingFile("clientConfiguration.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "ClientConfiguration.java"));
                apiTemplateFiles.put("apiClient.mustache", "Client.java");
                if (!additionalProperties.containsKey(SINGLE_CONTENT_TYPES)) {
                    additionalProperties.put(SINGLE_CONTENT_TYPES, "true");
                    this.setSingleContentTypes(true);
                }
            } else {
                apiTemplateFiles.put("apiController.mustache", "Controller.java");
                supportingFiles.add(new SupportingFile("apiException.mustache",
                        (sourceFolder + File.separator + apiPackage).replace(".", java.io.File.separator),
                        "ApiException.java"));
                supportingFiles.add(new SupportingFile("apiResponseMessage.mustache",
                        (sourceFolder + File.separator + apiPackage).replace(".", java.io.File.separator),
                        "ApiResponseMessage.java"));
                supportingFiles.add(new SupportingFile("notFoundException.mustache",
                        (sourceFolder + File.separator + apiPackage).replace(".", java.io.File.separator),
                        "NotFoundException.java"));
                supportingFiles.add(new SupportingFile("apiOriginFilter.mustache",
                        (sourceFolder + File.separator + apiPackage).replace(".", java.io.File.separator),
                        "ApiOriginFilter.java"));
                supportingFiles.add(new SupportingFile("swaggerDocumentationConfig.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "SwaggerDocumentationConfig.java"));
            }
        } else if (this.swaggerDocketConfig && !library.equals(SPRING_CLOUD_LIBRARY)) {
            supportingFiles.add(new SupportingFile("swaggerDocumentationConfig.mustache",
                    (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                    "SwaggerDocumentationConfig.java"));
        }

        if ("threetenbp".equals(dateLibrary)) {
            supportingFiles.add(new SupportingFile("customInstantDeserializer.mustache",
                    (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                    "CustomInstantDeserializer.java"));
            if (library.equals(DEFAULT_LIBRARY) || library.equals(SPRING_CLOUD_LIBRARY)) {
                supportingFiles.add(new SupportingFile("jacksonConfiguration.mustache",
                        (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator),
                        "JacksonConfiguration.java"));
            }
        }

        if ((!this.delegatePattern && this.java8) || this.delegateMethod) {
            additionalProperties.put("jdk8-no-delegate", true);
        }


        if (this.delegatePattern && !this.delegateMethod) {
            additionalProperties.put("isDelegate", "true");
            apiTemplateFiles.put("apiDelegate.mustache", "Delegate.java");
        }

        if (this.java8) {
            additionalProperties.put("javaVersion", "1.8");
            additionalProperties.put("jdk8", "true");
            if (this.async) {
                additionalProperties.put(RESPONSE_WRAPPER, "CompletableFuture");
            }
        } else if (this.async) {
            additionalProperties.put(RESPONSE_WRAPPER, "Callable");
        }

        // Some well-known Spring or Spring-Cloud response wrappers
        switch (this.responseWrapper) {
            case "Future":
            case "Callable":
            case "CompletableFuture":
                additionalProperties.put(RESPONSE_WRAPPER, "java.util.concurrent" + this.responseWrapper);
                break;
            case "ListenableFuture":
                additionalProperties.put(RESPONSE_WRAPPER, "org.springframework.util.concurrent.ListenableFuture");
                break;
            case "DeferredResult":
                additionalProperties.put(RESPONSE_WRAPPER,
                        "org.springframework.web.context.request.async.DeferredResult");
                break;
            case "HystrixCommand":
                additionalProperties.put(RESPONSE_WRAPPER, "com.netflix.hystrix.HystrixCommand");
                break;
            case "RxObservable":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Observable");
                break;
            case "RxSingle":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Single");
                break;
            default:
                break;
        }

        supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

        // add lambda for mustache templates
        additionalProperties.put("lambdaEscapeDoubleQuote", new Mustache.Lambda() {
            @Override
            public void execute(final Template.Fragment fragment, final Writer writer) throws IOException {
                writer.write(fragment.execute().replaceAll("\"", Matcher.quoteReplacement("\\\"")));
            }
        });
        additionalProperties.put("lambdaRemoveLineBreak", new Mustache.Lambda() {
            @Override
            public void execute(final Template.Fragment fragment, final Writer writer) throws IOException {
                writer.write(fragment.execute().replaceAll("\\r|\\n", ""));
            }
        });
    }

    /**
     * This method removes header parameters from the list of parameters and also corrects last allParams hasMore state.
     *
     * @param allParams
     *            list of all parameters
     */
    private void removeHeadersFromAllParams(final List<CodegenParameter> allParams) {
        if (allParams.isEmpty()) {
            return;
        }
        final ArrayList<CodegenParameter> copy = new ArrayList<>(allParams);
        allParams.clear();

        for (final CodegenParameter p : copy) {
            if (!p.isHeaderParam) {
                allParams.add(p);
            }
        }
        allParams.get(allParams.size() - 1).hasMore = false;
    }

    public void setAsync(final boolean async) {
        this.async = async;
    }

    public void setBasePackage(final String configPackage) {
        this.basePackage = configPackage;
    }

    public void setConfigPackage(final String configPackage) {
        this.configPackage = configPackage;
    }

    public void setDelegatePattern(final boolean delegatePattern) {
        this.delegatePattern = delegatePattern;
    }

    public void setImplicitHeaders(final boolean implicitHeaders) {
        this.implicitHeaders = implicitHeaders;
    }

    public void setInterfaceOnly(final boolean interfaceOnly) {
        this.interfaceOnly = interfaceOnly;
    }

    public void setJacksonVersion(final String jacksonVersion) {
        this.jacksonVersion = jacksonVersion;
    }

    public void setJava8(final boolean java8) {
        this.java8 = java8;
    }

    @Override
    public void setParameterExampleValue(final CodegenParameter p) {
        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("File".equals(type)) {
            String example;

            if (p.defaultValue == null) {
                example = p.example;
            } else {
                example = p.defaultValue;
            }

            if (example == null) {
                example = "/path/to/file";
            }
            example = "new org.springframework.core.io.FileSystemResource(new java.io.File(\"" + escapeText(example)
                    + "\"))";
            p.example = example;
        } else {
            super.setParameterExampleValue(p);
        }
    }

    public void setResponseWrapper(final String responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    public void setSingleContentTypes(final boolean singleContentTypes) {
        this.singleContentTypes = singleContentTypes;
    }

    public void setSpringfoxVersion(final String springfoxVersion) {
        this.springfoxVersion = springfoxVersion;
    }

    public void setSpringVersion(final String springVersion) {
        this.springVersion = springVersion;
    }

    public void setSwaggerDocketConfig(final boolean swaggerDocketConfig) {
        this.swaggerDocketConfig = swaggerDocketConfig;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public void setUseBeanValidation(final boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    public void setUseJetty(final boolean useJetty) {
        this.useJetty = useJetty;
    }

    @Override
    public void setUseOptional(final boolean useOptional) {
        this.useOptional = useOptional;
    }

    public void setUseTags(final boolean useTags) {
        this.useTags = useTags;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        name = sanitizeName(name);
        return camelize(name) + "Api";
    }
}
