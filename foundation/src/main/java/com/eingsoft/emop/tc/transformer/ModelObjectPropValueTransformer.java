package com.eingsoft.emop.tc.transformer;

import lombok.NonNull;

import com.eingsoft.emop.tc.model.ModelObject;

/**
 * Transformer to handle property getter from {@link ModelObject}
 * 
 * @author beam
 *
 */
public interface ModelObjectPropValueTransformer extends Comparable<ModelObjectPropValueTransformer> {

    /**
     * the bigger one will be executed first
     */
    int getPriority();

    @Override
    default public int compareTo(ModelObjectPropValueTransformer o) {
        return this.getPriority() - o.getPriority();
    }

    /**
     * transformer name
     */
    String getName();

    /**
     * Transform the result to a new value, the ${originalResult} should be returned if it doesn't need do any
     * transformation in this instance.
     */
    Object transform(@NonNull ModelObject modelObject, @NonNull String propertyName, Object originalResult);
}
