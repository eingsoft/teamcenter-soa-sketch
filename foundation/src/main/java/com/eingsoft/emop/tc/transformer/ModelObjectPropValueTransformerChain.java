package com.eingsoft.emop.tc.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.model.ModelObject;

/**
 * Global transformer registry to store {@link ModelObjectPropValueTransformer} instance, at the same time, provide
 * register/unregister transformers.
 * 
 * @author beam
 *
 */
@Log4j2
public class ModelObjectPropValueTransformerChain {

    private static ModelObjectPropValueTransformerChain chain = new ModelObjectPropValueTransformerChain();

    private ModelObjectPropValueTransformerChain() {}

    private Set<ModelObjectPropValueTransformer> propValueTransformers = new HashSet<ModelObjectPropValueTransformer>();

    public void registerGlobally(@NonNull ModelObjectPropValueTransformer transformer) {
        propValueTransformers.add(transformer);
    }

    public boolean unregisterGlobally(@NonNull ModelObjectPropValueTransformer transformer) {
        return propValueTransformers.remove(transformer);
    }

    public void registerRequestLevel(@NonNull ModelObjectPropValueTransformer transformer) {
        SOAExecutionContext.current().register(transformer);
    }

    public boolean unregisterRequestLevel(@NonNull ModelObjectPropValueTransformer transformer) {
        return SOAExecutionContext.current().unregister(transformer);
    }

    public static ModelObjectPropValueTransformerChain getInstance() {
        return chain;
    }

    /**
     * Transform the result to a new value
     */
    public Object transform(@NonNull ModelObject modelObject, @NonNull String propertyName, Object originalResult) {
        Set<ModelObjectPropValueTransformer> list = new HashSet<ModelObjectPropValueTransformer>();
        list.addAll(SOAExecutionContext.current().getTransformers());
        list.addAll(propValueTransformers);
        List<ModelObjectPropValueTransformer> transformers = new ArrayList<ModelObjectPropValueTransformer>(list);
        Collections.sort(transformers, Collections.reverseOrder());
        Object result = originalResult;
        for (ModelObjectPropValueTransformer transformer : transformers) {
            log.debug("try to transform result " + result + " using " + transformer);
            result = transformer.transform(modelObject, propertyName, result);
            log.debug("transformed result " + result);
        }
        return result;
    }

}
