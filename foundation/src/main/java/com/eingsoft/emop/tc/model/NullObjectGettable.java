package com.eingsoft.emop.tc.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;

import com.google.common.collect.Lists;
import com.teamcenter.soa.client.model.ModelObject;

import lombok.extern.log4j.Log4j2;

/**
 * @author beam
 * 
 *         represent the null object or empty list, contains the information
 *
 */
@Log4j2
public final class NullObjectGettable extends ArrayList implements Gettable {

    /**
     * 
     */
    private static final long serialVersionUID = 6304467669801772188L;

    private final String info;

    public NullObjectGettable(ModelObject obj, String propName) {
        this.info = obj.getTypeObject().getName() + "@" + obj.getUid() + ":" + propName;
    }

    public NullObjectGettable(String info) {
        this.info = info;
    }

    @Override
    public Object get(String propertyName) {
        if (Gettable.SKIP_NULL_OR_EMPTY.get() != null && Gettable.SKIP_NULL_OR_EMPTY.get()) {
            log.warn("encountered NullPointException(" + info + "->" + propertyName + "), skip it.");
            return new NullObjectGettable(info + "->" + propertyName);
        }
        return null;
    }

    @Override
    public Object get(int index) {
        log.warn("encountered IndexOutOfBoundsException(" + info + "[" + index + "]), skip it.");
        return new NullObjectGettable(info + "[" + index + "]");
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object getOptional(String propertyName) {
        log.warn("encountered NullPointException(" + info + "->" + propertyName + "), skip it.");
        return new NullObjectGettable(info + "->" + propertyName);
    }

    @Override
    public String getDisplayVal(String propertyName) {
        if (Gettable.SKIP_NULL_OR_EMPTY.get() != null && Gettable.SKIP_NULL_OR_EMPTY.get()) {
            log.warn("encountered NullPointException(" + info + "->" + propertyName + "), skip it.");
            return "";
        }
        return null;
    }
    
    @Override
    public List<String> getDisplayVals(String propertyName) {
        if (Gettable.SKIP_NULL_OR_EMPTY.get() != null && Gettable.SKIP_NULL_OR_EMPTY.get()) {
            log.warn("encountered NullPointException(" + info + "->" + propertyName + "), skip it.");
            return Lists.newArrayList();
        }
        return null;
    }

    @Override
    public String toString() {
        return Strings.EMPTY;
    }

    @Override
    public Object rel(String relationshipName, String... typeNames) {
        return new NullObjectGettable(info + "->" + relationshipName);
    }
    
    @Override
    public Object ref(String relationshipName, String... typeNames) {
        return new NullObjectGettable(info + "->" + relationshipName);
    }

    @Override
    public Object simpleGet(String propertyName) {
        return this.get(propertyName);
    }
}
