package io.swagger.codegen.options;

import java.util.HashMap;
import java.util.Map;

import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.languages.SpringCodegen;

public class SpringOptionsProvider extends JavaOptionsProvider {
    public static final String TITLE = "swagger";
    public static final String CONFIG_PACKAGE_VALUE = "configPackage";
    public static final String BASE_PACKAGE_VALUE = "basePackage";
    public static final String LIBRARY_VALUE = "spring-mvc"; // FIXME hidding value from super class
    public static final String INTERFACE_ONLY = "true";
    public static final String DELEGATE_PATTERN = "true";
    public static final String SINGLE_CONTENT_TYPES = "true";
    public static final String JAVA_8 = "true";
    public static final String ASYNC = "true";
    public static final String RESPONSE_WRAPPER = "Callable";
    public static final String USE_TAGS = "useTags";
    public static final String USE_BEANVALIDATION = "false";
    public static final String VERSION_SPRING = "4.3.4.RELEASE";
    public static final String VERSION_SPRINGFOX = "2.6.1";
    public static final String VERSION_JACKSON = "2.8.6";
    public static final String MODEL_PREFIX = "Api";
    public static final String MODEL_SUFFIX = "";

    @Override
    public Map<String, String> createOptions() {
        final Map<String, String> options = new HashMap<>(super.createOptions());
        options.put(SpringCodegen.TITLE, TITLE);
        options.put(SpringCodegen.CONFIG_PACKAGE, CONFIG_PACKAGE_VALUE);
        options.put(SpringCodegen.BASE_PACKAGE, BASE_PACKAGE_VALUE);
        options.put(CodegenConstants.LIBRARY, LIBRARY_VALUE);
        options.put(SpringCodegen.INTERFACE_ONLY, INTERFACE_ONLY);
        options.put(SpringCodegen.DELEGATE_PATTERN, DELEGATE_PATTERN);
        options.put(SpringCodegen.SINGLE_CONTENT_TYPES, SINGLE_CONTENT_TYPES);
        options.put(SpringCodegen.JAVA_8, JAVA_8);
        options.put(SpringCodegen.ASYNC, ASYNC);
        options.put(SpringCodegen.RESPONSE_WRAPPER, RESPONSE_WRAPPER);
        options.put(SpringCodegen.USE_TAGS, USE_TAGS);
        options.put(SpringCodegen.USE_BEANVALIDATION, USE_BEANVALIDATION);
        options.put(SpringCodegen.VERSION_JACKSON, VERSION_JACKSON);
        options.put(SpringCodegen.VERSION_SPRING, VERSION_SPRING);
        options.put(SpringCodegen.VERSION_SPRINGFOX, VERSION_SPRINGFOX);

        return options;
    }

    @Override
    public String getLanguage() {
        return "spring";
    }

    @Override
    public boolean isServer() {
        return true;
    }
}
