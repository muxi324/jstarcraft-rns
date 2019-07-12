package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.rns.search.annotation.RetrievalAnalyze;
import com.jstarcraft.rns.search.annotation.RetrievalIndex;
import com.jstarcraft.rns.search.annotation.RetrievalSort;
import com.jstarcraft.rns.search.annotation.RetrievalStore;
import com.jstarcraft.rns.search.exception.SearchException;

public class RetrievalInformation<T> {

    protected static final EnumMap<Specification, RetrievalConverter> CONVERTERS = new EnumMap<>(Specification.class);

    static {
        CONVERTERS.put(Specification.ARRAY, new ArrayConverter());
        CONVERTERS.put(Specification.BOOLEAN, new BooleanConverter());
        CONVERTERS.put(Specification.COLLECTION, new CollectionConverter());
        CONVERTERS.put(Specification.ENUMERATION, new EnumerationConverter());
        CONVERTERS.put(Specification.INSTANT, new InstantConverter());
        CONVERTERS.put(Specification.MAP, new MapConverter());
        CONVERTERS.put(Specification.NUMBER, new NumberConverter());
        CONVERTERS.put(Specification.OBJECT, new ObjectConverter());
        CONVERTERS.put(Specification.STRING, new StringConverter());
    }

    private Map<Field, RetrievalConverter> converters;

    public RetrievalInformation(Class<T> clazz) {
        ReflectionUtility.doWithFields(clazz, (field) -> {
            // TODO 检查是否存在自定义转换器

            Type type = field.getGenericType();
            Specification specification = Specification.getSpecification(type);
            RetrievalConverter converter = CONVERTERS.get(specification);
            if (converter == null) {
                throw new SearchException();
            } else {
                converters.put(field, converter);
            }
        });
    }

    public Document convert(T object) {
        Document document = new Document();

        try {
            for (Entry<Field, RetrievalConverter> term : converters.entrySet()) {
                Field field = term.getKey();

                // TODO 此处可以考虑优化
                String name = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(object);
                RetrievalAnalyze analyze = field.getAnnotation(RetrievalAnalyze.class);
                RetrievalIndex index = field.getAnnotation(RetrievalIndex.class);
                RetrievalSort sort = field.getAnnotation(RetrievalSort.class);
                RetrievalStore store = field.getAnnotation(RetrievalStore.class);

                RetrievalConverter converter = term.getValue();
                for (IndexableField indexable : converter.convert(name, type, data, analyze, index, sort, store)) {
                    document.add(indexable);
                }
            }
        } catch (Exception exception) {
            // TODO
        }

        return document;
    }

}
