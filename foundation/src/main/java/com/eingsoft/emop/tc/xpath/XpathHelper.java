package com.eingsoft.emop.tc.xpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathIntrospector;
import com.eingsoft.emop.tc.xpath.ModelObjectPropertyHandler.ModelObjectBatchInvoker;
import lombok.NonNull;

public class XpathHelper {

    static {
        JXPathIntrospector.registerDynamicClass(com.teamcenter.soa.client.model.ModelObject.class,
            ModelObjectPropertyHandler.class);
    }
    
    private static Pattern variableNamePattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$");

    /**
     * Get the property through xpath expression, it returns single value, if the expression evaluated to multiply
     * values, return the first one.
     */
    public static @Nullable Object xpath(@NonNull Object target, @NonNull String expression) {
      try {
        validateExpression(expression);
        JXPathContext context = JXPathContext.newContext(target);
        context.setLenient(true);
        return context.getValue(expression);
      }finally {
        ModelObjectBatchInvoker.cleanup();
      }
    }
    
    /**
     * Get the property through xpath expression upon the given targets, it returns single value for each target, if the expression evaluated to multiply
     * values, return the first one, pay attention, the return list may contains null to keep the same size with targets.
     */
    public static List<?> xpathBatch(@NonNull List<? extends Object> targets, @NonNull String expression) {
      validateExpression(expression);
      JXPathContext context = JXPathContext.newContext(targets);
      context.setLenient(true);
      ModelObjectBatchInvoker.addModelObjects(targets);
      // preload all properties
      xpathValues(new CollectionShell(targets), "collection/" + expression);
      List<Object> result = new ArrayList<>();
      for (Object target : targets) {
        result.add(xpath(target, expression));
      }
      return result;
    }
    
    public static List<List<?>> xpathValuesBatch(@NonNull List<? extends Object> targets, @NonNull String expression) {
      validateExpression(expression);
      JXPathContext context = JXPathContext.newContext(targets);
      context.setLenient(true);
      ModelObjectBatchInvoker.addModelObjects(targets);
      // preload all properties
      xpathValues(new CollectionShell(targets), "collection/" + expression);
      List<List<?>> result = new ArrayList<>();
      for (Object target : targets) {
        result.add(xpathValues(target, expression));
      }
      return result;
    }
    
    /**
     * Get the property through xpath expression, it will return 
     */
    public static List<?> xpathValues(@NonNull Object target, @NonNull String expression) {
        validateExpression(expression);
        JXPathContext context = JXPathContext.newContext(target);
        context.setLenient(true);
        List<Object> result = new ArrayList<>();
        Iterator<?> it = context.iterate(expression);
        while (it.hasNext()) {
            Object o = it.next();
            if (o != null) {
                result.add(o);
            }
        }
        return result;
    }

    public static boolean isValidPropName(String propName) {
        return variableNamePattern.matcher(propName).matches();
    }

    private static void validateExpression(String expression) {
        if (expression.contains("//")) {
            throw new IllegalArgumentException("xpath(" + expression
                + ") contains '//' which may lead to performance issue.");
        }
    }
    
    public static class CollectionShell {
      private final List<? extends Object> collection;
      public CollectionShell(@NonNull List<? extends Object> collection) {
        this.collection = collection;
      }
      public List<? extends Object> getCollection() {
        return collection;
      }

    }
}
