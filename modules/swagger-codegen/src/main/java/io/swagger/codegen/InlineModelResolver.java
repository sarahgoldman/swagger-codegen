package io.swagger.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Xml;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.AbstractProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;

public class InlineModelResolver {
    static Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);
    private Swagger swagger;
    private boolean skipMatches;

    Map<String, Model> addedModels = new HashMap<>();
    Map<String, String> generatedSignature = new HashMap<>();

    public void addGenerated(final String name, final Model model) {
        generatedSignature.put(Json.pretty(model), name);
    }

    /**
     * Copy vendor extensions from Property to another Property
     *
     * @param source
     * @param target
     */
    public void copyVendorExtensions(final Property source, final AbstractProperty target) {
        final Map<String, Object> vendorExtensions = source.getVendorExtensions();
        for (final String extName : vendorExtensions.keySet()) {
            target.setVendorExtension(extName, vendorExtensions.get(extName));
        }
    }

    public void flatten(final Swagger swagger) {
        this.swagger = swagger;

        if (swagger.getDefinitions() == null) {
            swagger.setDefinitions(new HashMap<String, Model>());
        }

        // operations
        final Map<String, Path> paths = swagger.getPaths();
        final Map<String, Model> models = swagger.getDefinitions();

        if (paths != null) {
            for (final String pathname : paths.keySet()) {
                final Path path = paths.get(pathname);

                for (final Operation operation : path.getOperations()) {
                    final List<Parameter> parameters = operation.getParameters();

                    if (parameters != null) {
                        for (final Parameter parameter : parameters) {
                            if (parameter instanceof BodyParameter) {
                                final BodyParameter bp = (BodyParameter) parameter;
                                if (bp.getSchema() != null) {
                                    final Model model = bp.getSchema();
                                    if (model instanceof ModelImpl) {
                                        final ModelImpl obj = (ModelImpl) model;
                                        if (obj.getType() == null || "object".equals(obj.getType())) {
                                            if (obj.getProperties() != null && obj.getProperties().size() > 0) {
                                                flattenProperties(obj.getProperties(), pathname);
                                                final String modelName = resolveModelName(obj.getTitle(), bp.getName());
                                                bp.setSchema(new RefModel(modelName));
                                                addGenerated(modelName, model);
                                                swagger.addDefinition(modelName, model);
                                            }
                                        }
                                    } else if (model instanceof ArrayModel) {
                                        final ArrayModel am = (ArrayModel) model;
                                        final Property inner = am.getItems();

                                        if (inner instanceof ObjectProperty) {
                                            final ObjectProperty op = (ObjectProperty) inner;
                                            if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                flattenProperties(op.getProperties(), pathname);
                                                final String modelName = resolveModelName(op.getTitle(), bp.getName());
                                                final Model innerModel = modelFromProperty(op, modelName);
                                                final String existing = matchGenerated(innerModel);
                                                if (existing != null) {
                                                    am.setItems(new RefProperty(existing));
                                                } else {
                                                    am.setItems(new RefProperty(modelName));
                                                    addGenerated(modelName, innerModel);
                                                    swagger.addDefinition(modelName, innerModel);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    final Map<String, Response> responses = operation.getResponses();
                    if (responses != null) {
                        for (final String key : responses.keySet()) {
                            final Response response = responses.get(key);
                            if (response.getSchema() != null) {
                                final Property property = response.getSchema();
                                if (property instanceof ObjectProperty) {
                                    final ObjectProperty op = (ObjectProperty) property;
                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                        final String modelName = resolveModelName(op.getTitle(),
                                                operation.getOperationId() + "_" + key);
                                        final Model model = modelFromProperty(op, modelName);
                                        final String existing = matchGenerated(model);
                                        if (existing != null) {
                                            response.setSchema(this.makeRefProperty(existing, property));
                                        } else {
                                            response.setSchema(this.makeRefProperty(modelName, property));
                                            addGenerated(modelName, model);
                                            swagger.addDefinition(modelName, model);
                                        }
                                    }
                                } else if (property instanceof ArrayProperty) {
                                    final ArrayProperty ap = (ArrayProperty) property;
                                    final Property inner = ap.getItems();

                                    if (inner instanceof ObjectProperty) {
                                        final ObjectProperty op = (ObjectProperty) inner;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            final String modelName = resolveModelName(op.getTitle(),
                                                    operation.getOperationId() + "_" + key);
                                            final Model innerModel = modelFromProperty(op, modelName);
                                            final String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                ap.setItems(this.makeRefProperty(existing, op));
                                            } else {
                                                ap.setItems(this.makeRefProperty(modelName, op));
                                                addGenerated(modelName, innerModel);
                                                swagger.addDefinition(modelName, innerModel);
                                            }
                                        }
                                    }
                                } else if (property instanceof MapProperty) {
                                    final MapProperty mp = (MapProperty) property;

                                    final Property innerProperty = mp.getAdditionalProperties();
                                    if (innerProperty instanceof ObjectProperty) {
                                        final ObjectProperty op = (ObjectProperty) innerProperty;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            final String modelName = resolveModelName(op.getTitle(),
                                                    operation.getOperationId() + "_" + key);
                                            final Model innerModel = modelFromProperty(op, modelName);
                                            final String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                mp.setAdditionalProperties(new RefProperty(existing));
                                            } else {
                                                mp.setAdditionalProperties(new RefProperty(modelName));
                                                addGenerated(modelName, innerModel);
                                                swagger.addDefinition(modelName, innerModel);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // definitions
        if (models != null) {
            final List<String> modelNames = new ArrayList<>(models.keySet());
            for (final String modelName : modelNames) {
                final Model model = models.get(modelName);
                if (model instanceof ModelImpl) {
                    final ModelImpl m = (ModelImpl) model;

                    final Map<String, Property> properties = m.getProperties();
                    flattenProperties(properties, modelName);

                } else if (model instanceof ArrayModel) {
                    final ArrayModel m = (ArrayModel) model;
                    final Property inner = m.getItems();
                    if (inner instanceof ObjectProperty) {
                        final ObjectProperty op = (ObjectProperty) inner;
                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                            final String innerModelName = resolveModelName(op.getTitle(), modelName + "_inner");
                            final Model innerModel = modelFromProperty(op, innerModelName);
                            final String existing = matchGenerated(innerModel);
                            if (existing == null) {
                                swagger.addDefinition(innerModelName, innerModel);
                                addGenerated(innerModelName, innerModel);
                                m.setItems(new RefProperty(innerModelName));
                            } else {
                                m.setItems(new RefProperty(existing));
                            }
                        }
                    }
                } else if (model instanceof ComposedModel) {
                    final ComposedModel m = (ComposedModel) model;
                    if (m.getChild() != null) {
                        final Map<String, Property> properties = m.getChild().getProperties();
                        flattenProperties(properties, modelName);
                    }
                }
            }
        }
    }

    public void flattenProperties(final Map<String, Property> properties, final String path) {
        if (properties == null) {
            return;
        }
        final Map<String, Property> propsToUpdate = new HashMap<>();
        final Map<String, Model> modelsToAdd = new HashMap<>();
        for (final String key : properties.keySet()) {
            final Property property = properties.get(key);
            if (property instanceof ObjectProperty && ((ObjectProperty) property).getProperties() != null
                    && ((ObjectProperty) property).getProperties().size() > 0) {

                final ObjectProperty op = (ObjectProperty) property;

                final String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                final Model model = modelFromProperty(op, modelName);

                final String existing = matchGenerated(model);

                if (existing != null) {
                    propsToUpdate.put(key, new RefProperty(existing));
                } else {
                    propsToUpdate.put(key, new RefProperty(modelName));
                    modelsToAdd.put(modelName, model);
                    addGenerated(modelName, model);
                    swagger.addDefinition(modelName, model);
                }
            } else if (property instanceof ArrayProperty) {
                final ArrayProperty ap = (ArrayProperty) property;
                final Property inner = ap.getItems();

                if (inner instanceof ObjectProperty) {
                    final ObjectProperty op = (ObjectProperty) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        final String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        final Model innerModel = modelFromProperty(op, modelName);
                        final String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            ap.setItems(new RefProperty(existing));
                        } else {
                            ap.setItems(new RefProperty(modelName));
                            addGenerated(modelName, innerModel);
                            swagger.addDefinition(modelName, innerModel);
                        }
                    }
                }
            } else if (property instanceof MapProperty) {
                final MapProperty mp = (MapProperty) property;
                final Property inner = mp.getAdditionalProperties();

                if (inner instanceof ObjectProperty) {
                    final ObjectProperty op = (ObjectProperty) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        final String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        final Model innerModel = modelFromProperty(op, modelName);
                        final String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            mp.setAdditionalProperties(new RefProperty(existing));
                        } else {
                            mp.setAdditionalProperties(new RefProperty(modelName));
                            addGenerated(modelName, innerModel);
                            swagger.addDefinition(modelName, innerModel);
                        }
                    }
                }
            }
        }
        if (propsToUpdate.size() > 0) {
            for (final String key : propsToUpdate.keySet()) {
                properties.put(key, propsToUpdate.get(key));
            }
        }
        for (final String key : modelsToAdd.keySet()) {
            swagger.addDefinition(key, modelsToAdd.get(key));
            this.addedModels.put(key, modelsToAdd.get(key));
        }
    }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    /**
     * Make a RefProperty
     *
     * @param ref
     * @param property
     * @return
     */
    public Property makeRefProperty(final String ref, final Property property) {
        final RefProperty newProperty = new RefProperty(ref);
        this.copyVendorExtensions(property, newProperty);
        return newProperty;
    }

    public String matchGenerated(final Model model) {
        if (this.skipMatches) {
            return null;
        }
        final String json = Json.pretty(model);
        if (generatedSignature.containsKey(json)) {
            return generatedSignature.get(json);
        }
        return null;
    }

    @SuppressWarnings("static-method")
    public Model modelFromProperty(final ArrayProperty object, @SuppressWarnings("unused") final String path) {
        final String description = object.getDescription();
        String example = null;

        final Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        final Property inner = object.getItems();
        if (inner instanceof ObjectProperty) {
            final ArrayModel model = new ArrayModel();
            model.setDescription(description);
            model.setExample(example);
            model.setItems(object.getItems());
            return model;
        }

        return null;
    }

    @SuppressWarnings("static-method")
    public Model modelFromProperty(final MapProperty object, @SuppressWarnings("unused") final String path) {
        final String description = object.getDescription();
        String example = null;

        final Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        final ArrayModel model = new ArrayModel();
        model.setDescription(description);
        model.setExample(example);
        model.setItems(object.getAdditionalProperties());

        return model;
    }

    public Model modelFromProperty(final ObjectProperty object, final String path) {
        final String description = object.getDescription();
        String example = null;

        final Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        final String name = object.getName();
        final Xml xml = object.getXml();
        final Map<String, Property> properties = object.getProperties();

        final ModelImpl model = new ModelImpl();
        model.setDescription(description);
        model.setExample(example);
        model.setName(name);
        model.setXml(xml);

        if (properties != null) {
            flattenProperties(properties, path);
            model.setProperties(properties);
        }

        return model;
    }

    private String resolveModelName(final String title, final String key) {
        if (title == null) {
            return uniqueName(key);
        } else {
            return uniqueName(title);
        }
    }

    public void setSkipMatches(final boolean skipMatches) {
        this.skipMatches = skipMatches;
    }

    public String uniqueName(String key) {
        int count = 0;
        final boolean done = false;
        key = key.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
                                                       // should not be
                                                       // assigned. Also declare
                                                       // the methods parameters
                                                       // as 'final'.
        while (!done) {
            String name = key;
            if (count > 0) {
                name = key + "_" + count;
            }
            if (swagger.getDefinitions() == null) {
                return name;
            } else if (!swagger.getDefinitions().containsKey(name)) {
                return name;
            }
            count += 1;
        }
    }

}
